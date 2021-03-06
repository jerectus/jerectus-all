package jerectus.util.template;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;

import jerectus.io.IO;
import jerectus.util.StringEditor;
import jerectus.util.Sys;
import jerectus.util.Try;
import jerectus.util.logging.Logger;
import jerectus.util.regex.PatternMatcher;
import jerectus.util.regex.Regex;

public class TemplateEngine {
    private static final Logger log = Logger.getLogger(TemplateEngine.class);
    private JexlEngine engine;
    private LRUMap<String, Template> templateCache = new LRUMap<>();
    private Template root;

    public TemplateEngine(Consumer<JexlBuilder> fn) {
        var ns = new HashMap<String, Object>();
        ns.put("tf", Template.Functions.class);
        var jb = new JexlBuilder().namespaces(ns);
        if (fn != null) {
            fn.accept(jb);
        }
        engine = jb.create();
        root = new Template(null, engine.createScript(
                "__for = function(each, fn) { for (var it : each) { fn(it.value, it); } each.end(); };"), null);
    }

    public TemplateEngine(Class<? extends Template.Functions> functionClass) {
        this(b -> b.namespaces().put("tf", functionClass));
    }

    public TemplateEngine() {
        this((Consumer<JexlBuilder>) null);
    }

    public Template getTemplate(Path path, Supplier<Template> fn) {
        return templateCache.computeIfAbsent(path.normalize().toString(), key -> fn.get());
    }

    public Template getTemplate(Path path) {
        return getTemplate(path, () -> createTemplate(path));
    }

    public Template getTemplate(Path path, Function<Path, String> preprocessor) {
        return getTemplate(path, () -> createTemplate(path, preprocessor.apply(path)));
    }

    public Template getTemplate(Path path, String source) {
        return getTemplate(path, () -> createTemplate(path, source));
    }

    public Template createTemplate(String source) {
        return createTemplate(null, source);
    }

    public Template createTemplate(Path path, String source) {
        return compile(path == null ? null : path.normalize(), source);
    }

    public Template createTemplate(Path path) {
        return createTemplate(path, null);
    }

    public void checkScript(String script, String place) {
        try {
            engine.createExpression(script);
        } catch (JexlException e) {
            log(Paths.get(place), script, e);
        }
    }

    static final Pattern TEMPLATE_PTN = Pattern.compile("<(%[=%]?)(.*?)%>|\\$\\{(.*?)\\}", Pattern.DOTALL);

