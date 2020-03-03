package io.semla.persistence;

import io.semla.inject.TypedFactory;
import io.semla.reflect.Types;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.semla.util.Unchecked.unchecked;

@Singleton
public class TypedEntityManagerFactory extends TypedFactory<TypedEntityManager<?, ?, ?, ?, ?, ?, ?, ?, ?>> {

    private final Map<Type, TypedEntityManager<?, ?, ?, ?, ?, ?, ?, ?, ?>> typedEntityManagersByType = new LinkedHashMap<>();
    private final EntityManagerFactory entityManagerFactory;

    @Inject
    public TypedEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public TypedEntityManager<?, ?, ?, ?, ?, ?, ?, ?, ?> create(Type type, Annotation[] annotations) {
        return typedEntityManagersByType.computeIfAbsent(type, t -> {
            Class<? extends TypedEntityManager<?, ?, ?, ?, ?, ?, ?, ?, ?>> managerType = Types.rawTypeOf(type);
            EntityManager<?> entityManager = entityManagerFactory.of(Types.typeArgumentOf(managerType.getGenericSuperclass(), 1));
            return unchecked(() -> managerType.getConstructor(EntityManager.class).newInstance(entityManager));
        });
    }

    @Override
    public String toString() {
        return "io.semla.persistence.TypedEntityManagerFactory";
    }
}
