package jerectus.util;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sys {
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
        return Sys.replace(s.toLowerCase(), "_(\\w)", m -> m.group(1).toUpperCase());
    }

    public static String snakeCase(String s) {
        return s.replaceAll("([a-z])([A-Z])", "$1_$2");
    }

    public static RuntimeException asRuntimeException(Exception e) {
        throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }

    public static RuntimeException asRuntimeException(Exception e, Object... params) {
        throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }

    public static <T> T newInstance(Constructor<T> c) {
        return Try.get(() -> c.newInstance());
    }

    public static <T> T newInstance(Class<T> clazz) {
        return Try.get(() -> newInstance(clazz.getConstructor()));
    }

    public static Map<String, Object> populate(Object bean) {
        var map = new LinkedHashMap<String, Object>();
        BeanProperty.getProperties(bean.getClass()).forEach(p -> map.put(p.getName(), p.get(bean)));
        return map;
    }

    public static String replace(String text, Pattern ptn, Function<Matcher, String> fn) {
        StringBuilder sb = new StringBuilder();
        Matcher m = ptn.matcher(text);
        int i = 0;
        while (m.find()) {
            sb.append(text.substring(i, m.start()));
            sb.append(fn.apply(m));
            i = m.end();
        }
        sb.append(text.substring(i));
        return sb.toString();
    }

    public static String replace(String text, String ptn, Function<Matcher, String> fn) {
        return replace(text, Pattern.compile(ptn), fn);
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object value) {
        return (T) value;
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
}