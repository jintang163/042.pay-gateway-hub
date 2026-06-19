package com.payhub.marketing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.marketing.dto.PayLinkSaveRequest;
import com.payhub.marketing.dto.PayLinkVO;

import java.util.Map;

public interface PayLinkService {

    IPage<PayLinkVO> listPage(Long current, Long size, String merchantNo, Map<String, Object> params);

    PayLinkVO getByLinkCode(String linkCode);

    void saveLink(PayLinkSaveRequest request);

    void toggleStatus(Long id);

    void deleteLink(Long id);

    PayLinkVO resolveLink(String linkCode);
}
