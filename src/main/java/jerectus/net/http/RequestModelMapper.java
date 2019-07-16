package jerectus.net.http;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import org.glassfish.grizzly.http.server.Request;

import jerectus.util.Reflect;
import jerectus.util.Sys;
import jerectus.util.logging.Logger;

public class RequestModelMapper {
    private static final Logger log = Logger.getLogger(RequestModelMapper.class);
    private static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static Object convert(Request req, Map<String, String> options) {
        Element elem = new Element();
        req.getParameterMap().forEach((key, value) -> {
            if (key.startsWith("--")) {
                options.put("-submit", key.substring(2));
            } else if (key.startsWith("-")) {
                options.put(key.substring(1), value[0]);
            } else {
                elem.set(key, value);
            }
        });
        log.info(req.getRequestURI(), " : ", options.get("-submit"));
        return elem.toObject();
    }

    public static <T> T convert(Object object, Class<T> type) {
        return objectMapper.convertValue(object, type);
    }

    public static <T> T convert(Request req, Class<T> type, Map<String, String> options) {
        Element elem = new Element();
        req.getParameterMap().forEach((key, value) -> {
            if (key.startsWith("--")) {
                options.put("-submit", key.substring(2));
            } else if (key.startsWith("-")) {
                options.put(key.substring(1), value[0]);
            } else {
                elem.set(key, value);
            }
        });
        log.info(req.getRequestURI(), " : ", options.get("-submit"));
        T model = elem.toObject(type);
        if (model == null) {
            model = Sys.newInstance(type);
            Reflect.invoke(model, "init");
        }
        return model;
    }

    static class Element {
        private static final int VALUE = -1;
        private static final int MAP = -2;

        private int maxIndex = VALUE;
        private Map<String, Element> children;
        private Object value;

        private Element getChild(String key) {
            if (maxIndex != MAP) {
                if (!key.matches("\\d+")) {
                    maxIndex = MAP;
                } else {
                    int index = Integer.parseInt(key);
                    if (index > maxIndex) {
                        maxIndex = index;
                    }
                }
            }
            if (children == null) {
                children = new LinkedHashMap<>();
            }
            return children.computeIfAbsent(key, _0 -> new Element());
        }

        public void set(Object value) {
            if (Sys.isArray(value) && Array.getLength(value) == 1) {
                value = Array.get(value, 0);
            }
            this.value = value;
        }

        public void set(String path, Object value) {
            if (path.endsWith("--default")) {
                var elem = select(path.substring(0, path.length() - 9));
                if (elem.maxIndex == VALUE && elem.value == null) {
                    elem.set(value);
                }
            } else {
                select(path).set(value);
            }
        }

        public Element select(String path) {
            Element e = this;

            for (String key : path.replaceAll("\\[(\\d+)\\]", ".$1").split("\\.")) {
                e = e.getChild(key);
            }

            return e;
        }

        @Override
        public String toString() {
            return children == null ? Sys.toString(value, "") : children.toString();
        }

        public Object toObject() {
            switch (maxIndex) {
            case VALUE:
                return value;
            case MAP:
                Map<String, Object> map = new LinkedHashMap<>();
                children.forEach((key, value) -> {
                    map.put(key, value.toObject());
                });
                return map;
            default:
                List<Object> list = new ArrayList<>(maxIndex + 1);
                for (int i = 0; i <= maxIndex; i++) {
                    list.add(null);
                }
                children.forEach((key, value) -> {
                    list.set(Integer.parseInt(key), value.toObject());
                });
                return list;
            }
        }

        public <T> T toObject(Class<T> type) {
            var values = toObject();
            try {
                return objectMapper.convertValue(values, type);
            } catch (IllegalArgumentException e) {
                if (e.getCause() instanceof InvalidFormatException) {
                    var ife = (InvalidFormatException) e.getCause();
                    log.debug("ife=" + Sys.populate(ife));
                }
                throw new MappingException(e.getCause(), values);
            }
        }
    }

    public static class MappingException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private Object values;

        public MappingException(Throwable cause, Object values) {
            super(cause);
            this.values = values;
        }

        public Object getValues() {
            return values;
        }
    }
}
