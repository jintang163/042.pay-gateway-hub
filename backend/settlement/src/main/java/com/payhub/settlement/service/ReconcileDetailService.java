package com.payhub.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.settlement.dto.ReconcileDetailVO;
import com.payhub.settlement.dto.ReconcileSummaryVO;
import com.payhub.settlement.entity.ReconcileDetail;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReconcileDetailService extends IService<ReconcileDetail> {

    IPage<ReconcileDetailVO> listPage(Long current, Long size, Map<String, Object> params);

    List<ReconcileDetailVO> listByReconcileNo(String reconcileNo);

    ReconcileSummaryVO getSummary(String reconcileNo);

    ReconcileSummaryVO getSummaryByDateAndChannel(LocalDate reconcileDate, String payChannel);

    void handleDetail(Long detailId, Integer handleStatus, String handleRemark, String handleUserId, String handleUserName);

    void exportDetails(String reconcileNo, HttpServletResponse response);

    void exportDetailsByCondition(Map<String, Object> params, HttpServletResponse response);
}
