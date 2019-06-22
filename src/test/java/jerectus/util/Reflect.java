package jerectus.util;

import java.lang.reflect.Method;

public class Reflect {
    public static boolean isAssignableFrom(Class<?> type, Object value) {
        if (value == null) {
            return !type.isPrimitive();
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
                        boolean found = true;
                        for (int i = 0; i < args.length; i++) {
                            if (!isAssignableFrom(m.getParameterTypes()[i], args[i])) {
                                found = false;
                                break;
                            }
                        }
                        if (found) {
                            return m;
                        }
                    }
                }
                throw new RuntimeException();
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