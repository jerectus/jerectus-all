package jerectus.sql.template;

import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jerectus.io.IO;
import jerectus.sql.parser.Cursor;
import jerectus.sql.parser.SqlToken;
import jerectus.sql.parser.SqlTokenizer;
import jerectus.util.template.Template;
import jerectus.util.template.TemplateContext;
import jerectus.util.template.TemplateEngine;
import jerectus.util.Sys;
import jerectus.util.logging.Logger;
import jerectus.util.regex.Regex;

public class SqlTemplate {
    private static final Logger log = Logger.getLogger(SqlTemplate.class);
    private static final TemplateEngine engine = new TemplateEngine(b -> {
        b.namespaces().put("tf", Functions.class);
        b.strict(false);
    });
    private Supplier<Template> supplier;
    private Template sqlTemplate;

    public SqlTemplate(String sql) {
        supplier = () -> engine.createTemplate(preprocess(sql));
    }

    public SqlTemplate(Supplier<String> fn) {
        supplier = () -> engine.createTemplate(preprocess(fn.get()));
    }

    public SqlTemplate(Path path) {
        supplier = () -> engine.getTemplate(path, preprocess(IO.load(path)));
    }

    private static String preprocess(String sql) {
        var cursor = Cursor.of(new SqlTokenizer().parse(sql), -1);
        while (cursor.moveNext()) {
            var t = cursor.get();
            if (t.is("comment1?")) {
                var m = Regex.test(t.getContent().trim());
                if (m.matches("#(if|for|else(-if)?|end-(if|for))\\b.*")) {
                    t.value = (m.group(1).matches("if|for") ? "\u007f" : "") + "<%%" + m.group().substring(1) + "%>";
                    t.frontSpace = "";
                    if (cursor.prev().get().is("newline")) {
                        cursor.prev().get().value = "";
                    }
                } else if (m.matches("##if\\s+(.+)")) {
                    cursor.remove(0);
                    encloseLine(cursor, String.format("<%%if(%s){%%>", m.group(1)), "<%}else{%>\u007f<%}%>");
                } else if (m.matches(":(%)?(\\w+)(%)?(\\?)?")) {
                    t.value = String.format("<%%=tf:bind(%s, %s, %s)%%>", m.group(2), m.group(1) != null,
                            m.group(3) != null);
                    cursor.remove(1);
                    if (m.group(4) != null) {
                        encloseLine(cursor, String.format("<%%if(tf:isNotEmpty(%s)){%%>", m.group(2)),
                                "<%}else{%>\u007f<%}%>");
                    }
                } else if (m.matches("(@.*)")) {
                    t.value = String.format("' ' \"%s\",", m.group(1).trim());
                } else {
                    t.value = escape(t.value);
                }
            } else if (t.is("string")) {
                t.value = escape(t.value);
            }
        }
        sql = cursor.list().stream().map(t -> t.toString()).collect(Collectors.joining());
        return sql;
    }

    private static String escape(String s) {
        s = s.replace("\\", "\\\\");
        s = s.replace("$", "\\$");
        s = s.replace("<%", "<<%%>%");
        return s;
    }

    private Template getTemplate() {
        if (sqlTemplate == null) {
            sqlTemplate = supplier.get();
        }
        return sqlTemplate;
    }

    public Result process(Object vars) {
        var ctx = new TemplateContext(vars);
        String sql = getTemplate().execute(ctx);
        sql = adjust(sql);
        return new Result(sql, Sys.cast(ctx.get("__params")));
    }

