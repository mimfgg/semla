package io.semla.datasource;

import io.semla.config.DatasourceConfiguration;
import io.semla.inject.Injector;
import io.semla.inject.TypedFactory;
import io.semla.model.EntityModel;
import io.semla.reflect.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Singleton
public class DatasourceFactory extends TypedFactory<Datasource<?>> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Map<Type, Datasource<?>> datasourcesByType = new LinkedHashMap<>();
    private final Injector injector;

    private DatasourceConfiguration defaultDatasourceConfiguration = new DatasourceConfiguration() {
        @Override
        public <T> Datasource<T> create(EntityModel<T> entityModel) {
            throw new IllegalStateException("no default datasource is set and " + entityModel.getType() + " hasn't been explicitely registered!");
        }
    };

    @Inject
    public DatasourceFactory(Injector injector) {
        this.injector = injector;
    }

    @Override
    public Datasource<?> create(Type type, Annotation[] annotations) {
        return of(Types.typeArgumentOf(type));
    }

    public void setDefaultDatasourceConfiguration(DatasourceConfiguration defaultDatasourceConfiguration) {
        this.defaultDatasourceConfiguration = defaultDatasourceConfiguration;
    }

    public void registerDatasource(Datasource<?> datasource) {
        if (datasourcesByType.containsKey(datasource.model().getType())) {
            logger.warn("replacing " + datasourcesByType.get(datasource.model().getType()) + " by " + datasource);
        }
        injector.inject(datasource);
        datasourcesByType.put(datasource.model().getType(), datasource);
    }

    @SuppressWarnings("unchecked")
    public <T, DatasourceType extends Datasource<T>> DatasourceType of(Class<T> clazz) {
        if (!datasourcesByType.containsKey(clazz)) {
            registerDatasource(defaultDatasourceConfiguration.create(EntityModel.of(clazz)));
        }
        return (DatasourceType) datasourcesByType.get(clazz);
    }

    public Collection<Datasource<?>> datasources() {
        return datasourcesByType.values();
    }


}
