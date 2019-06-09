package jerectus.sql.internal;

import java.nio.file.Path;

import jerectus.util.Resources;

public class SqlFile {
    public static Path get0(Object o, String name) {
        Class<?> clazz = o instanceof Class ? (Class<?>) o : o.getClass();
        return Resources.get(o, "sql/" + Classes.getSimpleName(clazz) + "/" + name + ".sql");
    }
}
