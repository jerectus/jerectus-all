package jerectus.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedHashMap;

public class BeanProperty {
    private String name;
    private Method getter;
    private Method setter;
    private Field field;

    public BeanProperty(String name) {
        this.name = name;
    }

    public static Collection<BeanProperty> getProperties(Class<?> clazz) {
        var map = new LinkedHashMap<String, BeanProperty>();
        var fn = new Object() {
            BeanProperty getProperty(Method m) {
                m.setAccessible(true);
                return getProperty(Sys.uncapitalize(m.getName().substring(3)));
            }

            BeanProperty getProperty(String name) {
                return map.computeIfAbsent(name, _0 -> new BeanProperty(name));
            }
        };
        for (var m : clazz.getMethods()) {
            if (Modifier.isStatic(m.getModifiers()) || m.getDeclaringClass().getName().startsWith("java.")) {
                // do nothing
            } else if (m.getParameterCount() == 0 && m.getName().matches("get[A-Z].*")) {
                fn.getProperty(m).getter = m;
            } else if (m.getParameterCount() == 1 && m.getName().matches("set[A-Z].*")) {
                fn.getProperty(m).setter = m;
            }
        }
        for (var f : clazz.getFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                f.setAccessible(true);
                fn.getProperty(f.getName()).field = f;
            }
        }
        return map.values();
    }

    public String getName() {
        return name;
    }

    public Object get(Object bean) {
        try {
            return getter != null ? getter.invoke(bean) : field != null ? field.get(bean) : null;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw Sys.asRuntimeException(e);
        }
    }

    public void set(Object bean, Object value) {
        try {
            if (setter != null) {
                setter.invoke(bean, value);
            } else if (field != null) {
                field.set(bean, value);
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw Sys.asRuntimeException(e);
        }
    }

    public Class<?> getType() {
        return getter != null ? getter.getReturnType() : field != null ? field.getType() : null;
    }

    public <T extends Annotation> T getAnnotation(Class<T> clazz) {
        T result;
        if (getter != null && (result = getter.getAnnotation(clazz)) != null)
            return result;
        if (setter != null && (result = setter.getAnnotation(clazz)) != null)
            return result;
        if (field != null && (result = field.getAnnotation(clazz)) != null)
            return result;
        return null;
    }
}
