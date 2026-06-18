package com.payhub.settlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.settlement.entity.AgentRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AgentRelationMapper extends BaseMapper<AgentRelation> {

    IPage<AgentRelation> selectPageList(IPage<AgentRelation> page, @Param("params") Map<String, Object> params);

    List<AgentRelation> selectByParentMerchantNo(@Param("parentMerchantNo") String parentMerchantNo);

    List<AgentRelation> selectAllSubordinates(@Param("merchantNo") String merchantNo);

    AgentRelation selectByMerchantNo(@Param("merchantNo") String merchantNo);
}
