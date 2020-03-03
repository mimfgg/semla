package io.semla.datasource;

import io.semla.config.DatasourceConfiguration;
import io.semla.cucumber.steps.EntitySteps;
import io.semla.model.Player;
import org.junit.After;
import org.junit.Before;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public abstract class CompositeDatasourceTest<DatasourceType extends Datasource<Player>> extends DatasourceTest {

    private final UnaryOperator<DatasourceConfiguration> wrapper;
    private final Function<DatasourceType, Datasource<Player>> firstDatasource;
    private final Function<DatasourceType, Datasource<Player>> secondDatasource;

    protected DatasourceConfiguration defaultDatasource;
    protected Datasource<Player> datasource1;
    protected Datasource<Player> datasource2;

    protected CompositeDatasourceTest(UnaryOperator<DatasourceConfiguration> wrapper,
                                      Function<DatasourceType, Datasource<Player>> firstDatasource,
                                      Function<DatasourceType, Datasource<Player>> secondDatasource) {
        this.wrapper = wrapper;
        this.firstDatasource = firstDatasource;
        this.secondDatasource = secondDatasource;
    }

    @Before
    public void before() {
        this.defaultDatasource = EntitySteps.getDefaultDatasource();
        EntitySteps.setDefaultDatasource(wrapper.apply(defaultDatasource));
        datasource1 = firstDatasource.apply(EntitySteps.datasourceOf(Player.class));
        datasource2 = secondDatasource.apply(EntitySteps.datasourceOf(Player.class));
        super.before();
    }

    @After
    public void after() {
        super.after();
        EntitySteps.setDefaultDatasource(defaultDatasource);
    }
}
