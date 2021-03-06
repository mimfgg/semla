package io.semla.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static io.semla.reflect.Types.supplierOf;
import static java.util.stream.Collectors.toCollection;

@SuppressWarnings("unchecked")
public class InstanceContext {

    private final Map<String, Object> cache = new LinkedHashMap<>();

    public <E> Optional<E> getCached(E instance) {
        Model<E> model = Model.of(instance);
        String key = model.toKeyString(instance);
        if (cache.containsKey(key)) {
            return Optional.of((E) cache.get(key));
        }
        return Optional.empty();
    }

    public <E> E remapOrCache(E instance) {
        if (instance != null) {
            Model<E> model = Model.of(instance);
            String key = model.toKeyString(instance);
            if (cache.containsKey(key)) {
                E cached = (E) cache.get(key);
                model.merge(instance, cached);
                return cached;
            }
            cache.put(key, instance);
        }
        return instance;
    }

    public <E, CollectionType extends Collection<E>> CollectionType remapOrCache(CollectionType instances) {
        return instances.stream().map(this::remapOrCache).collect(toCollection(supplierOf(instances.getClass())));
    }
}
