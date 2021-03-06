package io.semla.datasource;

import io.semla.model.Player;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public class ReadOneWriteAllDatasourceTest extends ReplicatedDatasourceTest<ReadOneWriteAllDatasource<Player>> {

    public static final UnaryOperator<Datasource.Configuration> WRAPPER = defaultDatasource ->
        ReadOneWriteAllDatasource.configure().withDatasources(defaultDatasource, defaultDatasource);
    public static final Function<ReadOneWriteAllDatasource<Player>, Datasource<Player>> FIRST_DATASOURCE = datasource -> datasource.raw().get(0);
    public static final Function<ReadOneWriteAllDatasource<Player>, Datasource<Player>> SECOND_DATASOURCE = datasource -> datasource.raw().get(1);

    public ReadOneWriteAllDatasourceTest() {
        super(WRAPPER, FIRST_DATASOURCE, SECOND_DATASOURCE);
    }
}
