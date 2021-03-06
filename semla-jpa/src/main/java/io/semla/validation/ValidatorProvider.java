package io.semla.validation;

import io.semla.inject.TypedFactory;
import io.semla.reflect.Proxy;
import io.semla.util.ImmutableSet;
import org.slf4j.LoggerFactory;

import javax.validation.NoProviderFoundException;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class ValidatorProvider extends TypedFactory<Validator> {

    private Validator validator;

    public ValidatorProvider() {
        try {
            validator = Validation.buildDefaultValidatorFactory().getValidator();
        } catch (NoProviderFoundException e) {
            LoggerFactory.getLogger(this.getClass()).warn("no io.semla.validation provider found, io.semla.validation is disabled!");
            validator = Proxy.of(Validator.class, (proxy, method, args) -> ImmutableSet.empty());
        }
    }

    @Override
    public Validator create(Type type, Annotation[] annotations) {
        return validator;
    }
}
