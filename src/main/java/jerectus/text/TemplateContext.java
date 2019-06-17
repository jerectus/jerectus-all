package jerectus.text;

import java.util.Map;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.internal.Closure;

import jerectus.util.Sys;

public class TemplateContext implements JexlContext {
    JexlContext ctx;

    public TemplateContext(JexlContext ctx) {
        this.ctx = ctx;
    }

    public TemplateContext(Object vars) {
        this(new MapContext(vars instanceof Map ? Sys.cast(vars) : Sys.populate(vars)));
        set("__root", this);
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
        return ctx.has("__has") || ctx.has(name);
    }

    public EachContext each(Object values, String valuesExpr, String varName) {
        return new EachContext(this, values, valuesExpr, varName);
    }

    public void forEach(Object values, String valuesExpr, String varName, Closure fn) {
        var e = new EachContext(this, values, valuesExpr, varName);
        for (var it : e) {
            fn.execute(e, e, it.value, it);
        }
    }

    public String nameof(String expr) {
        return expr;
    }
}
