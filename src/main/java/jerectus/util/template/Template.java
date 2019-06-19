package jerectus.util.template;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.internal.Closure;

import jerectus.util.Try;
import jerectus.util.regex.Regex;

public class Template {
    private static ThreadLocal<TemplateContext> currentContext = new ThreadLocal<>();
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
        try {
            var ctx = context instanceof TemplateContext ? (TemplateContext) context : new TemplateContext(context);
            currentContext.set(ctx);
            var tmplObj = new LinkedHashMap<String, Object>();
            ctx.set("this", tmplObj);
            ctx.set("out", out);
            getTemplateObject(ctx);
            var render = tmplObj.get("__render__");
            if (render instanceof Closure) {
                ((Closure) render).execute(ctx);
            }
        } finally {
            currentContext.set(null);
        }
    }

    public void writeTo(Object context, PrintStream out) {
        writeTo0(context, out);
        out.println();
    }

    public void writeTo(Object context, Writer out) {
        writeTo0(context, out instanceof PrintWriter ? out : new PrintWriter(out));
        Try.run(() -> out.flush());
    }

    public String execute(Object context) {
        var out = new StringWriter();
        writeTo(context, out);
        return out.toString();
    }

    @Override
    public String toString() {
        return script.toString();
    }

    public static TemplateContext currentContext() {
        return currentContext.get();
    }

    private static final Pattern ESCAPE_PTN = Pattern.compile("[\\\\`\\$]");

    public static String quote(String s) {
        return "`" + Regex.replace(s, ESCAPE_PTN, m -> {
            switch (m.group().charAt(0)) {
            case '\\':
                return "\\\\";
            case '`':
                return "\\`";
            case '$':
                return "\\$";
            default:
                return m.group();
            }
        }) + "`";
    }

    public static class Functions {
        public static Map<String, Object> copy(Map<String, Object> map) {
            return new LinkedHashMap<>(map);
        }

        public static void forEach(Object values, String valuesExpr, String varName, Closure fn) {
            TemplateContext ctx = currentContext();
            ctx.eachStat = new EachStat(ctx.eachStat, values, valuesExpr, varName, nameof(valuesExpr));
            for (var it : ctx.eachStat) {
                fn.execute(ctx, it.value, it);
            }
            ctx.eachStat = ctx.eachStat.parent;
        }

        public static String nameof(String name) {
            TemplateContext ctx = currentContext();
            var p = Pattern.compile("([_a-zA-Z\\$]\\w*)(.*)");
            var m = p.matcher(name);
            if (m.matches()) {
                String rootName = m.group(1);
                for (var i = ctx.eachStat; i != null; i = i.parent) {
                    if (i.varName.equals(rootName)) {
                        return i.baseName + "[" + i.index + "]" + m.group(2);
                    }
                }
            }
            return name;
        }
    }
}
