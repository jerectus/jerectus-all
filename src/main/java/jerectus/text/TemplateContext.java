package jerectus.text;

import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.internal.Closure;

import jerectus.util.Sys;

public class TemplateContext implements JexlContext {
    JexlContext ctx;
    EachStat eachStat;

    @SuppressWarnings("unchecked")
    public TemplateContext(Object vars) {
        ctx = vars instanceof JexlContext ? (JexlContext) vars
                : new MapContext(vars instanceof Map ? (Map<String, Object>) vars : Sys.populate(vars));
        set("__ctx", this);
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

    public void forEach(Object values, String valuesExpr, String varName, Closure fn) {
        eachStat = new EachStat(eachStat, values, valuesExpr, varName, nameof(valuesExpr));
        for (var it : eachStat) {
            fn.execute(this, it.value, it);
        }
        eachStat = eachStat.parent;
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
