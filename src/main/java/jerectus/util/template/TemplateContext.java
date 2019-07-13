package jerectus.util.template;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;

import jerectus.util.Sys;

public class TemplateContext implements JexlContext {
    private JexlContext ctx;
    private Map<String, Object> attr = new HashMap<>();
    EachStat eachStat;

    @SuppressWarnings("unchecked")
    public TemplateContext(Object vars) {
        ctx = vars instanceof JexlContext ? (JexlContext) vars
                : new MapContext(vars instanceof Map ? (Map<String, Object>) vars : Sys.populate(vars));
    }

    @Override
    public Object get(String name) {
        return name.startsWith("__") && attr.containsKey(name) ? attr.get(name) : ctx.get(name);
    }

    @Override
    public void set(String name, Object value) {
        if (name.startsWith("__")) {
            attr.put(name, value);
        } else {
            ctx.set(name, value);
        }
    }

    @Override
    public boolean has(String name) {
        return name.startsWith("__") ? attr.containsKey(name) : ctx.has(name);
    }

    public Object computeIfAbsent(String name, Function<String, Object> fn) {
        if (name.startsWith("__")) {
            return attr.computeIfAbsent(name, fn);
        } else {
            if (!ctx.has(name)) {
                ctx.set(name, fn.apply(name));
            }
            return ctx.get(name);
        }
    }

    public String nameof(String name) {
        var p = Pattern.compile("([_a-zA-Z\\$]\\w*)(.*)");
        var m = p.matcher(name);
        if (m.matches()) {
            String rootName = m.group(1);
            for (var i = eachStat; i != null; i = i.parent) {
                if (i.varName.equals(rootName)) {
                    return i.baseName + "[" + i.index + "]" + m.group(2);
                }
            }
        }
        return name;
    }
}
