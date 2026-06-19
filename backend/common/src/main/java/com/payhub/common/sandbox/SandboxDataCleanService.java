package com.payhub.common.sandbox;

import com.payhub.common.config.DynamicDataSource;
import com.payhub.common.context.SandboxContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class SandboxDataCleanService {

    private static final List<String> CLEAN_TABLES = Arrays.asList(
            "pay_order",
            "pay_refund",
            "pay_channel_log",
            "sandbox_test_record",
            "merchant_config_test_log",
            "callback_simulate_log",
            "risk_control_log",
            "api_access_log"
    );

    @Autowired
    @Qualifier("sandboxDataSource")
    private DataSource sandboxDataSource;

    public void cleanAllSandboxData() {
        log.info("开始清理沙箱数据库数据，清理时间: {}", LocalDateTime.now());
        SandboxContext.setSandboxMode(true);
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(sandboxDataSource);
            for (String table : CLEAN_TABLES) {
                try {
                    jdbcTemplate.execute("TRUNCATE TABLE " + table);
                    log.info("已清理沙箱表: {}", table);
                } catch (Exception e) {
                    log.warn("清理沙箱表失败: {}, error: {}", table, e.getMessage());
                }
            }
            log.info("沙箱数据库数据清理完成");
        } finally {
            SandboxContext.clear();
        }
    }

    public void cleanExpiredSandboxData(int daysToKeep) {
        log.info("开始清理沙箱数据库过期数据（保留{}天），清理时间: {}", daysToKeep, LocalDateTime.now());
        SandboxContext.setSandboxMode(true);
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(sandboxDataSource);
            String sql = "DELETE FROM ";
            String where = " WHERE created_at < DATE_SUB(NOW(), INTERVAL " + daysToKeep + " DAY)";

            for (String table : CLEAN_TABLES) {
                try {
                    int count = jdbcTemplate.update(sql + table + where);
                    log.info("已清理沙箱表: {}, 删除记录数: {}", table, count);
                } catch (Exception e) {
                    log.warn("清理沙箱表失败: {}, error: {}", table, e.getMessage());
                }
            }
            log.info("沙箱数据库过期数据清理完成");
        } finally {
            SandboxContext.clear();
        }
    }
}
