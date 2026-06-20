package com.payhub.settlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.settlement.entity.MerchantWithdraw;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface MerchantWithdrawMapper extends BaseMapper<MerchantWithdraw> {

    IPage<MerchantWithdraw> selectPageList(IPage<MerchantWithdraw> page, @Param("params") Map<String, Object> params);

    List<MerchantWithdraw> selectByMerchantNo(@Param("merchantNo") String merchantNo);

    MerchantWithdraw selectByWithdrawNo(@Param("withdrawNo") String withdrawNo);

    BigDecimal selectTotalWithdrawByMerchant(@Param("merchantNo") String merchantNo, @Param("withdrawStatus") Integer withdrawStatus);

    BigDecimal selectTotalSettleAmount(@Param("merchantNo") String merchantNo);
}
