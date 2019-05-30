package jerectus.sql.internal;

public class Classes {
    public static String getSimpleName(Class<?> clazz) {
        String name = clazz.getSimpleName();
        if (name.isEmpty()) {
            name = clazz.getSuperclass().getSimpleName();
        }
        return name;
    }
}
