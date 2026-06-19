package com.payhub.common.config;

import com.payhub.common.context.SandboxContext;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DynamicDataSource extends AbstractRoutingDataSource {

    public static final String DATA_SOURCE_PRODUCTION = "production";
    public static final String DATA_SOURCE_SANDBOX = "sandbox";

    @Override
    protected Object determineCurrentLookupKey() {
        if (SandboxContext.isSandboxMode()) {
            return DATA_SOURCE_SANDBOX;
        }
        return DATA_SOURCE_PRODUCTION;
    }
}
