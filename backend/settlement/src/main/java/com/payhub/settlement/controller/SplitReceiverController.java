package com.payhub.settlement.controller;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.context.CurrentUserContext;
import com.payhub.common.result.Result;
import com.payhub.settlement.dto.SplitReceiverBatchImportItem;
import com.payhub.settlement.dto.SplitReceiverSaveRequest;
import com.payhub.settlement.dto.SplitReceiverVO;
import com.payhub.settlement.dto.SplitReceiverVerifyLogVO;
import com.payhub.settlement.dto.SplitReceiverVerifyRequest;
import com.payhub.settlement.service.SplitReceiverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/split-receiver")
public class SplitReceiverController {

    @Autowired
    private SplitReceiverService splitReceiverService;

    @GetMapping("/list")
    public Result<IPage<SplitReceiverVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String receiverNo,
            @RequestParam(required = false) String receiverName,
            @RequestParam(required = false) Integer receiverType,
            @RequestParam(required = false) Integer verifyStatus,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String idCardNo,
            @RequestParam(required = false) String bankCardNo) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        Map<String, Object> params = new HashMap<>();
        params.put("receiverNo", receiverNo);
        params.put("receiverName", receiverName);
        params.put("receiverType", receiverType);
        params.put("verifyStatus", verifyStatus);
        params.put("status", status);
        params.put("idCardNo", idCardNo);
        params.put("bankCardNo", bankCardNo);
        IPage<SplitReceiverVO> page = splitReceiverService.listPage(current, size, merchantNo, params);
        return Result.success(page);
    }

    @GetMapping("/{receiverNo}")
    public Result<SplitReceiverVO> getByReceiverNo(@PathVariable String receiverNo) {
        SplitReceiverVO vo = splitReceiverService.getByReceiverNo(receiverNo);
        return Result.success(vo);
    }

    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody SplitReceiverSaveRequest request) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        String operatorId = getCurrentOperatorId();
        String operatorName = getCurrentOperatorName();
        splitReceiverService.saveReceiver(request, merchantNo, operatorId, operatorName);
        return Result.success();
    }

    @PostMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable Long id) {
        splitReceiverService.toggleStatus(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        splitReceiverService.deleteReceiver(id);
        return Result.success();
    }

    @PostMapping("/verify")
    public Result<Void> verify(@Valid @RequestBody SplitReceiverVerifyRequest request) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        String operatorId = getCurrentOperatorId();
        String operatorName = getCurrentOperatorName();
        splitReceiverService.verifyReceiver(request, merchantNo, operatorId, operatorName);
        return Result.success();
    }

    @GetMapping("/available")
    public Result<List<SplitReceiverVO>> listAvailable() {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        List<SplitReceiverVO> list = splitReceiverService.listAvailableReceivers(merchantNo);
        return Result.success(list);
    }

    @PostMapping("/batch-import")
    public Result<Map<String, Object>> batchImport(@RequestBody List<SplitReceiverBatchImportItem> items) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        String operatorId = getCurrentOperatorId();
        String operatorName = getCurrentOperatorName();
        Map<String, Object> result = splitReceiverService.batchImport(items, merchantNo, operatorId, operatorName);
        return Result.success(result);
    }

    @PostMapping(value = "/batch-import-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> batchImportFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "autoVerify", defaultValue = "false") Boolean autoVerify) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        String operatorId = getCurrentOperatorId();
        String operatorName = getCurrentOperatorName();
        Map<String, Object> result = splitReceiverService.batchImportWithFile(file, autoVerify, merchantNo, operatorId, operatorName);
        return Result.success(result);
    }

    @PostMapping("/batch-verify")
    public Result<Map<String, Object>> batchVerify(@RequestBody Map<String, Object> body) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        String operatorId = getCurrentOperatorId();
        String operatorName = getCurrentOperatorName();
        Object receiverNosObj = body.get("receiverNos");
        List<String> receiverNos = new ArrayList<>();
        if (receiverNosObj instanceof List) {
            for (Object o : (List<?>) receiverNosObj) {
                if (o != null) {
                    receiverNos.add(o.toString());
                }
            }
        }
        if (CollUtil.isEmpty(receiverNos)) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("successCount", 0);
            emptyResult.put("failCount", 0);
            emptyResult.put("failDetails", new ArrayList<>());
            return Result.success(emptyResult);
        }
        Map<String, Object> result = splitReceiverService.batchVerifyReceiver(receiverNos, merchantNo, operatorId, operatorName);
        return Result.success(result);
    }

    @GetMapping("/batch-import-template")
    public Result<Map<String, Object>> getBatchImportTemplate() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> headers = new ArrayList<>();

        String[][] headerDefs = {
                {"接收方名称", "String", "必填，接收方的名称"},
                {"接收方类型", "String/Integer", "必填，1或个人=个人；2或企业=企业"},
                {"证件姓名", "String", "必填，身份证或营业执照上的姓名/名称"},
                {"证件号码", "String", "必填，身份证号或统一社会信用代码"},
                {"银行卡号", "String", "必填，银行账户号码"},
                {"预留手机号", "String", "必填，银行卡预留手机号"},
                {"开户银行", "String", "必填，如：中国工商银行"},
                {"开户支行", "String", "选填，如：北京朝阳支行"},
                {"联系人", "String", "选填，联系人姓名"},
                {"联系电话", "String", "选填，联系电话"},
                {"联系邮箱", "String", "选填，联系邮箱"},
                {"备注", "String", "选填，备注信息"}
        };

        for (String[] def : headerDefs) {
            Map<String, Object> header = new HashMap<>();
            header.put("name", def[0]);
            header.put("type", def[1]);
            header.put("desc", def[2]);
            headers.add(header);
        }

        result.put("fileName", "split_receiver_import_template.xlsx");
        result.put("supportedFormats", new String[]{".xlsx", ".xls", ".csv"});
        result.put("headers", headers);
        result.put("notes", CollUtil.newArrayList(
                "第一行必须为表头行，表头名称需严格按照模板填写",
                "接收方类型支持 1/2 数字 或 个人/企业 中文",
                "证件号+银行卡号 不能重复（同一商户下）",
                "沙箱测试规则：银行卡号以 0000 结尾 -> 认证成功；以 9999 结尾 -> 认证失败；其他 -> 认证成功"
        ));
        return Result.success(result);
    }

    @GetMapping("/verify-logs")
    public Result<IPage<SplitReceiverVerifyLogVO>> listVerifyLogs(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String receiverNo,
            @RequestParam(required = false) Integer verifyChannel,
            @RequestParam(required = false) Integer verifyStatus,
            @RequestParam(required = false) String verifyRequestId) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        Map<String, Object> params = new HashMap<>();
        params.put("receiverNo", receiverNo);
        params.put("verifyChannel", verifyChannel);
        params.put("verifyStatus", verifyStatus);
        params.put("verifyRequestId", verifyRequestId);
        IPage<SplitReceiverVerifyLogVO> page = splitReceiverService.listVerifyLogs(current, size, merchantNo, params);
        return Result.success(page);
    }

    private String getCurrentOperatorId() {
        Object user = CurrentUserContext.getCurrentUser();
        if (user == null) {
            return null;
        }
        try {
            java.lang.reflect.Method method = user.getClass().getMethod("getId");
            Object id = method.invoke(user);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getCurrentOperatorName() {
        Object user = CurrentUserContext.getCurrentUser();
        if (user == null) {
            return null;
        }
        try {
            java.lang.reflect.Method method = user.getClass().getMethod("getUsername");
            Object name = method.invoke(user);
            return name != null ? name.toString() : null;
        } catch (Exception e) {
            try {
                java.lang.reflect.Method method = user.getClass().getMethod("getName");
                Object name = method.invoke(user);
                return name != null ? name.toString() : null;
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
