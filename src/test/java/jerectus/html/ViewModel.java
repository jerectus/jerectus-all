package jerectus.html;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.glassfish.grizzly.http.server.Request;

import jerectus.util.Reflect;
import jerectus.util.Sys;

class Element {
    private static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
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
        this.value = value;
    }

    public void set(String path, Object value) {
        select(path).set(value);
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
        return objectMapper.convertValue(toObject(), type);
    }
}

public class ViewModel {
    public static <T> T convert(Class<T> type, Request req) {
        Element elem = new Element();
        String[] cmd = { null };
        req.getParameterMap().forEach((key, value) -> {
            if (key.startsWith("--")) {
                cmd[0] = key.substring(2);
            } else {
                elem.set(key, value);
            }
        });
        T model = elem.toObject(type);
        if (model == null) {
            model = Sys.newInstance(type);
            Reflect.invoke(model, "init");
        }
        if (cmd[0] != null) {
            Reflect.invoke(model, cmd[0]);
        }
        return model;
    }
}
