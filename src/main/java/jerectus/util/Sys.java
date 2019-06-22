package jerectus.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import jerectus.util.regex.Regex;

public class Sys {
    public static String toString(Object value, String other) {
        return value != null ? value.toString() : other;
    }

    public static String toString(Object value) {
        return toString(value, null);
    }

    public static <T> boolean eq(T a, T b) {
        if (a == b)
            return true;
        if (a == null || b == null)
            return false;
        return a.equals(b);
    }

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static String ifEmpty(String s, String t) {
        return !isEmpty(s) ? s : t;
    }

    public static boolean isArray(Object o) {
        return o != null && o.getClass().isArray();
    }

    public static String substring(String s, int start, int end) {
        if (start < 0) {
            start = s.length() + start;
        }
        if (start < 0) {
            start = 0;
        } else if (start > s.length()) {
            start = s.length();
        }
        if (end < 0) {
            end = s.length() + end;
        }
        if (end < 0) {
            end = 0;
        } else if (end > s.length()) {
            end = s.length();
        }
        return s.substring(start, end);
    }

    public static String substring(String s, int start) {
        return substring(s, start, Integer.MAX_VALUE);
    }

    public static String capitalize(String s) {
        if (s == null || s.isEmpty())
            return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static String uncapitalize(String s) {
        if (s == null || s.isEmpty())
            return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    public static String camelCase(String s) {
        return Regex.replace(s.toLowerCase(), "_(\\w)", m -> m.group(1).toUpperCase());
    }

    public static String snakeCase(String s) {
        return s.replaceAll("([a-z])([A-Z])", "$1_$2");
    }

    public static RuntimeException asRuntimeException(Exception e) {
        throw e instanceof RuntimeException ? (RuntimeException) e
                : e instanceof IOException ? new UncheckedIOException((IOException) e) : new RuntimeException(e);
    }

    public static <T> T newInstance(Constructor<T> c) {
        return Try.get(() -> c.newInstance());
    }

    public static <T> T newInstance(Class<T> clazz) {
        return Try.get(() -> newInstance(clazz.getConstructor()));
    }

    public static Map<String, Object> populate(Object bean) {
        if (bean == null)
            return null;
        var map = new LinkedHashMap<String, Object>();
        BeanProperty.getProperties(bean.getClass()).forEach(p -> map.put(p.getName(), p.get(bean)));
        return map;
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object value) {
        return (T) value;
    }

    @SuppressWarnings("unchecked")
    public static <T, R> R cast(T value, Class<R> clazz, Function<T, R> fn) {
        return clazz.isAssignableFrom(value.getClass()) ? (R) value : fn.apply(value);
    }

    public static <T> void addAll(Collection<T> c, T[] values) {
        for (var value : values) {
            c.add(value);
        }
    }

    public static String repeat(String s, int n, String delim) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(delim);
            }
            sb.append(s);
        }
        return sb.toString();
    }

    public static <T> String join(Iterable<T> o, String delim, Function<T, String> fn) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (var it : o) {
            if (first) {
                first = false;
            } else {
                sb.append(delim);
            }
            sb.append(fn.apply(it));
        }
        return sb.toString();
    }

    public static String join(String[] o, String delim, Function<String, String> fn) {
        return join(new ArrayIterable<String>(o), delim, fn);
    }

    @SuppressWarnings("unchecked")
    public static Iterator<Object> iterator(Object o) {
        if (o == null) {
            return Sys.cast(Collections.EMPTY_LIST.iterator());
        }
        if (o instanceof Iterator) {
            return Sys.cast(o);
        }
        if (o instanceof Iterable) {
            return ((Iterable<Object>) o).iterator();
        }
        if (o instanceof Map) {
            return cast(((Map<?, ?>) o).entrySet().iterator());
        }
        return new ArrayIterator<Object>(isArray(o) ? o : new Object[] { o });
    }

    public static <T> Iterable<T> each(Iterator<T> it) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return it;
            }
        };
    }

    public static <T> Iterable<T> each(Enumeration<T> enm) {
        return each(new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return enm.hasMoreElements();
            }

            @Override
            public T next() {
                return enm.nextElement();
            }
        });
    }

    public static <T> Iterable<T> each(Object o) {
        if (o == null)
            return cast(Collections.EMPTY_LIST);
        if (o instanceof Iterable) {
            return cast(o);
        }
        if (o instanceof Map) {
            return cast(((Map<?, ?>) o).entrySet());
        }
        if (isArray(o)) {
            return new ArrayIterable<T>(o);
        }
        return cast(Arrays.asList(o));
    }

    public static <T> Iterable<T> iterable(Consumer<Generator<T>> fn) {
        return () -> new YieldIterator<T>() {
            @Override
            public void generate() {
                fn.accept(this);
            }
        };
    }

    public static int size(Object o) {
        if (o == null) {
            return 0;
        } else if (o instanceof Collection) {
            Collection<Object> c = Sys.cast(o);
            return c.size();
        } else if (o.getClass().isArray()) {
            return Array.getLength(o);
        } else {
            return 1;
        }
    }

    public static <T, C extends Collection<T>> C copy(C c, Iterable<T> it) {
        for (var v : it) {
            c.add(v);
        }
        return c;
    }

    public static <T> T eval(T it, Consumer<T> fn) {
        fn.accept(it);
        return it;
    }

    public static void closeQuietly(AutoCloseable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
                // do nothing
            }
        }
    }
}