package com.payhub.marketing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.marketing.dto.ActivitySaveRequest;
import com.payhub.marketing.dto.ActivityVO;

import java.util.Map;

public interface ActivityService {

    IPage<ActivityVO> listPage(Long current, Long size, String merchantNo, Map<String, Object> params);

    ActivityVO getByActivityCode(String activityCode);

    void saveActivity(ActivitySaveRequest request);

    void toggleStatus(Long id);

    void deleteActivity(Long id);
}
