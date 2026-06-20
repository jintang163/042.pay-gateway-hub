package com.payhub.marketing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.marketing.dto.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public interface MerchantAdService {

    IPage<MerchantAdVO> listPage(Long current, Long size, String merchantNo, Map<String, Object> params);

    MerchantAdVO getByAdCode(String adCode);

    MerchantAdVO getById(Long id);

    void saveAd(MerchantAdSaveRequest request);

    void toggleStatus(Long id);

    void deleteAd(Long id);

    List<AdDisplayVO> listDisplayAds(String merchantNo, String position, Integer limit);

    void recordImpression(List<String> adCodes);

    AdClickReportResult reportClick(AdClickReportRequest request, HttpServletRequest httpRequest);

    AdStatsOverviewVO getStatsOverview(String merchantNo, String startDate, String endDate, String adCode, String position);
}
