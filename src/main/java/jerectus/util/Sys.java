package jerectus.util;

import java.lang.reflect.Constructor;

public class Sys {
    public static RuntimeException asRuntimeException(Exception e) {
        throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }

    public <T> T newInstance(Constructor<T> c) {
        return Try.get(() -> c.newInstance());
    }

    public <T> T newInstance(Class<T> clazz) {
        return Try.get(() -> newInstance(clazz.getConstructor()));
    }
}