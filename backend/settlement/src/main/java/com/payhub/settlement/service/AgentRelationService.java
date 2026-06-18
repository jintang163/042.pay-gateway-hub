package com.payhub.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.settlement.dto.AgentRelationSaveRequest;
import com.payhub.settlement.dto.AgentRelationVO;
import com.payhub.settlement.dto.AgentStatsVO;
import com.payhub.settlement.dto.AgentTreeVO;
import com.payhub.settlement.entity.AgentRelation;

import java.util.List;
import java.util.Map;

public interface AgentRelationService extends IService<AgentRelation> {

    void saveAgentRelation(AgentRelationSaveRequest request);

    void deleteAgentRelation(Long id);

    AgentRelationVO getAgentRelationById(Long id);

    AgentRelationVO getAgentRelationByMerchantNo(String merchantNo);

    IPage<AgentRelationVO> listPage(Long current, Long size, Map<String, Object> params);

    List<AgentTreeVO> getAgentTree(String merchantNo);

    List<AgentRelationVO> listDirectSubordinates(String parentMerchantNo);

    List<AgentRelationVO> listAllSubordinates(String merchantNo);

    AgentStatsVO getAgentStats(String merchantNo);

    void updateAgentStatus(Long id, Integer status);
}
