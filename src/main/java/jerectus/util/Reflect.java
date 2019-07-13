package jerectus.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Reflect {
    public static boolean isAssignableFrom(Class<?> type, Object value) {
        if (value == null) {
            return !type.isPrimitive();
        }
        if (type == int.class) {
            return value instanceof Integer;
        } else if (type == char.class) {
            return value instanceof Character;
        } else if (type.isPrimitive()) {
            return type.getName().equals(value.getClass().getSimpleName().toLowerCase());
        }
        return type.isAssignableFrom(value.getClass());
    }

    public static Method getMethod(Class<?> clazz, String methodName, Object... args) {
        if (args.length == 0) {
            try {
                return clazz.getMethod(methodName);
            } catch (NoSuchMethodException e) {
                return null;
            } catch (Exception e) {
                throw Sys.asRuntimeException(e);
            }
        } else {
            for (var m : clazz.getMethods()) {
                var params = m.getParameters();
                if (m.getName().equals(methodName) && params.length == args.length) {
                    for (int i = 0;; i++) {
                        if (i >= args.length)
                            return m;
                        if (!isAssignableFrom(params[i].getType(), args[i])) {
                            break;
                        }
                    }
                }
            }
            return null;
        }
    }

    public static Object invoke(Object object, String methodName, Object... args) {
        try {
            var m = getMethod(object.getClass(), methodName, args);
            return m == null ? null : m.invoke(object, args);
        } catch (InvocationTargetException e) {
            throw Sys.asRuntimeException(e.getCause());
        } catch (Exception e) {
            throw Sys.asRuntimeException(e);
        }
    }

    public static Object invoke(Class<?> clazz, String methodName, Object... args) {
        try {
            var m = getMethod(clazz, methodName, args);
            return m == null ? null : m.invoke(null, args);
        } catch (InvocationTargetException e) {
            throw Sys.asRuntimeException(e.getCause());
        } catch (Exception e) {
            throw Sys.asRuntimeException(e);
        }
    }

    public static Class<?> getNestClass(Class<?> clazz, String name) {
        for (var c : clazz.getNestMembers()) {
            if (c.getSimpleName().equals(name)) {
                return c;
            }
        }
        return null;
    }
}