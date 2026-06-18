package com.payhub.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.settlement.dto.AgentProfitRecordVO;
import com.payhub.settlement.entity.AgentProfitRecord;
import com.payhub.settlement.entity.AgentProfitRule;
import com.payhub.settlement.entity.AgentRelation;
import com.payhub.settlement.enums.AgentProfitStatusEnum;
import com.payhub.settlement.mapper.AgentProfitRecordMapper;
import com.payhub.settlement.mapper.AgentProfitRuleMapper;
import com.payhub.settlement.mapper.AgentRelationMapper;
import com.payhub.settlement.service.AgentProfitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AgentProfitServiceImpl extends ServiceImpl<AgentProfitRecordMapper, AgentProfitRecord> implements AgentProfitService {

    @Autowired
    private AgentRelationMapper agentRelationMapper;

    @Autowired
    private AgentProfitRuleMapper agentProfitRuleMapper;

    @Override
    public AgentProfitRecordVO getProfitRecordById(Long id) {
        AgentProfitRecord record = this.getById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分润记录不存在");
        }
        return convertToVO(record);
    }

    @Override
    public AgentProfitRecordVO getProfitRecordByProfitNo(String profitNo) {
        AgentProfitRecord record = baseMapper.selectOne(null);
        return record != null ? convertToVO(record) : null;
    }

    @Override
    public IPage<AgentProfitRecordVO> listPage(Long current, Long size, Map<String, Object> params) {
        Page<AgentProfitRecord> page = new Page<>(current, size);
        IPage<AgentProfitRecord> recordPage = baseMapper.selectPageList(page, params);
        return recordPage.convert(this::convertToVO);
    }

    @Override
    public List<AgentProfitRecordVO> listByAgentMerchantNo(String agentMerchantNo) {
        List<AgentProfitRecord> records = baseMapper.selectByAgentMerchantNo(agentMerchantNo);
        return records.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public BigDecimal getTotalProfit(String agentMerchantNo, Integer profitStatus) {
        BigDecimal total = baseMapper.selectTotalProfitByAgent(agentMerchantNo, profitStatus);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateProfit(String orderNo, String merchantNo, BigDecimal orderAmount, BigDecimal feeAmount) {
        AgentRelation relation = agentRelationMapper.selectByMerchantNo(merchantNo);
        if (relation == null) {
            log.info("商户无代理关系，跳过分润计算: merchantNo={}, orderNo={}", merchantNo, orderNo);
            return;
        }
        String agentPath = relation.getAgentPath();
        if (agentPath == null || agentPath.isEmpty()) {
            return;
        }
        String[] pathNodes = agentPath.split("/");
        String currentMerchantNo = merchantNo;
        String currentMerchantName = relation.getMerchantName();
        for (int i = pathNodes.length - 2; i >= 0; i--) {
            String agentMerchantNo = pathNodes[i];
            AgentRelation agentRelation = agentRelationMapper.selectByMerchantNo(agentMerchantNo);
            if (agentRelation == null || agentRelation.getStatus() != 1) {
                continue;
            }
            List<AgentProfitRule> rules = agentProfitRuleMapper.selectByMerchantNo(agentMerchantNo);
            BigDecimal commissionRate = agentRelation.getCommissionRate();
            if (CollUtil.isNotEmpty(rules)) {
                int level = pathNodes.length - 1 - i;
                for (AgentProfitRule rule : rules) {
                    if (rule.getAgentLevel() != null && rule.getAgentLevel() == level && rule.getStatus() == 1) {
                        commissionRate = rule.getCommissionRate();
                        break;
                    }
                }
            }
            if (commissionRate == null || commissionRate.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal profitAmount = feeAmount.multiply(commissionRate).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
            AgentProfitRecord record = new AgentProfitRecord();
            record.setProfitNo(OrderNoGenerator.generateWithPrefix("AP"));
            record.setOrderNo(orderNo);
            record.setMerchantNo(currentMerchantNo);
            record.setMerchantName(currentMerchantName);
            record.setAgentMerchantNo(agentMerchantNo);
            record.setAgentMerchantName(agentRelation.getMerchantName());
            record.setAgentLevel(pathNodes.length - 1 - i);
            record.setOrderAmount(orderAmount);
            record.setFeeAmount(feeAmount);
            record.setProfitAmount(profitAmount);
            record.setCommissionRate(commissionRate);
            record.setSettleDate(DateUtil.format(LocalDate.now(), "yyyy-MM-dd"));
            record.setProfitStatus(AgentProfitStatusEnum.PENDING.getCode());
            this.save(record);
            currentMerchantNo = agentMerchantNo;
            currentMerchantName = agentRelation.getMerchantName();
        }
        log.info("代理分润计算完成: orderNo={}, merchantNo={}", orderNo, merchantNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleAgentProfit(String settleDate) {
        List<AgentProfitRecord> pendingRecords = baseMapper.selectBySettleDate(settleDate);
        if (CollUtil.isEmpty(pendingRecords)) {
            log.info("无待结算分润记录: settleDate={}", settleDate);
            return;
        }
        for (AgentProfitRecord record : pendingRecords) {
            if (AgentProfitStatusEnum.PENDING.getCode().equals(record.getProfitStatus())) {
                record.setProfitStatus(AgentProfitStatusEnum.SETTLED.getCode());
                this.updateById(record);
            }
        }
        log.info("代理分润结算完成: settleDate={}, count={}", settleDate, pendingRecords.size());
    }

    private AgentProfitRecordVO convertToVO(AgentProfitRecord record) {
        AgentProfitRecordVO vo = BeanUtil.copyProperties(record, AgentProfitRecordVO.class);
        AgentProfitStatusEnum statusEnum = AgentProfitStatusEnum.getByCode(record.getProfitStatus());
        vo.setProfitStatusDesc(statusEnum != null ? statusEnum.getDesc() : "");
        return vo;
    }
}
