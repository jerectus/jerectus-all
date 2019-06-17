package jerectus.text2;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.internal.Closure;

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
        var ctx = new TemplateContext(context);
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