    Template compile(Path path, String s) {
        var sb = new StringEditor();
        try {
            if (s == null) {
                s = Try.get(() -> Files.readString(path, StandardCharsets.UTF_8));
            }
            log.debug("[ ", path, " ]\n", s);
            var blocks = new StringEditor();
            var pm = new PatternMatcher();
            Template parent = path != null && pm.matches(s, "(?s)<%%extends\\s+([^>]+?)%>.*")
                    ? getTemplate(path.resolveSibling(pm.group(1).trim() + IO.getExtention(path)))
                    : null;
            blocks.append("<%(function(this) {%>");
            if (parent != null) {
                blocks.append("<%var _super = tf:copy(this);%>");
            }
            s = Regex.replace(s, "(?s)<%%block\\s+(\\w+).*?%>(.*?)<%%end-block%>", m -> {
                blocks.append("<%this.", m.group(1), " = function() {%>");
                if (parent != null) {
                    blocks.append("<%var super = _super.", m.group(1), ";%>");
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
            int pos = 0;
            var fn = new Object() {
                void printRaw(String s) {
                    if (!Sys.isEmpty(s)) {
                        sb.append("out.print(", quote(s), ");");
                    }
                }

                void print(String s) {
                    if (s != null) {
                        s = s.trim();
                    }
                    if (!Sys.isEmpty(s)) {
                        sb.append("out.print(", s, ");");
                    }
                }

                void text(String s) {
                    if (!Sys.isEmpty(s)) {
                        printRaw(s);
                    }
                }

                void code(String s) {
                    sb.append(s, " ");
                }

                void logic(String s) {
                    var p = new PatternMatcher();
                    if (p.matches(s, "if\\b(.*)")) {
                        sb.append("if(", p.group(1), "){");
                    } else if (p.matches(s, "else-if\\b(.*)")) {
                        sb.append("}else if(", p.group(1), "){");
                    } else if (p.matches(s, "else\\b(.*)")) {
                        sb.append("}else{");
                    } else if (p.matches(s, "end-if\\b(.*)")) {
                        sb.append("/*end-if*/}");
                    } else if (p.matches(s, "for\\s+((\\w+)\\s*(,\\s*(\\w+))?\\s*(:|\\bin\\b))?\\s*([^;]+)(;.*)?")) {
                        var iter = Sys.ifEmpty(p.group(2), "it");
                        var stat = Sys.ifEmpty(p.group(4), iter + "$");
                        var list = p.group(6).trim();
                        var options = p.group(7) == null ? "" : p.group(7).substring(1).trim();
                        sb.append("for(var ", stat, " : tf:each(", list, ", ", quote(list), ", ", quote(iter),
                                ")){var ", iter, "=", stat, ".value;");
                        if (p.matches(options, "delim\\s*=\\s*(\"[^\"]+\"|'[^']+'|`([^`]|\\`)+`)")) {
                            sb.append("if(!", stat, ".first){out.print(", p.group(1), ");}");
                        }
                    } else if (p.matches(s, "end-for\\b(.*)")) {
                        sb.append("/*end-for*/}");
                    } else if (p.matches(s, "super\\s*")) {
                        sb.append("super();");
                    } else {
                        sb.append("<%%", s, "%>");
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
            s = sb.toString();
            log.debug("[ ", path, " ]\n", s);
            return new Template(Sys.ifNull(parent, root), engine.createScript(s), path);
        } catch (JexlException e) {
            log(path, sb, e);
            throw e;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    public static void log(Path path, CharSequence src, JexlException e) {
        var sw = new StringWriter();
        var out = new PrintWriter(sw);
        var info = e.getInfo();
        var l = info.getLine();
        var c = info.getColumn();
        var lines = src.toString().split("(?s)\r\n|\n");
        out.println("[ " + path + " ] (" + l + ":" + c + ")");
        int i = Math.max(l - 5, 1);
        int n = Math.min(l + 5, lines.length);
        for (; i <= n; i++) {
            out.printf("%5d:", i);
            out.println(lines[i - 1]);
            if (i == l) {
                out.print("=====>");
                out.print(Sys.repeat(" ", c - 1, "") + "^ ");
                out.print(e.getMessage().replaceAll("^.*?@\\d+:\\d+ ", ""));
                out.println();
            }
        }
        log.error(sw.toString(), e);
    }

    private static String quote(String s) {
        return Template.quote(s);
    }

    public static String compileFragment(String s, char target, Function<String, String> escape) {
        var p = Pattern.compile("\\$\\{(.*?)\\}");
        var m = p.matcher(s);
        var sb = new StringEditor();
        int pos = 0;
        if (target == 'E') {
            while (m.find()) {
                if (pos < m.start()) {
                    var t = s.substring(pos, m.start());
                    sb.append("+", quote(t));
                }
                sb.append("+", escape.apply(m.group(1)));
                pos = m.end();
            }
            if (pos < s.length()) {
                var t = s.substring(pos);
                sb.append("+", quote(t));
            }
            if (sb.length() > 0 && sb.charAt(0) == '+') {
                sb.delete(0, 1);
            }
            if (sb.length() == 0) {
                sb.append("``");
            }

        } else {
            while (m.find()) {
                if (pos < m.start()) {
                    var t = s.substring(pos, m.start());
                    sb.append(t);
                }
                sb.append("<%=", escape.apply(m.group(1)), "%>");
                pos = m.end();
            }
            if (pos < s.length()) {
                var t = s.substring(pos);
                sb.append(t);
            }
        }
        return sb.toString();
    }

    public static String compileFragment(String s, char target, String escape) {
        return compileFragment(s, target, v -> escape.replace("?", v));
    }

    public static String compileFragment(String s, char target) {
        return compileFragment(s, target, v -> v);
    }
}
