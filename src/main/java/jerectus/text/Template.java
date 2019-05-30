package jerectus.text;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.internal.Closure;

import jerectus.util.BeanProperty;
import jerectus.util.Sys;

public class Template {
    private Template parent;
    private JexlScript script;

    public Template(Template parent, JexlScript script) {
        this.parent = parent;
        this.script = script;
    }

    public Object getTemplateObject(JexlContext ctx) {
        if (parent != null) {
            parent.getTemplateObject(ctx);
        }
        return script.execute(ctx);
    }

    private void writeTo0(Object context, Object out) {
        var isMap = context instanceof Map;
        var ctx = new MapContext(isMap ? Sys.cast(context) : null);
        if (!isMap) {
            BeanProperty.getProperties(context.getClass()).forEach(prop -> {
                ctx.set(prop.getName(), prop.get(context));
            });
        }
        var tmplObj = new LinkedHashMap<String, Object>();
        ctx.set("this", tmplObj);
        ctx.set("out", out);
        getTemplateObject(ctx);
        var render = tmplObj.get("__render__");
        if (render instanceof Closure) {
            ((Closure) render).execute(ctx);
        }
    }

    public void writeTo(Object context, PrintStream out) {
        writeTo0(context, out);
    }

    public void writeTo(Object context, Writer out) {
        writeTo0(context, out instanceof PrintWriter ? out : new PrintWriter(out));
    }

    public String execute(Object context) {
        var out = new StringWriter();
        writeTo(context, out);
        return out.toString();
    }
}
