package io.semla.datasource;

import io.semla.model.Player;

import static io.semla.datasource.MasterSlaveDatasourceTest.*;

public class MasterSlaveKeyValueDatasourceTest extends ReplicatedKeyValueDatasourceTest<MasterSlaveDatasource<Player>> {

    public MasterSlaveKeyValueDatasourceTest() {
        super(WRAPPER, FIRST_DATASOURCE, SECOND_DATASOURCE);
    }
}
