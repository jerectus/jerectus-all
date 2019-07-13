package jerectus.util.template;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.internal.Closure;

import jerectus.util.Try;
import jerectus.util.regex.Regex;

public class Template {
    private static ThreadLocal<TemplateContext> currentContext = new ThreadLocal<>();
    private Template parent;
    private JexlScript script;
    private Path path;

    public Template(Template parent, JexlScript script, Path path) {
        this.parent = parent;
        this.script = script;
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public Object getTemplateObject(JexlContext ctx) {
        if (parent != null) {
            parent.getTemplateObject(ctx);
        }
        return script.execute(ctx);
    }

    private void writeTo(Object context, Object out) {
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
        } catch (JexlException e) {
            TemplateEngine.log(path, script.toString(), e);
            throw e;
        } finally {
            currentContext.set(null);
        }
    }

    public void execute(Object context, PrintStream out) {
        writeTo(context, out);
        out.println();
    }

    public void execute(Object context, Writer out) {
        writeTo(context, new PrintWriter(out) {
            @Override
            public void print(char[] s) {
                if (s != null) {
                    super.print(s);
                }
            }
        });
        Try.run(() -> out.flush());
    }

    public String execute(Object context) {
        var out = new StringWriter();
        execute(context, out);
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
        @SuppressWarnings("rawtypes")
        public static boolean isa(Object x, Object type) {
            if (x == null || type == null) {
                return false;
            }
            Class<?> t1 = x instanceof Class ? (Class) x : x.getClass();
            if (type instanceof String) {
                var className = (String) type;
                if (className.indexOf(".") == -1) {
                    className = "java.lang." + className;
                    try {
                        type = Class.forName(className);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            Class<?> t2 = (Class) type;
            return t2.isAssignableFrom(t1);
        }

        public static boolean isNumber(Object x) {
            return x instanceof Number;
        }

        public static int toInt(Object x, int other) {
            if (x instanceof Number) {
                return ((Number) x).intValue();
            } else {
                try {
                    return Integer.parseInt(x.toString());
                } catch (Exception e) {
                    return other;
                }
            }
        }

        public static Map<String, Object> copy(Map<String, Object> map) {
            return new LinkedHashMap<>(map);
        }

        public static EachStat each(Object values, String valuesExpr, String varName) {
            return new EachStat(currentContext(), values, valuesExpr, varName);
        }

        public static void forEach(Object values, String valuesExpr, String varName, Closure fn) {
            var ctx = currentContext();
            var eachStat = new EachStat(ctx, values, valuesExpr, varName);
            try {
                for (var it : eachStat) {
                    fn.execute(ctx, it.value, it);
                }
            } finally {
                eachStat.end();
            }
        }

        public static String nameof(String name) {
            return currentContext().nameof(name);
        }
    }
}
