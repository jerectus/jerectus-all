package jerectus.text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;

import jerectus.util.PatternMatcher;
import jerectus.util.Sys;
import jerectus.util.Try;

public class TemplateEngine {
    private static final JexlEngine engine;
    static {
        var ns = new HashMap<String, Object>();
        ns.put("tf", TemplateFunctions.class);
        engine = new JexlBuilder().namespaces(ns).create();
    }

    private Function<String, String> preprocessor = s -> s;
    private LRUMap<String, Template> scriptCache = new LRUMap<>();

    public TemplateEngine preprocessor(Function<String, String> preprocessor) {
        this.preprocessor = preprocessor;
        return this;
    }

    public Template getTemplate(Path path) {
        return scriptCache.computeIfAbsent(path.toString(), key -> createTemplate(path));
    }

    public Template createTemplate(Path path, String source) {
        return compile(path, source);
    }

    public Template createTemplate(String source) {
        return createTemplate(null, source);
    }

    public Template createTemplate(Path path) {
        return createTemplate(path, null);
    }

    static final Pattern ESCAPE_PTN = Pattern.compile("[\\\\`\\$]");

    String escape(String s) {
        return "`" + Sys.replace(s, ESCAPE_PTN, m -> {
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

    static final Pattern TEMPLATE_PTN = Pattern.compile("<(%[=%]?)(.*?)%>|\\$\\{(.*?)\\}", Pattern.DOTALL);

    Template compile(Path path, String s) {
        if (s == null) {
            s = Try.get(() -> Files.readString(path, StandardCharsets.UTF_8));
        }
        s = preprocessor.apply(s);
        // System.out.println(s);
        var blocks = new StringBuilder();
        var pm = new PatternMatcher();
        Template parent = path != null && pm.matches(s, "(?s)<%%extends\\s+(\\w+).*?%>")
                ? compile(path.resolveSibling(pm.group(1) + ".html"), null)
                : null;
        blocks.append("<%(function(this) {%>");
        if (parent != null) {
            blocks.append("<%var _super = tf:copy(this);%>");
        }
        s = Sys.replace(s, "(?s)<%%block\\s+(\\w+).*?%>(.*?)<%%end-block%>", m -> {
            blocks.append("<%this.").append(m.group(1)).append(" = function() {%>");
            if (parent != null) {
                blocks.append("<%var super = _super.").append(m.group(1)).append(";%>");
            }
            blocks.append(m.group(2));
            blocks.append("<%};%>");
            return "<%this." + m.group(1) + "();%>";
        });
        if (parent == null) {
            blocks.append("<%this.__render__ = function() {%>");
            blocks.append(s);
            blocks.append("<%};%>");
        }
        blocks.append("<%})(this)%>");
        s = blocks.toString();
        // System.out.println(s);
        var m = TEMPLATE_PTN.matcher(s);
        var sb = new StringBuilder();
        int pos = 0;
        var fn = new Object() {
            void append(Object... params) {
                for (var v : params) {
                    sb.append(v);
                }
            }

            void printRaw(String s) {
                if (!Sys.isEmpty(s)) {
                    append("out.print(", escape(s), ");");
                }
            }

            void print(String s) {
                if (s != null) {
                    s = s.trim();
                }
                if (!Sys.isEmpty(s)) {
                    append("out.print(tf:escape(", s, "));");
                }
            }

            void text(String s) {
                if (!Sys.isEmpty(s)) {
                    printRaw(s);
                }
            }

            void code(String s) {
                append(s);
            }

            void logic(String s) {
                var p = new PatternMatcher();
                if (p.matches(s, "if\\b(.*)")) {
                    append("if (", m.group(1), ") {");
                } else if (p.matches(s, "else-if\\b(.*)")) {
                    append("} else if (", m.group(1), ") {");
                } else if (p.matches(s, "else\\b(.*)")) {
                    append("} else {");
                } else if (p.matches(s, "end-if\\b(.*)")) {
                    append("}");
                } else if (p.matches(s, "each\\b\\s*((\\w+)\\s*(,\\s*(\\w+))?\\s*:)?\\s*(.+)")) {
                    var iter = Sys.ifEmpty(p.group(2), "it");
                    var stat = Sys.ifEmpty(p.group(4), iter + "$");
                    var list = p.group(5);
                    append("for (var ", stat, " : tf:each(", list, ")) { var ", iter, " = ", stat, ".value;");
                } else if (p.matches(s, "end-each\\b(.*)")) {
                    append("}");
                } else if (p.matches(s, "super\\s*")) {
                    append("super();");
                }
            }
        };
        while (m.find()) {
            fn.text(s.substring(pos, m.start()));
            if (m.group(3) != null) {
                fn.print(m.group(3));
            } else if (m.group(1).equals("%")) {
                fn.code(m.group(2));
            } else if (m.group(1).equals("%=")) {
                fn.print(m.group(2));
            } else if (m.group(1).equals("%%")) {
                fn.logic(m.group(2));
            }
            pos = m.end();
        }
        fn.text(s.substring(pos));
        System.out.println(sb);
        return new Template(parent, engine.createScript(sb.toString()));
    }

    public static class ForEachStat {
        Object value;
        int index = -1;

        public Object getValue() {
            return value;
        }

        public int getIndex() {
            return index;
        }

        public int getCount() {
            return index + 1;
        }

        public boolean isFirst() {
            return index == 0;
        }

        public boolean isOdd() {
            return index % 2 == 0;
        }

        public boolean isEven() {
            return index % 2 == 1;
        }
    }

    public static class TemplateFunctions {
        public static String escape(Object s) {
            return s == null ? null : Sys.replace(s.toString(), "[<>&]", m -> {
                switch (m.group().charAt(0)) {
                case '<':
                    return "&gt;";
                case '>':
                    return "&lt;";
                case '&':
                    return "&amp;";
                }
                return m.group();
            });
        }

        public static Iterable<ForEachStat> each(Object values) {
            var it = Sys.iterator(values);
            var stat = new ForEachStat();
            return new Iterable<ForEachStat>() {
                @Override
                public Iterator<ForEachStat> iterator() {
                    return new Iterator<ForEachStat>() {
                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public ForEachStat next() {
                            stat.value = it.next();
                            stat.index++;
                            return stat;
                        }
                    };
                }
            };
        }

        public static Map<String, Object> copy(Map<String, Object> map) {
            return new LinkedHashMap<>(map);
        }
    }
}