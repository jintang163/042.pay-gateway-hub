package com.payhub.risk.engine;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payhub.risk.dto.RiskFact;
import com.payhub.risk.entity.RiskRule;
import com.payhub.risk.mapper.RiskRuleMapper;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class DroolsRuleEngine {

    @Autowired
    private KieServices kieServices;

    @Autowired
    private KieRepository kieRepository;

    @Autowired
    private RiskRuleMapper riskRuleMapper;

    private final AtomicReference<KieContainer> kieContainerRef = new AtomicReference<>();

    private final ReentrantLock reloadLock = new ReentrantLock();

    private ReleaseId currentReleaseId;

    @PostConstruct
    public void init() {
        reloadRules();
    }

    public void reloadRules() {
        reloadLock.lock();
        try {
            List<RiskRule> enabledRules = riskRuleMapper.selectList(
                    new LambdaQueryWrapper<RiskRule>()
                            .eq(RiskRule::getStatus, 1)
                            .eq(RiskRule::getDeleted, 0)
                            .orderByAsc(RiskRule::getPriority)
            );

            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

            for (RiskRule rule : enabledRules) {
                try {
                    String drlContent = buildRuleContent(rule);
                    String path = "src/main/resources/com/payhub/risk/rules/" + rule.getRuleCode() + ".drl";
                    kieFileSystem.write(path, drlContent);
                    log.info("加载规则[{}]-[{}]成功", rule.getRuleCode(), rule.getRuleName());
                } catch (Exception e) {
                    log.error("加载规则[{}]-[{}]失败", rule.getRuleCode(), rule.getRuleName(), e);
                }
            }

            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();

            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                List<Message> errors = kieBuilder.getResults().getMessages(Message.Level.ERROR);
                StringBuilder errorMsg = new StringBuilder("规则编译错误:");
                for (Message error : errors) {
                    errorMsg.append("\n").append(error.getText());
                }
                log.error(errorMsg.toString());
                throw new RuntimeException(errorMsg.toString());
            }

            ReleaseId releaseId = kieRepository.getDefaultReleaseId();
            KieContainer oldContainer = kieContainerRef.get();
            KieContainer newContainer = kieServices.newKieContainer(releaseId);
            kieContainerRef.set(newContainer);
            this.currentReleaseId = releaseId;

            if (oldContainer != null) {
                try {
                    oldContainer.dispose();
                } catch (Exception e) {
                    log.warn("旧KieContainer释放失败", e);
                }
            }

            log.info("规则引擎重载完成，共加载{}条规则", enabledRules.size());
        } finally {
            reloadLock.unlock();
        }
    }

    public RiskFact executeRules(RiskFact fact) {
        KieContainer container = kieContainerRef.get();
        if (container == null) {
            log.warn("KieContainer未初始化，跳过规则执行");
            return fact;
        }

        KieSession kieSession = null;
        try {
            kieSession = container.newKieSession();
            kieSession.insert(fact);
            int firedCount = kieSession.fireAllRules();
            log.debug("规则执行完成，触发{}条规则", firedCount);
        } catch (Exception e) {
            log.error("规则执行异常", e);
        } finally {
            if (kieSession != null) {
                kieSession.dispose();
            }
        }

        return fact;
    }

    public String buildRuleContent(RiskRule rule) {
        String ruleType = rule.getRuleType();
        String ruleCondition = rule.getRuleCondition();
        JSONObject conditionParams = StrUtil.isNotBlank(ruleCondition)
                ? JSON.parseObject(ruleCondition)
                : new JSONObject();

        String conditionExpression = buildConditionExpression(ruleType, conditionParams);
        String actionExpression = buildActionExpression(rule);

        StringBuilder drl = new StringBuilder();
        drl.append("package com.payhub.risk.rules;\n");
        drl.append("import com.payhub.risk.dto.RiskFact;\n");
        drl.append("\n");
        drl.append("rule \"").append(rule.getRuleCode()).append("\"\n");
        drl.append("  salience ").append(rule.getPriority() != null ? rule.getPriority() : 100).append("\n");
        drl.append("  when\n");
        drl.append("    $fact: RiskFact(").append(conditionExpression).append(")\n");
        drl.append("  then\n");
        drl.append("    ").append(actionExpression).append("\n");
        drl.append("end\n");

        return drl.toString();
    }

    private String buildConditionExpression(String ruleType, JSONObject params) {
        switch (ruleType) {
            case "AMOUNT":
                return buildAmountCondition(params);
            case "FREQUENCY":
                return buildFrequencyCondition(params);
            case "IP_BLACKLIST":
                return buildIpBlacklistCondition();
            case "DEVICE":
                return buildDeviceCondition(params);
            case "BEHAVIOR":
                return buildBehaviorCondition(params);
            default:
                return "payAmount != null";
        }
    }

    private String buildAmountCondition(JSONObject params) {
        StringBuilder condition = new StringBuilder("payAmount != null");
        BigDecimal minAmount = params.getBigDecimal("minAmount");
        BigDecimal maxAmount = params.getBigDecimal("maxAmount");

        if (minAmount != null) {
            condition.append(", payAmount.compareTo(new java.math.BigDecimal(\"")
                    .append(minAmount.toPlainString()).append("\")) >= 0");
        }
        if (maxAmount != null) {
            condition.append(", payAmount.compareTo(new java.math.BigDecimal(\"")
                    .append(maxAmount.toPlainString()).append("\")) > 0");
        }

        return condition.toString();
    }

    private String buildFrequencyCondition(JSONObject params) {
        Long maxCount = params.getLong("maxCount");
        if (maxCount == null) {
            maxCount = 100L;
        }
        return "frequencyCount != null, frequencyCount > " + maxCount;
    }

    private String buildIpBlacklistCondition() {
        return "ipBlacklisted != null, ipBlacklisted == true";
    }

    private String buildDeviceCondition(JSONObject params) {
        Integer minRiskScore = params.getInteger("minRiskScore");
        if (minRiskScore == null) {
            minRiskScore = 60;
        }
        return "deviceRiskScore != null, deviceRiskScore >= " + minRiskScore;
    }

    private String buildBehaviorCondition(JSONObject params) {
        String customCondition = params.getString("customCondition");
        if (StrUtil.isNotBlank(customCondition)) {
            return customCondition;
        }
        return "behaviorAbnormal != null, behaviorAbnormal == true";
    }

    private String buildActionExpression(RiskRule rule) {
        String actionType = rule.getActionType() != null ? rule.getActionType() : "BLOCK";
        Integer riskLevel = rule.getRiskLevel() != null ? rule.getRiskLevel() : 1;
        String ruleCode = rule.getRuleCode();
        String ruleName = rule.getRuleName() != null ? rule.getRuleName() : ruleCode;

        StringBuilder action = new StringBuilder();
        action.append("$fact.addHitRule(\"").append(ruleCode).append("\", ");
        action.append("\"").append(ruleName).append("命中\", ");
        action.append(riskLevel).append(");\n");

        if ("BLOCK".equals(actionType)) {
            action.append("    $fact.setBlocked(true);\n");
        }
        action.append("    $fact.setActionType(\"").append(actionType).append("\");\n");

        return action.toString();
    }
}
