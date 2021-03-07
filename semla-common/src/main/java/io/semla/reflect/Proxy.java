package io.semla.reflect;

import javassist.util.proxy.ProxyFactory;

import java.lang.reflect.InvocationHandler;
import java.util.stream.Stream;

import static io.semla.util.Unchecked.unchecked;

@SuppressWarnings("unchecked")
public final class Proxy {

    private Proxy() {}

    public static <T> T of(Class<T> clazz, InvocationHandler invocationHandler) {
        return of(clazz, new Object[0], invocationHandler);
    }

    public static <T> T of(Class<T> clazz, Object[] constructorArgs, InvocationHandler invocationHandler) {
        return of(clazz, Stream.of(constructorArgs).map(Object::getClass).toArray(Class[]::new), constructorArgs, invocationHandler);
    }

    public static <T> T of(Class<T> clazz, Class<?>[] constructorTypes, Object[] constructorArgs, InvocationHandler invocationHandler) {
        if (clazz.isInterface()) {
            return (T) java.lang.reflect.Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, invocationHandler);
        } else {
            ProxyFactory factory = new ProxyFactory();
            factory.setSuperclass(clazz);
            return (T) unchecked(() -> factory.create(constructorTypes, constructorArgs,
                (proxy, method, proceed, args) -> invocationHandler.invoke(proxy, method, args))
            );
        }
    }

}
