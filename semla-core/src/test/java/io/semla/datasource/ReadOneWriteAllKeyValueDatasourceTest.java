package io.semla.datasource;

import io.semla.model.Player;

import static io.semla.datasource.ReadOneWriteAllDatasourceTest.*;

public class ReadOneWriteAllKeyValueDatasourceTest extends ReplicatedKeyValueDatasourceTest<ReadOneWriteAllDatasource<Player>> {

    public ReadOneWriteAllKeyValueDatasourceTest() {
        super(WRAPPER, FIRST_DATASOURCE, SECOND_DATASOURCE);
    }
}
