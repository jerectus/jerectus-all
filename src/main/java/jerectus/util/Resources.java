package jerectus.util;

import java.nio.file.Path;

import jerectus.io.IO;

public class Resources {
    public static Path get(Object o, String name) {
        return Try.get(() -> {
            if (o == null) {
                return IO.toPath(Thread.currentThread().getContextClassLoader().getResource(name));
            } else {
                Class<?> clazz = o instanceof Class ? (Class<?>) o : o.getClass();
                return IO.toPath(clazz.getResource(name));
            }
        });
    }

    public static String load(Object o, String path) {
        return IO.load(get(o, path));
    }

    public static Path getMember(Object o, String name) {
        Class<?> clazz = o instanceof Class ? (Class<?>) o : o.getClass();
        return get(clazz, "res/" + clazz.getSimpleName() + "/" + name);
    }
}
