package io.semla.query;

import io.semla.model.EntityModel;
import io.semla.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ContextualQuery<T> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final EntityModel<T> model;
    protected final PersistenceContext context;

    protected ContextualQuery(PersistenceContext context, EntityModel<T> model) {
        this.context = context;
        this.model = model;
    }

    public PersistenceContext context() {
        return context;
    }

    public EntityModel<T> model() {
        return model;
    }
}
