package com.payhub.settlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payhub.settlement.entity.SettlementRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface SettlementRecordMapper extends BaseMapper<SettlementRecord> {

    @Select("SELECT * FROM settlement_record WHERE merchant_no = #{merchantNo} AND pay_channel = #{payChannel} AND settle_date = #{settleDate} LIMIT 1")
    SettlementRecord selectByMerchantChannelDate(@Param("merchantNo") String merchantNo, @Param("payChannel") String payChannel, @Param("settleDate") LocalDate settleDate);
}
