package jerectus.sql.template;

import java.lang.reflect.Array;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.internal.Closure;

import jerectus.io.IO;
import jerectus.sql.internal.Cursor;
import jerectus.sql.parser.SqlParser;
import jerectus.text.Template;
import jerectus.text.TemplateEngine;
import jerectus.util.Sys;
import jerectus.util.logging.Logger;

public class SqlTemplate {
    private static final Logger log = Logger.getLogger(SqlTemplate.class);
    private Supplier<String> sqlSupplier;
    private String sql;
    private Template sqlTemplate;

    public SqlTemplate(String sql) {
        this.sql = sql;
    }

    public SqlTemplate(Supplier<String> fn) {
        sqlSupplier = fn;
    }

    public SqlTemplate(Path sql) {
        sqlSupplier = () -> IO.load(sql);
    }

    private String getSql() {
        if (sql == null && sqlSupplier != null) {
            sql = sqlSupplier.get();
        }
        return sql;
    }

    private String escape(String s) {
        s = s.replace("\\", "\\\\");
        s = s.replace("$", "\\$");
        s = s.replace("<%", "<<%%>%");
        return s;
    }

    private Template getTemplate() {
        if (sqlTemplate != null)
            return sqlTemplate;

        var cursor = new SqlParser().parse(getSql());
        while (cursor.moveNext()) {
            var t = cursor.get();
            if (t.is("comment1?")) {
                var m = new Match(t.getContent().trim());
                if (m.matches("#if\\s+(.+)")) {
                    t.value = "\u007f<%if (" + m.group(1) + ") {%>";
                } else if (m.matches("#elseif\\s+(.+)")) {
                    t.value = "<%} else if (" + m.group(1) + ") {%>";
                } else if (m.matches("#else")) {
                    t.value = "<%} else {%>";
                } else if (m.matches("#end-if")) {
                    t.value = "<%}%>";
                } else if (m.matches("#end-for\\b(\\s*:(.*))?")) {
                    t.value = "<%});%>";
                    if (m.group(2) != null) {
                        var sep = m.group(2).trim();
                        var c = cursor.find(it -> it.is("#for"), -1);
                        if (c.get().is("#for")) {
                            var f = c.get();
                            c = c.next();
                            if (c.get().is("newline")) {
                                c = c.next();
                            }
                            insertBefore(c, String.format("<%%if (!%s.first) {%%>%s <%%}%%>", f.attr("stat"), sep));
                            cursor.moveNext();
                        }
                    }
                } else if (m.matches("#for\\s+([a-zA-Z_\\$]\\w*)\\s*(,\\s*([a-zA-Z_\\$]\\w*))?:\\s*(.*)")) {
                    t.attr("var", m.group(1));
                    t.attr("stat", Sys.ifEmpty(m.group(3), m.group(1) + "$stat"));
                    t.attr("list", m.group(4));
                    t.type = "#for";
                    t.value = String.format("\u007f<%%tf:forEach(this, %s, function (%s, %s) {%%>", t.attr("list"),
                            t.attr("var"), t.attr("stat"));
                } else if (m.matches("##if\\s+(.+)")) {
                    cursor.remove(0);
                    encloseLine(cursor, String.format("<%%if (%s) {%%>", m.group(1)), "<%} else {%>\u007f<%}%>");
                } else if (m.matches(":(%)?(\\w+)(%)?(\\?)?")) {
                    t.value = String.format("<%%= sql.bind(%s, %s, %s) %%>", m.group(2), m.group(1) != null,
                            m.group(3) != null);
                    cursor.remove(1);
                    if (m.group(4) != null) {
                        encloseLine(cursor, String.format("<%%if (sql.isNotEmpty(%s)) {%%>", m.group(2)),
                                "<%} else {%>\u007f<%}%>");
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
        try {
            log.info("sql:", sql);
            sqlTemplate = new TemplateEngine().createTemplate(sql);
        } catch (Exception e) {
            throw Sys.asRuntimeException(e);
        }
        return sqlTemplate;
    }

    public Result process(Object context) {
        Map<String, Object> bindings = new jerectus.sql.template.Param(context);
        var ctx = new SQL();
        bindings.put("sql", ctx);
        String sql = getTemplate().execute(bindings);
        log.info("sql:", sql);
        for (;;) {
            sql = sql.replaceAll("\u007f(,|\\s+|\u007f|and\\b|or\\b)*\u007f", "\u007f");
            sql = sql.replaceAll(" *,\\s*\u007f\\R?", "");
            sql = sql.replaceAll("\u007f\\s*?( +),", "$1 ");
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
        log.info("sql:", sql);
        return new Result(sql, ctx.params);
    }

    private SqlParser.Token newToken(String type, String value, String frontSpace) {
        return new SqlParser.Token(type, value, frontSpace);
    }

    private void insertBefore(Cursor<SqlParser.Token> c, String s) {
        c.insertBefore(newToken("", s, Sys.ifEmpty(c.get().frontSpace, " ")));
        c.get().frontSpace = "";
    }

    private void encloseLine(Cursor<SqlParser.Token> cursor, String head, String tail) {
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
            this.parameters = parameters;
        }
    }

    public static class SQL {
        public List<Object> params = new ArrayList<>();

        private static String repeat(String s, int n, String delim) {
            StringBuilder sql = new StringBuilder();
            for (int i = 0; i < n; i++) {
                if (i > 0) {
                    sql.append(delim);
                }
                sql.append(s);
            }
            return sql.toString();
        }

        public String escape(String s, String escapeChar) {
            if (escapeChar == null) {
                escapeChar = "~";
            }
            return s.replace(escapeChar, escapeChar + escapeChar).replace("%", escapeChar + "%").replace("_",
                    escapeChar + "_");
        }

        public String escape(String s) {
            return escape(s, null);
        }

        public String prefix(String s, String escapeChar) {
            return escape(s, escapeChar) + "%";
        }

        public String prefix(String s) {
            return prefix(s, null);
        }

        public String suffix(String s, String escapeChar) {
            return "%" + escape(s, escapeChar);
        }

        public String suffix(String s) {
            return suffix(s, null);
        }

        public String infix(String s, String escapeChar) {
            return "%" + escape(s, escapeChar) + "%";
        }

        public String infix(String s) {
            return infix(s, null);
        }

        public String bind(Object param, boolean head, boolean tail) {
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
                    return repeat("?", c.size(), ",");
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
                    return repeat("?", n, ",");
                }
                if (head || tail) {
                    param = (head ? "%" : "") + escape(param.toString()) + (tail ? "%" : "");
                }
            }
            params.add(param);
            return "?";
        }

        public void bindTo(PreparedStatement stmt) throws Exception {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
        }

        public boolean isNotEmpty(Object o) {
            return o != null && !"".equals(o) && !" ".equals(o) && size(o) != 0;
        }

        public void forEach(JexlContext ctx, Object c, Closure fn) {
            var stat = new LoopStat();
            stat.size = size(c);
            stat.index = 0;
            stat.count = 1;
            stat.first = true;
            for (var it : Sys.each(c)) {
                stat.last = stat.count == stat.size;
                fn.execute(ctx, it, stat);
                stat.index++;
                stat.count++;
                stat.first = false;
            }
        }

        private static int size(Object o) {
            if (o == null) {
                return 0;
            } else if (o instanceof Collection) {
                Collection<Object> c = Sys.cast(o);
                return c.size();
            } else if (o.getClass().isArray()) {
                return Array.getLength(o);
            } else {
                return 1;
            }
        }

        public static class LoopStat {
            public int size;
            public int index;
            public int count;
            public boolean first;
            public boolean last;
        }
    }
}

class Match {
    private String s;
    private Matcher m;

    public Match(String s) {
        this.s = s;
    }

    public boolean matches(Pattern pattern) {
        m = pattern.matcher(s);
        return m.matches();
    }

    public boolean matches(String pattern) {
        return matches(Pattern.compile(pattern));
    }

    public Matcher matcher() {
        return m;
    }

    public String group(int index) {
        return m.group(index);
    }
}