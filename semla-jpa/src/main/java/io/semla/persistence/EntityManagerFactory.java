package io.semla.persistence;

import io.semla.datasource.DatasourceFactory;
import io.semla.inject.Injector;
import io.semla.inject.TypedFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.semla.reflect.Types.rawTypeArgumentOf;

@Singleton
public class EntityManagerFactory extends TypedFactory<EntityManager<?>> {

    private final Map<Type, EntityManager<?>> entityManagersByType = new LinkedHashMap<>();

    private final Injector injector;
    private final DatasourceFactory datasourceFactory;

    @Inject
    public EntityManagerFactory(Injector injector, DatasourceFactory datasourceFactory) {
        this.injector = injector;
        this.datasourceFactory = datasourceFactory;
    }

    @SuppressWarnings("unchecked")
    public <T> EntityManager<T> of(Class<T> clazz) {
        return (EntityManager<T>) entityManagersByType.computeIfAbsent(clazz,
            type -> new EntityManager<>(datasourceFactory.of(clazz), this)
        );
    }

    public PersistenceContext newContext() {
        return new PersistenceContext(this);
    }

    @Override
    public EntityManager<?> create(Type type, Annotation[] annotations) {
        return of(rawTypeArgumentOf(type));
    }

    public Injector injector() {
        return injector;
    }
}
