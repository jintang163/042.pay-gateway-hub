package com.payhub.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.settlement.dto.WriteoffRecordVO;
import com.payhub.settlement.entity.ReconcileWriteoffRecord;

import java.util.Map;

public interface ReconcileWriteoffRecordService extends IService<ReconcileWriteoffRecord> {

    IPage<WriteoffRecordVO> listPage(Long current, Long size, Map<String, Object> params);

    WriteoffRecordVO getByWriteoffNo(String writeoffNo);

    void executeWriteoff(Long id);

    void retryWriteoff(Long id);
}