    private static String adjust(String sql) {
        for (;;) {
            sql = sql.replaceAll("\u007f(,|\\s+|\u007f|and\\b|or\\b)*\u007f", "\u007f");
            sql = sql.replaceAll(" *,\\s*\u007f\\R?", "");
            // sql = sql.replaceAll("\u007f\\s*?( +),", "$1 ");
            sql = sql.replaceAll("\u007f\\s*,\\s*", "");
            // sql = sql.replaceAll("\u007f[\\s\u007f,]+,", ",");
            // sql = sql.replaceAll("(\\(\\s*)\u007f(\\s*)(,|and\\b|or\\b)\\s*", "$1$2");
            // sql = sql.replaceAll("\u007f[\\s\u007f]*\u007f", "\u007f");
            // sql = sql.replaceAll(",\\s*\u007f\\s*,", ",");
            // sql = sql.replaceAll(" *,\\s*\u007f", "");
            // sql = sql.replaceAll("\u007f(\\s*),( *)", "$1 $2");
            sql = sql.replaceAll("(?i) *\\band\\s*\u007f", "");
            sql = sql.replaceAll("(?i) *\u007f\\s*and\\b", "");
            sql = sql.replaceAll("(?i) *\\bor\\s*\u007f", "");
            sql = sql.replaceAll("(?i) *\u007f\\s*or\\b", "");
            // sql = sql.replaceAll("\\n[ \t\u007f]+\\n", "\u007f\n");
            Matcher m = Pattern.compile("\\(\\s*\u007f\\s*\\)").matcher(sql);
            if (!m.find())
                break;
            sql = sql.replaceAll("\\(\\s*\u007f\\s*\\)", "\u007f");
        }
        var adjWords = "on|where|group\\s+by|having|order\\s+by|union";
        var adjPtn = Pattern.compile(String.format("(?is)\\b(%1$s)\\s*\u007f\\s*(%1$s)\\b", adjWords));
        for (;;) {
            var m = adjPtn.matcher(sql);
            if (!m.find())
                break;
            sql = sql.substring(0, m.start()) + m.group(2) + sql.substring(m.end());
        }
        sql = sql.replaceAll("\\b(" + adjWords + ")\\b[\\s\u007f]*$", "");
        sql = sql.replaceAll(" *\u007f *", "");
        return sql;
    }

    @Override
    public String toString() {
        return sqlTemplate.toString();
    }

    private static SqlToken newToken(String type, String value, String frontSpace) {
        return new SqlToken(type, value, frontSpace);
    }

    private static void insertBefore(Cursor<SqlToken> c, String s) {
        c.insertBefore(newToken("", s, Sys.ifEmpty(c.get().frontSpace, " ")));
        c.get().frontSpace = "";
    }

    private static void encloseLine(Cursor<SqlToken> cursor, String head, String tail) {
        var begin = cursor.find(it -> it.is("newline"), -1).next();
        if (begin.get().matches("(?i)where|and|or|on|when|having|,|$")) {
            begin.moveNext();
        }
        insertBefore(begin, head);
        var end = cursor.find(it -> it.is("newline"), 1);
        end.insertBefore(newToken("", tail, ""));
        cursor.moveNext();
    }

    public static class Result {
        public String sql;
        public List<Object> parameters;

        public Result(String sql, List<Object> parameters) {
            this.sql = sql;
            this.parameters = Sys.ifNull(parameters, Collections.emptyList());
            log.debug("result=\n", this);
        }

        @Override
        public String toString() {
            return sql + parameters;
        }
    }

    public static class Functions extends Template.Functions {
        public static String escape(String s, String escapeChar) {
            if (escapeChar == null) {
                escapeChar = "~";
            }
            return s.replace(escapeChar, escapeChar + escapeChar).replace("%", escapeChar + "%").replace("_",
                    escapeChar + "_");
        }

        public static String escape(String s) {
            return escape(s, null);
        }

        public static String prefix(String s, String escapeChar) {
            return escape(s, escapeChar) + "%";
        }

        public static String prefix(String s) {
            return prefix(s, null);
        }

        public static String suffix(String s, String escapeChar) {
            return "%" + escape(s, escapeChar);
        }

        public static String suffix(String s) {
            return suffix(s, null);
        }

        public static String infix(String s, String escapeChar) {
            return "%" + escape(s, escapeChar) + "%";
        }

        public static String infix(String s) {
            return infix(s, null);
        }

        public static boolean isNotEmpty(Object o) {
            return o != null && !"".equals(o) && !" ".equals(o) && Sys.size(o) != 0;
        }

        public static String bind(Object param, boolean head, boolean tail) {
            var ctx = Template.currentContext();
            @SuppressWarnings("unchecked")
            var params = (List<Object>) ctx.computeIfAbsent("__params", key -> new ArrayList<>());
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
                    param = (head ? "%" : "") + escape(param.toString()) + (tail ? "%" : "");
                }
            }
            params.add(param);
            return "?";
        }
    }
}
