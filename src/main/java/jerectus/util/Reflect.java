package jerectus.util;

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

    public static Method getMethod(Object object, String methodName, Object... args) {
        try {
            Class<?> clazz = object.getClass();
            if (args.length == 0) {
                return clazz.getMethod(methodName);
            } else {
                for (var m : clazz.getMethods()) {
                    if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
                        for (int i = 0;; i++) {
                            if (i >= args.length)
                                return m;
                            if (!isAssignableFrom(m.getParameterTypes()[i], args[i])) {
                                break;
                            }
                        }
                    }
                }
                throw new RuntimeException(new NoSuchMethodException(methodName));
            }
        } catch (Exception e) {
            throw Sys.asRuntimeException(e);
        }
    }

    public static Object invoke(Object object, String methodName, Object... args) {
        try {
            return getMethod(object, methodName, args).invoke(object, args);
        } catch (Exception e) {
            throw Sys.asRuntimeException(e);
        }
    }
}