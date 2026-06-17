package com.payhub.risk.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.risk.dto.SandboxTestRequest;
import com.payhub.risk.dto.SandboxTestResultVO;
import com.payhub.risk.entity.SandboxTestRecord;

import java.util.List;
import java.util.Map;

public interface SandboxTestService extends IService<SandboxTestRecord> {

    SandboxTestResultVO executeTest(SandboxTestRequest request);

    IPage<SandboxTestResultVO> listTestRecords(Long current, Long size, Map<String, Object> params);

    SandboxTestResultVO getTestRecord(String testId);

    List<Map<String, Object>> listTestScenes();
}
