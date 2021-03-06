package io.semla.datasource;

import io.semla.Semla;
import io.semla.model.EntityModel;
import io.semla.model.Fruit;
import io.semla.reflect.TypeReference;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DatasourceFactoryTest {

    @Test
    public void no_default_factory() {
        DatasourceFactory datasourceFactory = Semla.create().getInstance(DatasourceFactory.class);
        assertThatThrownBy(() -> datasourceFactory.of(Fruit.class))
            .hasMessage("no default datasource is set and class io.semla.model.Fruit hasn't been explicitely registered!");
    }

    @Test
    public void as_a_factory() {
        Datasource<Fruit> datasource = Semla.configure()
            .withDefaultDatasource(InMemoryDatasource.configure())
            .create()
            .getInstance(new TypeReference<Datasource<Fruit>>() {});
        assertThat(datasource).isNotNull();
    }

    @Test
    public void redefining_a_datasource() {
        DatasourceFactory datasourceFactory = Semla.configure()
            .withDefaultDatasource(InMemoryDatasource.configure())
            .create()
            .getInstance(DatasourceFactory.class);
        datasourceFactory.registerDatasource(InMemoryDatasource.configure().create(EntityModel.of(Fruit.class)));
        Datasource<Fruit> datasource = datasourceFactory.of(Fruit.class);
        assertThat(datasource).isNotNull();
        datasourceFactory.registerDatasource(InMemoryDatasource.configure().create(EntityModel.of(Fruit.class)));
        Datasource<Fruit> newDatasource = datasourceFactory.of(Fruit.class);
        assertThat(newDatasource).isNotNull();
        assertThat(newDatasource).isNotEqualTo(datasource);
    }
}
