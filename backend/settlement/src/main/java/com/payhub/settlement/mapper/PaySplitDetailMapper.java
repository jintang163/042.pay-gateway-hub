package com.payhub.settlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payhub.settlement.entity.PaySplitDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaySplitDetailMapper extends BaseMapper<PaySplitDetail> {

    @Select("SELECT * FROM pay_split_detail WHERE order_no = #{orderNo} ORDER BY id DESC")
    List<PaySplitDetail> selectByOrderNo(@Param("orderNo") String orderNo);

    @Select("SELECT * FROM pay_split_detail WHERE settlement_id = #{settlementId} ORDER BY id DESC")
    List<PaySplitDetail> selectBySettlementId(@Param("settlementId") Long settlementId);

    @Select("SELECT COALESCE(SUM(split_amount), 0) FROM pay_split_detail " +
            "WHERE merchant_no = #{merchantNo} " +
            "AND status = 1 " +
            "AND settle_time >= #{startDate} " +
            "AND settle_time < #{endDate}")
    BigDecimal sumSettledAmountByMerchantNo(@Param("merchantNo") String merchantNo,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    @Update("UPDATE pay_split_detail SET status = #{status}, settle_time = #{settleTime}, updated_at = NOW() " +
            "WHERE settlement_id = #{settlementId}")
    int updateStatusBySettlementId(@Param("settlementId") Long settlementId,
                                   @Param("status") Integer status,
                                   @Param("settleTime") LocalDateTime settleTime);
}
