package io.semla.datasource;

import io.semla.model.EntityModel;
import io.semla.model.Player;
import io.semla.util.Lists;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public class MasterSlaveDatasourceTest extends ReplicatedDatasourceTest<MasterSlaveDatasource<Player>> {

    public static final UnaryOperator<Datasource.Configuration> WRAPPER = defaultFactory -> new Datasource.Configuration() {
        @Override
        public <T> Datasource<T> create(EntityModel<T> entityModel) {
            Datasource<T> master = defaultFactory.create(entityModel);
            Datasource<T> slave = defaultFactory.create(entityModel);
            // faking replication
            CachedDatasource<T> replicatedMaster = new CachedDatasource<T>(entityModel, slave, master);
            return new MasterSlaveDatasource<>(entityModel, replicatedMaster, Lists.of(slave));
        }
    };
    public static final Function<MasterSlaveDatasource<Player>, Datasource<Player>> FIRST_DATASOURCE = datasource -> datasource.raw().first();
    public static final Function<MasterSlaveDatasource<Player>, Datasource<Player>> SECOND_DATASOURCE = datasource -> datasource.raw().second().get(0);

    public MasterSlaveDatasourceTest() {
        super(WRAPPER, FIRST_DATASOURCE, SECOND_DATASOURCE);
    }
}
