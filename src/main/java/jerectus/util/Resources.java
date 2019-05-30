package jerectus.util;

import java.nio.file.Path;
import java.nio.file.Paths;

import jerectus.io.IO;

public class Resources {
    public static Path get(Object o, String name) {
        return Try.get(() -> {
            Class<?> clazz = o == null ? ClassLoader.class : o instanceof Class ? (Class<?>) o : o.getClass();
            return Paths.get(clazz.getResource(name).toURI());
        });
    }

    public static String load(Object o, String path) {
        Class<?> clazz = o == null ? ClassLoader.class : o instanceof Class ? (Class<?>) o : o.getClass();
        return IO.load(get(clazz, path));
    }
}
