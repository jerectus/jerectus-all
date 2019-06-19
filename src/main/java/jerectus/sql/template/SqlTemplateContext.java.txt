package jerectus.sql.template;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jerectus.util.template.TemplateContext;
import jerectus.util.Sys;

public class SqlTemplateContext extends TemplateContext {
    private List<Object> params = new ArrayList<>();

    public SqlTemplateContext(Object vars) {
        super(vars);
    }

    public String bind(Object param, boolean head, boolean tail) {
        if (param != null) {
            if (param instanceof Collection) {
                Collection<Object> c = Sys.cast(param);
                if (c.isEmpty()) {
                    params.add(null);
                    return "?";
                }
                for (var v : c) {
                    params.add(v);
                }
                return Sys.repeat("?", c.size(), ",");
            }
            if (param.getClass().isArray()) {
                int n = Array.getLength(param);
                if (n == 0) {
                    params.add(null);
                    return "?";
                }
                for (int i = 0; i < n; i++) {
                    params.add(Array.get(param, i));
                }
                return Sys.repeat("?", n, ",");
            }
            if (head || tail) {
                param = (head ? "%" : "") + SqlTemplate.Functions.escape(param.toString()) + (tail ? "%" : "");
            }
        }
        params.add(param);
        return "?";
    }

    public List<Object> getParameters() {
        return params;
    }
}