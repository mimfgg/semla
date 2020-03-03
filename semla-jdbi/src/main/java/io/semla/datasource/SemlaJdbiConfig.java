package io.semla.datasource;

import org.jdbi.v3.core.config.JdbiConfig;

public class SemlaJdbiConfig implements JdbiConfig<SemlaJdbiConfig> {

    public boolean autoCreateTable;

    public SemlaJdbiConfig() {
    }

    private SemlaJdbiConfig(boolean autoCreateTable) {
        this.autoCreateTable = autoCreateTable;
    }

    @Override
    public SemlaJdbiConfig createCopy() {
        return new SemlaJdbiConfig(autoCreateTable);
    }
}
