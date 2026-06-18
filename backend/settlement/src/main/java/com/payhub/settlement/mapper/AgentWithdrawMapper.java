package com.payhub.settlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.settlement.entity.AgentWithdraw;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface AgentWithdrawMapper extends BaseMapper<AgentWithdraw> {

    IPage<AgentWithdraw> selectPageList(IPage<AgentWithdraw> page, @Param("params") Map<String, Object> params);

    List<AgentWithdraw> selectByMerchantNo(@Param("merchantNo") String merchantNo);

    AgentWithdraw selectByWithdrawNo(@Param("withdrawNo") String withdrawNo);

    BigDecimal selectTotalWithdrawByMerchant(@Param("merchantNo") String merchantNo, @Param("withdrawStatus") Integer withdrawStatus);
}
