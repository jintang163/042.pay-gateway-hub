package com.payhub.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.settlement.dto.ReconcileVO;
import com.payhub.settlement.entity.ReconcileRecord;

import java.time.LocalDate;
import java.util.Map;

public interface ReconcileService extends IService<ReconcileRecord> {

    ReconcileVO getByReconcileNo(String reconcileNo);

    IPage<ReconcileVO> listPage(Long current, Long size, Map<String, Object> params);

    void executeReconcile(String payChannel, LocalDate reconcileDate);
}
