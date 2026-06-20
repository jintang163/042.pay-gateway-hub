package com.payhub.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.settlement.dto.SplitReceiverBatchImportItem;
import com.payhub.settlement.dto.SplitReceiverIdCardVerifyRequest;
import com.payhub.settlement.dto.SplitReceiverSaveRequest;
import com.payhub.settlement.dto.SplitReceiverVO;
import com.payhub.settlement.dto.SplitReceiverVerifyLogVO;
import com.payhub.settlement.dto.SplitReceiverVerifyRequest;
import com.payhub.settlement.entity.SplitReceiver;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

public interface SplitReceiverService extends IService<SplitReceiver> {

    IPage<SplitReceiverVO> listPage(Long current, Long size, String merchantNo, Map<String, Object> params);

    SplitReceiverVO getByReceiverNo(String receiverNo);

    void saveReceiver(SplitReceiverSaveRequest request, String merchantNo, String operatorId, String operatorName);

    void toggleStatus(Long id);

    void deleteReceiver(Long id);

    void verifyReceiver(SplitReceiverVerifyRequest request, String merchantNo, String operatorId, String operatorName);

    void verifyIdCard(@Valid SplitReceiverIdCardVerifyRequest request, String merchantNo, String operatorId, String operatorName);

    SplitReceiver checkReceiverVerified(String receiverNo, String merchantNo);

    Map<String, Object> batchImport(List<SplitReceiverBatchImportItem> items, String merchantNo, String operatorId, String operatorName);

    Map<String, Object> batchImport(List<SplitReceiverBatchImportItem> items, Boolean autoVerify, String merchantNo, String operatorId, String operatorName);

    Map<String, Object> batchImportWithFile(MultipartFile file, Boolean autoVerify, String merchantNo, String operatorId, String operatorName);

    Map<String, Object> batchVerifyReceiver(List<String> receiverNos, String merchantNo, String operatorId, String operatorName);

    IPage<SplitReceiverVerifyLogVO> listVerifyLogs(Long current, Long size, String merchantNo, Map<String, Object> params);

    List<SplitReceiverVO> listAvailableReceivers(String merchantNo);
}
