package com.payhub.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.settlement.dto.*;
import com.payhub.settlement.entity.AgentProfitRecord;
import com.payhub.settlement.entity.AgentRelation;
import com.payhub.settlement.entity.AgentWithdraw;
import com.payhub.settlement.enums.AgentProfitStatusEnum;
import com.payhub.settlement.enums.AgentWithdrawStatusEnum;
import com.payhub.settlement.mapper.AgentRelationMapper;
import com.payhub.settlement.service.AgentProfitService;
import com.payhub.settlement.service.AgentRelationService;
import com.payhub.settlement.service.AgentWithdrawService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AgentRelationServiceImpl extends ServiceImpl<AgentRelationMapper, AgentRelation> implements AgentRelationService {

    @Autowired
    private AgentProfitService agentProfitService;

    @Autowired
    private AgentWithdrawService agentWithdrawService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAgentRelation(AgentRelationSaveRequest request) {
        AgentRelation relation;
        if (request.getId() != null) {
            relation = this.getById(request.getId());
            if (relation == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "代理关系不存在");
            }
        } else {
            AgentRelation exist = this.getOne(new LambdaQueryWrapper<AgentRelation>()
                    .eq(AgentRelation::getMerchantNo, request.getMerchantNo()));
            if (exist != null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "该商户已存在代理关系");
            }
            relation = new AgentRelation();
            relation.setAgentLevel(request.getAgentLevel() != null ? request.getAgentLevel() : 1);
        }

        if (StrUtil.isNotBlank(request.getParentMerchantNo())) {
            AgentRelation parent = this.getOne(new LambdaQueryWrapper<AgentRelation>()
                    .eq(AgentRelation::getMerchantNo, request.getParentMerchantNo()));
            if (parent == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "上级代理不存在");
            }
            relation.setParentMerchantNo(request.getParentMerchantNo());
            relation.setParentMerchantName(parent.getMerchantName());
            if (request.getAgentLevel() == null) {
                relation.setAgentLevel(parent.getAgentLevel() + 1);
            }
            String agentPath = StrUtil.isNotBlank(parent.getAgentPath())
                    ? parent.getAgentPath() + "/" + request.getMerchantNo()
                    : request.getParentMerchantNo() + "/" + request.getMerchantNo();
            relation.setAgentPath(agentPath);
        } else {
            relation.setParentMerchantNo("");
            relation.setParentMerchantName("");
            relation.setAgentPath(request.getMerchantNo());
        }

        relation.setMerchantNo(request.getMerchantNo());
        relation.setMerchantName(request.getMerchantName());
        if (request.getCommissionRate() != null) {
            relation.setCommissionRate(request.getCommissionRate());
        }
        relation.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        relation.setRemark(request.getRemark());

        this.saveOrUpdate(relation);
        log.info("代理关系保存成功: id={}, merchantNo={}, level={}", relation.getId(), request.getMerchantNo(), relation.getAgentLevel());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAgentRelation(Long id) {
        AgentRelation relation = this.getById(id);
        if (relation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "代理关系不存在");
        }
        long childCount = this.count(new LambdaQueryWrapper<AgentRelation>()
                .eq(AgentRelation::getParentMerchantNo, relation.getMerchantNo()));
        if (childCount > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "存在下级代理，无法删除");
        }
        this.removeById(id);
        log.info("代理关系删除成功: id={}, merchantNo={}", id, relation.getMerchantNo());
    }

    @Override
    public AgentRelationVO getAgentRelationById(Long id) {
        AgentRelation relation = this.getById(id);
        if (relation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "代理关系不存在");
        }
        return convertToVO(relation);
    }

    @Override
    public AgentRelationVO getAgentRelationByMerchantNo(String merchantNo) {
        AgentRelation relation = baseMapper.selectByMerchantNo(merchantNo);
        return relation != null ? convertToVO(relation) : null;
    }

    @Override
    public IPage<AgentRelationVO> listPage(Long current, Long size, Map<String, Object> params) {
        Page<AgentRelation> page = new Page<>(current, size);
        IPage<AgentRelation> relationPage = baseMapper.selectPageList(page, params);
        return relationPage.convert(this::convertToVO);
    }

    @Override
    public List<AgentTreeVO> getAgentTree(String merchantNo) {
        AgentRelation root = baseMapper.selectByMerchantNo(merchantNo);
        if (root == null) {
            return Collections.emptyList();
        }
        List<AgentRelation> allSubordinates = baseMapper.selectAllSubordinates(merchantNo);
        Map<String, List<AgentRelation>> childrenMap = allSubordinates.stream()
                .collect(Collectors.groupingBy(AgentRelation::getParentMerchantNo));
        AgentTreeVO rootTree = buildTreeVO(root);
        buildTree(rootTree, childrenMap);
        return Collections.singletonList(rootTree);
    }

    private void buildTree(AgentTreeVO parent, Map<String, List<AgentRelation>> childrenMap) {
        List<AgentRelation> children = childrenMap.get(parent.getMerchantNo());
        if (children != null && !children.isEmpty()) {
            List<AgentTreeVO> childTreeList = children.stream()
                    .map(this::buildTreeVO)
                    .collect(Collectors.toList());
            parent.setChildren(childTreeList);
            for (AgentTreeVO child : childTreeList) {
                buildTree(child, childrenMap);
            }
        }
    }

    private AgentTreeVO buildTreeVO(AgentRelation relation) {
        AgentTreeVO vo = new AgentTreeVO();
        vo.setId(relation.getId());
        vo.setMerchantNo(relation.getMerchantNo());
        vo.setMerchantName(relation.getMerchantName());
        vo.setAgentLevel(relation.getAgentLevel());
        vo.setCommissionRate(relation.getCommissionRate());
        vo.setStatus(relation.getStatus());
        return vo;
    }

    @Override
    public List<AgentRelationVO> listDirectSubordinates(String parentMerchantNo) {
        List<AgentRelation> relations = baseMapper.selectByParentMerchantNo(parentMerchantNo);
        return relations.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<AgentRelationVO> listAllSubordinates(String merchantNo) {
        List<AgentRelation> relations = baseMapper.selectAllSubordinates(merchantNo);
        return relations.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public AgentStatsVO getAgentStats(String merchantNo) {
        AgentStatsVO stats = new AgentStatsVO();
        List<AgentRelation> allSubordinates = baseMapper.selectAllSubordinates(merchantNo);
        stats.setTotalSubordinateCount(allSubordinates.size());
        long activeCount = allSubordinates.stream().filter(r -> r.getStatus() == 1).count();
        stats.setActiveAgentCount((int) activeCount);
        stats.setTotalAgentCount((int) allSubordinates.size() + 1);
        BigDecimal totalProfit = agentProfitService.getTotalProfit(merchantNo, null);
        stats.setTotalProfitAmount(totalProfit);
        BigDecimal availableBalance = agentWithdrawService.getAvailableBalance(merchantNo);
        stats.setAvailableBalance(availableBalance);
        BigDecimal frozenAmount = agentWithdrawService.getTotalWithdraw(merchantNo, AgentWithdrawStatusEnum.PENDING.getCode());
        stats.setFrozenAmount(frozenAmount);
        long todayCount = allSubordinates.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                .count();
        stats.setTodayNewAgentCount((int) todayCount);
        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAgentStatus(Long id, Integer status) {
        AgentRelation relation = this.getById(id);
        if (relation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "代理关系不存在");
        }
        relation.setStatus(status);
        this.updateById(relation);
        log.info("代理状态更新成功: id={}, status={}", id, status);
    }

    private AgentRelationVO convertToVO(AgentRelation relation) {
        AgentRelationVO vo = BeanUtil.copyProperties(relation, AgentRelationVO.class);
        vo.setStatusDesc(relation.getStatus() == 1 ? "启用" : "禁用");
        return vo;
    }
}
