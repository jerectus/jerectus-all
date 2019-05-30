package jerectus.sql.template;

import java.util.LinkedHashMap;
import java.util.Map;

import jerectus.util.Sys;

public class Param extends LinkedHashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    public Param(Object context) {
        if (context == null) {
            // do nothing
        } else if (context instanceof Map) {
            putAll(Sys.cast(context));
        } else {
            putAll(Sys.populate(context));
        }
    }

    @Override
    public boolean containsKey(Object key) {
        return true;
    }
}
