package jerectus.validation;

import java.lang.annotation.Annotation;
import java.util.concurrent.ConcurrentHashMap;

import jerectus.util.BeanProperty;
import jerectus.util.Reflect;
import jerectus.util.Sys;

public class Validator {
    public <T> void validate(T bean) {
        for (var prop : BeanProperty.getProperties(bean.getClass())) {
            var value = prop.get(bean);
            for (var anno : prop.getAnnotations()) {
                validate(anno, new ValidationContext() {
                    @Override
                    public Annotation constraint() {
                        return anno;
                    }

                    @Override
                    public Object value() {
                        return value;
                    }

                    @Override
                    public BeanProperty property() {
                        return prop;
                    }

                    @Override
                    public Object bean() {
                        return bean;
                    }
                });
            }
        }
        Reflect.invoke(bean, "validate");
    }

    public void validate(Annotation constraint, ValidationContext ctx) {
        Sys.with(Reflect.getNestClass(constraint.annotationType(), "Fn"), fn -> {
            Reflect.invoke(fn, "validate", constraint, ctx);
        });
    }

    private static final ConcurrentHashMap<String, Validator> validators = new ConcurrentHashMap<>();

    public static Validator getInstance() {
        return validators.computeIfAbsent("", key -> new Validator());
    }
}