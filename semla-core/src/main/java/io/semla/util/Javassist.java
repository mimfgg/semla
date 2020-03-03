package io.semla.util;

import io.semla.reflect.Modifier;
import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static io.semla.util.Unchecked.unchecked;

@SuppressWarnings("unchecked")
public class Javassist {

    private static final ReentrantLock lock = new ReentrantLock(true);
    private static final HashMap<String, Class<?>> JAVASSIST_CLASSES = new HashMap<>();

    private Javassist() {
    }

    public static <T> Class<T> getOrCreate(String classname, UnaryOperator<ClassBuilder> builder) {
        Class<T> clazz = (Class<T>) JAVASSIST_CLASSES.get(classname);
        if (clazz == null) {
            try {
                lock.lock();
                clazz = (Class<T>) unchecked(() -> {
                    if (!JAVASSIST_CLASSES.containsKey(classname)) {
                        Class<T> newClass = builder.apply(new ClassBuilder(classname)).create();
                        JAVASSIST_CLASSES.put(classname, newClass);
                        return newClass;
                    } else {
                        return JAVASSIST_CLASSES.get(classname);
                    }
                });
            } finally {
                lock.unlock();
            }
        }
        return clazz;
    }

    public static class ClassBuilder {

        final CtClass ctClass;
        final Map<Class<?>, Map<String, Object>> annotations = new LinkedHashMap<>();

        private ClassBuilder(String classname) {
            ctClass = ClassPool.getDefault().makeClass(classname);
        }

        public ClassBuilder extending(Class<?> superClass) {
            return extending(superClass.getCanonicalName());
        }

        public ClassBuilder extending(String name) {
            unchecked(() -> ctClass.setSuperclass(ClassPool.getDefault().get(name)));
            return this;
        }

        public ClassBuilder implementing(Class<?>... interfaceClass) {
            return implementing(Stream.of(interfaceClass).map(Class::getCanonicalName).toArray(String[]::new));
        }

        public ClassBuilder implementing(String... interfaceClass) {
            ctClass.setInterfaces(Stream.of(interfaceClass)
                .map(classname -> unchecked(() -> ClassPool.getDefault().get(classname)))
                .toArray(CtClass[]::new));
            return this;
        }

        public ClassBuilder addAnnotation(Class<? extends java.lang.annotation.Annotation> annotationType) {
            return addAnnotation(annotationType, UnaryOperator.identity());
        }

        public ClassBuilder addAnnotation(Class<? extends java.lang.annotation.Annotation> annotationType, UnaryOperator<AnnotationBuilder> function) {
            Map<String, Object> values = new LinkedHashMap<>();
            annotations.put(annotationType, values);
            function.apply(new AnnotationBuilder(values));
            return this;
        }

        public ClassBuilder addField(String fieldName, Class<?> fieldType) {
            return addField(fieldName, fieldType, UnaryOperator.identity());
        }

        public ClassBuilder addField(String fieldName, Class<?> fieldType, UnaryOperator<MemberBuilder<CtField>> function) {
            unchecked(() -> {
                CtField ctField = new CtField(ClassPool.getDefault().get(fieldType.getCanonicalName()), fieldName, ctClass);
                ctClass.addField(function.apply(new MemberBuilder<>(ctField, ctField.getFieldInfo()::addAttribute)).get());
            });
            return this;
        }

        public ClassBuilder addMethod(String source) {
            return addMethod(source, UnaryOperator.identity());
        }

        public ClassBuilder addMethod(String source, UnaryOperator<MemberBuilder<CtMethod>> function) {
            unchecked(() -> {
                CtMethod ctMethod = CtNewMethod.make(source, ctClass);
                ctClass.addMethod(function.apply(new MemberBuilder<>(ctMethod, ctMethod.getMethodInfo()::addAttribute)).get());
            });
            return this;
        }

        public ClassBuilder addConstructor(String source) {
            return addConstructor(source, UnaryOperator.identity());
        }

        public ClassBuilder addConstructor(String source, UnaryOperator<MemberBuilder<CtConstructor>> function) {
            unchecked(() -> {
                CtConstructor ctConstructor = CtNewConstructor.make(source, ctClass);
                ctClass.addConstructor(function.apply(new MemberBuilder<>(ctConstructor, ctConstructor.getMethodInfo()::addAttribute)).get());
            });
            return this;
        }

        private <T> Class<T> create() throws CannotCompileException {
            if (!annotations.isEmpty()) {
                ctClass.getClassFile().addAttribute(toAnnotationsAttribute(ctClass.getClassFile().getConstPool(), annotations));
            }
            return (Class<T>) ctClass.toClass();
        }

        public static class AnnotationBuilder {

            private final Map<String, Object> values;

            private AnnotationBuilder(Map<String, Object> values) {
                this.values = values;
            }

            public AnnotationBuilder set(String name, Object value) {
                if (value == null) {
                    throw new IllegalArgumentException("value cannot be null for " + name);
                }
                values.put(name, value);
                return this;
            }
        }

        public class MemberBuilder<MemberType extends CtMember> {

            private final MemberType ctMember;
            private final Map<Class<?>, Map<String, Object>> annotations = new LinkedHashMap<>();
            private final Consumer<AnnotationsAttribute> annotationTarget;

            private MemberBuilder(MemberType ctMember, Consumer<AnnotationsAttribute> annotationTarget) {
                this.ctMember = ctMember;
                this.annotationTarget = annotationTarget;
            }

            public MemberBuilder<MemberType> setModifier(io.semla.reflect.Modifier... modifiers) {
                ctMember.setModifiers(Modifier.valueOf(modifiers));
                return this;
            }

            public MemberBuilder<MemberType> addAnnotation(Class<?> annotationType) {
                return addAnnotation(annotationType, UnaryOperator.identity());
            }

            public MemberBuilder<MemberType> addAnnotation(Class<?> annotationType, UnaryOperator<AnnotationBuilder> annotationFunction) {
                Map<String, Object> values = new LinkedHashMap<>();
                annotations.put(annotationType, values);
                annotationFunction.apply(new AnnotationBuilder(values));
                return this;
            }

            private MemberType get() {
                if (!annotations.isEmpty()) {
                    annotationTarget.accept(toAnnotationsAttribute(ctClass.getClassFile().getConstPool(), annotations));
                }
                return ctMember;
            }
        }

        private static AnnotationsAttribute toAnnotationsAttribute(ConstPool constPool, Map<Class<?>, Map<String, Object>> annotations) {
            AnnotationsAttribute annotationsAttribute = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
            annotations.forEach((annotationType, values) -> {
                Annotation annotation = new Annotation(annotationType.getCanonicalName(), constPool);
                values.forEach((name, value) -> {
                    if (value instanceof String) {
                        annotation.addMemberValue(name, new StringMemberValue((String) value, constPool));
                    } else if (value.getClass().isEnum()) {
                        EnumMemberValue enumMemberValue = new EnumMemberValue(constPool);
                        enumMemberValue.setType(value.getClass().getCanonicalName());
                        enumMemberValue.setValue(String.valueOf(value));
                        annotation.addMemberValue(name, enumMemberValue);
                    } else {
                        throw new IllegalArgumentException("cannot create a ctMember value out of " + value);
                    }
                });
                annotationsAttribute.addAnnotation(annotation);
            });
            return annotationsAttribute;
        }
    }
}
