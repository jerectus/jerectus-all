package jerectus.util.template;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;

import jerectus.io.IO;
import jerectus.text.StringEditor;
import jerectus.util.Sys;
import jerectus.util.Try;
import jerectus.util.regex.PatternMatcher;
import jerectus.util.regex.Regex;

public class TemplateEngine {
    private JexlEngine engine;
    private Function<String, String> preprocessor = s -> s;
    private LRUMap<String, Template> templateCache = new LRUMap<>();

    public TemplateEngine(Consumer<JexlBuilder> fn) {
        var ns = new HashMap<String, Object>();
        ns.put("tf", Template.Functions.class);
        var jb = new JexlBuilder().namespaces(ns);
        fn.accept(jb);
        engine = jb.create();
    }

    public TemplateEngine(Class<? extends Template.Functions> functionClass) {
        this(b -> b.namespaces().put("tf", functionClass));
    }

    public TemplateEngine() {
        this(b -> {
        });
    }

    public TemplateEngine preprocessor(Function<String, String> preprocessor) {
        this.preprocessor = preprocessor;
        return this;
    }

    public Template getTemplate(Path path) {
        return templateCache.computeIfAbsent(path.toString(), key -> createTemplate(path));
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

    static final Pattern TEMPLATE_PTN = Pattern.compile("<(%[=%]?)(.*?)%>|\\$\\{(.*?)\\}", Pattern.DOTALL);

    Template compile(Path path, String s) {
        if (s == null) {
            s = Try.get(() -> Files.readString(path, StandardCharsets.UTF_8));
        }
        s = preprocessor.apply(s);
        var blocks = new StringEditor();
        var pm = new PatternMatcher();
        Template parent = path != null && pm.matches(s, "(?s)<%%extends\\s+(\\w+).*?%>.*")
                ? getTemplate(path.resolveSibling(pm.group(1) + IO.getExtention(path)))
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
        var sb = new StringEditor();
        int pos = 0;
        var fn = new Object() {
            void printRaw(String s) {
                if (!Sys.isEmpty(s)) {
                    sb.append("out.print(", Template.quote(s), ");");
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
                sb.append(s);
            }

            void logic(String s) {
                var p = new PatternMatcher();
                if (p.matches(s, "if\\b(.*)")) {
                    sb.append("if(", m.group(1), "){");
                } else if (p.matches(s, "else-if\\b(.*)")) {
                    sb.append("}else if(", m.group(1), "){");
                } else if (p.matches(s, "else\\b(.*)")) {
                    sb.append("}else{");
                } else if (p.matches(s, "end-if\\b(.*)")) {
                    sb.append("}");
                } else if (p.matches(s, "for\\s+((\\w+)\\s*(,\\s*(\\w+))?\\s*(:|\\bin\\b))?\\s*([^;]+)(;.*)?")) {
                    var iter = Sys.ifEmpty(p.group(2), "it");
                    var stat = Sys.ifEmpty(p.group(4), iter + "$");
                    var list = p.group(6).trim();
                    var options = p.group(7) == null ? "" : p.group(7).substring(1).trim();
                    sb.append("tf:forEach(", list, ", `", list, "`, `", iter, "`, function(", iter, ", ", stat, "){");
                    if (p.matches(options, "delim\\s*=\\s*(\"[^\"]+\"|'[^']+'|`([^`]|\\`)+`)")) {
                        sb.append("if(!", stat, ".first){out.print(", p.group(1), ");}");
                    }
                } else if (p.matches(s, "end-for\\b(.*)")) {
                    sb.append("});");
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
        try {
            return new Template(parent, engine.createScript(s));
        } catch (JexlException.Parsing e) {
            var info = e.getInfo();
            var l = info.getLine();
            var c = info.getColumn();
            var lines = s.split("(?s)\r\n|\n");
            System.out.println(lines[l - 1]);
            System.out.println(Sys.repeat(" ", c - 1, "") + "^ " + e.getMessage().replaceAll("^.*?@\\d+:\\d+ ", ""));
            throw e;
        }
    }
}