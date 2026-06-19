package com.payhub.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.settlement.dto.ReportPushRecordVO;
import com.payhub.settlement.entity.ReportPushRecord;

import java.util.Map;

public interface ReportPushRecordService extends IService<ReportPushRecord> {

    IPage<ReportPushRecordVO> listPage(Long current, Long size, Map<String, Object> params);

    ReportPushRecordVO getRecordById(Long id);

    boolean validateOwnership(Long id, String merchantNo);
}
