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

    @Update("<script>" +
            "UPDATE pay_split_detail " +
            "<set>" +
            "  transfer_status = #{status}," +
            "  <if test='channelTransferNo != null'>channel_transfer_no = #{channelTransferNo},</if>" +
            "  <if test='failReason != null'>transfer_fail_reason = #{failReason},</if>" +
            "  <if test='completeTime != null'>transfer_time = #{completeTime},</if>" +
            "  updated_at = NOW()" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int updateTransferStatusById(@Param("id") Long id,
                                 @Param("status") Integer status,
                                 @Param("channelTransferNo") String channelTransferNo,
                                 @Param("failReason") String failReason,
                                 @Param("completeTime") LocalDateTime completeTime);

    @Select("SELECT * FROM pay_split_detail " +
            "WHERE (transfer_status = 0 OR transfer_status IS NULL) " +
            "   OR (transfer_status = 3 " +
            "       AND (next_transfer_retry_time IS NULL OR next_transfer_retry_time < NOW()) " +
            "       AND (transfer_retry_count IS NULL OR transfer_retry_count < 5)) " +
            "ORDER BY id ASC " +
            "LIMIT #{limit}")
    List<PaySplitDetail> selectPendingTransferList(@Param("limit") int limit);

    @Select("SELECT COALESCE(SUM(split_amount), 0) FROM pay_split_detail " +
            "WHERE settlement_id = #{settlementId} " +
            "AND transfer_status = 2")
    BigDecimal sumTransferSuccessBySettlementId(@Param("settlementId") Long settlementId);
}
