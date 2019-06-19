package jerectus.util.template;

import java.util.Map;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;

import jerectus.util.Sys;

public class TemplateContext implements JexlContext {
    JexlContext ctx;
    EachStat eachStat;

    @SuppressWarnings("unchecked")
    public TemplateContext(Object vars) {
        ctx = vars instanceof JexlContext ? (JexlContext) vars
                : new MapContext(vars instanceof Map ? (Map<String, Object>) vars : Sys.populate(vars));
    }

    @Override
    public Object get(String name) {
        return ctx.get(name);
    }

    @Override
    public void set(String name, Object value) {
        ctx.set(name, value);
    }

    @Override
    public boolean has(String name) {
        return ctx.has(name);
    }
}
