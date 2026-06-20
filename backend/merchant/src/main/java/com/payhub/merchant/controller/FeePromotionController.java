package com.payhub.merchant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.context.CurrentUserContext;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.Result;
import com.payhub.common.result.ResultCode;
import com.payhub.merchant.dto.FeePromotionSaveRequest;
import com.payhub.merchant.dto.FeePromotionVO;
import com.payhub.merchant.dto.MerchantFeePromotionVO;
import com.payhub.merchant.service.FeePromotionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class FeePromotionController {

    @Autowired
    private FeePromotionService feePromotionService;

    @PostMapping("/admin/fee-promotion")
    public Result<Void> savePromotion(@Valid @RequestBody FeePromotionSaveRequest request) {
        if (!CurrentUserContext.isAdmin()) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "仅管理员可配置费率优惠活动");
        }
        feePromotionService.savePromotion(request);
        return Result.success();
    }

    @PostMapping("/admin/fee-promotion/{id}/toggle")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        if (!CurrentUserContext.isAdmin()) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "仅管理员可操作费率优惠活动");
        }
        feePromotionService.toggleStatus(id);
        return Result.success();
    }

    @DeleteMapping("/admin/fee-promotion/{id}")
    public Result<Void> deletePromotion(@PathVariable Long id) {
        if (!CurrentUserContext.isAdmin()) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "仅管理员可删除费率优惠活动");
        }
        feePromotionService.deletePromotion(id);
        return Result.success();
    }

    @GetMapping("/admin/fee-promotion/{promotionNo}")
    public Result<FeePromotionVO> getByPromotionNo(@PathVariable String promotionNo) {
        if (!CurrentUserContext.isAdmin()) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "仅管理员可查看费率优惠活动详情");
        }
        FeePromotionVO vo = feePromotionService.getByPromotionNo(promotionNo);
        return Result.success(vo);
    }

    @GetMapping("/admin/fee-promotion/list")
    public Result<IPage<FeePromotionVO>> list(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String promotionName,
            @RequestParam(required = false) Integer promotionType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (!CurrentUserContext.isAdmin()) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "仅管理员可查看费率优惠活动列表");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("promotionName", promotionName);
        params.put("promotionType", promotionType);
        params.put("status", status);
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        IPage<FeePromotionVO> page = feePromotionService.listPage(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/merchant/fee-promotion/list")
    public Result<List<MerchantFeePromotionVO>> listMerchantPromotions() {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        if (merchantNo == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN, "请先登录");
        }
        List<MerchantFeePromotionVO> list = feePromotionService.listMerchantPromotions(merchantNo);
        return Result.success(list);
    }

    @GetMapping("/merchant/fee-promotion/current")
    public Result<MerchantFeePromotionVO> getCurrentBestPromotion() {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        if (merchantNo == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN, "请先登录");
        }
        MerchantFeePromotionVO vo = feePromotionService.getCurrentBestPromotion(merchantNo);
        return Result.success(vo);
    }

    @GetMapping("/merchant/fee-promotion/{merchantNo}/list")
    public Result<List<MerchantFeePromotionVO>> listMerchantPromotionsByAdmin(@PathVariable String merchantNo) {
        if (!CurrentUserContext.isAdmin()) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "仅管理员可查看商户费率优惠");
        }
        List<MerchantFeePromotionVO> list = feePromotionService.listMerchantPromotions(merchantNo);
        return Result.success(list);
    }
}
