package jerectus.html.template;

import java.io.PrintStream;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import jerectus.html.HtmlVisitor;
import jerectus.util.StringEditor;
import jerectus.util.Sys;
import jerectus.util.logging.Logger;
import jerectus.util.regex.PatternMatcher;
import jerectus.util.template.Template;
import jerectus.util.template.TemplateEngine;

public class HtmlTemplate {
    private static final Logger log = Logger.getLogger(HtmlTemplate.class);
    private static final TemplateEngine engine = new TemplateEngine(Functions.class);
    private Template tmpl;

    public HtmlTemplate(Path path) {
        tmpl = engine.getTemplate(path, HtmlTemplate::preprocessor);
    }

    public void render(Writer out, Object self) {
        tmpl.execute(self, out);
    }

    public void render(PrintStream out, Object self) {
        tmpl.execute(self, out);
    }

    @Override
    public String toString() {
        return tmpl.toString();
    }

    public static String preprocessor(Path path) {
        try {
            var doc = Jsoup.parse(path.toFile(), "UTF-8", "");
            doc.outputSettings().prettyPrint(false);
            Sys.last(doc.selectFirst("body").childNodes(), it -> it.remove());
            doc.select("script").forEach(elem -> {
                if (elem.attr("type").equals("text/jexl")) {
                    elem.replaceWith(new Comment("%%" + elem.html()));
                }
            });
            doc.select("[v:bind]").forEach(elem -> {
                var bind = elem.attr("v:bind");
                elem.removeAttr("v:bind");
                applyBind(elem, bind);
            });
            doc.select("[name]").forEach(elem -> {
                var name = elem.attr("name");
                if (name.startsWith(":")) {
                    elem.removeAttr("name");
                    elem.attr("v:model", name.substring(1));
                }
            });
            doc.select("[v:model]").forEach(elem -> {
                String model = elem.attr("v:model");
                if (!elem.hasAttr("name")) {
                    elem.attr("name", model);
                }
                if (elem.is("select")) {
                    elem.removeAttr("v:model");
                    elem.select("option").forEach(opt -> {
                        opt.attr("v:model", model);
                        opt.attr("inner-text", opt.text());
                        opt.text("");
                    });
                } else if (elem.is("textarea")) {
                    elem.text("");
                }
            });
            doc.traverse(new HtmlVisitor() {
                @Override
                public void visit(Comment elem) {
                    Node prev = elem.previousSibling();
                    if (prev instanceof TextNode && ((TextNode) prev).text().matches("\\s+")) {
                        prev.remove();
                    }
                }
            });
            var sb = new StringEditor();
            doc.traverse(new HtmlVisitor() {
                public void visit(Element elem) {
                    if (elem.is("html")) {
                        sb.append("\n");
                    }
                    element(elem);
                    if (elem.is("html")) {
                        sb.append("\n");
                    }
                }

                public void leave(Element elem) {
                    if (elem.is("html")) {
                        sb.append("\n");
                    }
                    sb.append("</");
                    sb.append(elem.tagName());
                    sb.append(">");
                }

                public void visit(TextNode textNode) {
                    sb.append(TemplateEngine.compileFragment(textNode.outerHtml(), 'T', "tf:escape(?, '')"));
                }

                public void visit(Comment comment) {
                    if (comment.getData().startsWith("%")) {
                        sb.append(makeScript(comment.getData().substring(1)));
                    } else {
                        sb.append(comment.toString());
                    }
                }

                @Override
                public void visit(Node node) {
                    sb.append(node.outerHtml());
                }

                private void element(Element elem) {
                    if (elem.hasAttr("v:model") || elem.hasAttr("v:attr")) {
                        String model = Sys.ifEmpty(elem.attr("v:model"), "null");
                        elem.removeAttr("v:model");
                        elem.removeAttr("v:attr");
                        sb.append("<%=tf:tag(`", elem.tagName(), "`, {");
                        String[] sep = { "" };
                        elem.attributes().forEach(attr -> {
                            sb.append(sep[0]);
                            sep[0] = ", ";
                            if (attr.getKey().startsWith("v:attr:")) {
                                sb.append("`", attr.getKey().substring(7), "`:", attr.getValue());
                            } else {
                                sb.append("`", attr.getKey(), "`:");
                                sb.append(attr.getValue() == null ? "true"
                                        : TemplateEngine.compileFragment(attr.getValue(), 'E'));
                            }
                        });
                        sb.append("}, ", model, ")%>");
                    } else {
                        sb.append("<");
                        sb.append(elem.tagName());
                        elem.attributes().forEach(attr -> {
                            sb.append(" ");
                            sb.append(attr.getKey());
                            if (attr.toString().contains("=")) {
                                sb.append("=\"");
                                sb.append(TemplateEngine.compileFragment(attr.getValue(), 'T', "tf:escape(?, '@')"));
                                sb.append("\"");
                            }
                        });
                        sb.append(">");
                    }
                }
            });
            return sb.toString();
        } catch (Exception e) {
            throw Sys.asRuntimeException(e);
        }
    }

    private static void applyBind(Element elem, String bind) {
        Pattern ptn = Pattern.compile("([^:;]+)(:([^;]+))?(;|$)");
        Matcher m = ptn.matcher(bind);
        while (m.find()) {
            var type = Sys.trim(m.group(1));
            var args = Sys.trim(m.group(3));
            switch (type) {
            case "model":
                elem.attr("v:model", args);
                break;
            case "if":
            case "for": {
                Node node = elem.previousSibling();
                if (!(node instanceof TextNode) || !((TextNode) node).text().matches("\\s*")) {
                    node = elem;
                }
                node.before(new Comment("%" + type + " " + args));
                elem.after(new Comment("%end-" + type));
                break;
            }
            default:
                if (type.startsWith("@")) {
                    elem.attr("v:attr", true);
                    elem.attr("v:attr:" + type.substring(1), args);
                } else {
                    log.warn("unknown directive: ", type, " in ", getElementPath(elem), "[", bind, "]");
                }
                break;
            }
        }
    }

    private static String getElementPath(Element elem) {
        return elem.hasParent() ? getElementPath(elem.parent()) + "/" + elem.tagName() : "";
    }

    private static String makeScript(String code) {
        var m = new PatternMatcher();
        if (m.matches(code, "==(.*)")) {
            return "<%=" + m.group(1) + "%>";
        } else if (m.matches(code, "=(.*)")) {
            return "<%=tf:escape(" + m.group(1).trim() + ", '')%>";
        } else if (code.startsWith("%")) {
            return "<%" + code.substring(1).trim() + "%>";
        } else {
            return "<%%" + code + "%>";
        }
    }

    public static class Functions extends Template.Functions {
        public static String escape(Object v, String ctx) {
            if (v == null)
                return "";
            String s = v.toString();
            switch (ctx) {
            case "@":
                s = s.replace("&", "&amp;");
                s = s.replace("\"", "&quot;");
                break;
            default:
                s = s.replace("&", "&amp;");
                s = s.replace("<", "&lt;");
                s = s.replace(">", "&gt;");
                break;
            }
            return s;
        }

        public static String unescape(String s) {
            s = s.replace("&lt;", "<");
            s = s.replace("&gt;", ">");
            s = s.replace("&quot;", "\"");
            s = s.replace("&amp;", "&");
            return s;
        }

        public static String tag(String tagName, Map<String, Object> attributes, Object value) {
            Object innerText = null;
            switch (tagName) {
            case "input":
                switch (Sys.toString(attributes.getOrDefault("type", "text"))) {
                case "text":
                case "password":
                case "hidden":
                    attributes.put("value", value);
                    break;
                case "checkbox":
                case "radio":
                    attributes.put("checked", matches(attributes.get("value"), value));
                    break;
                }
                break;
            case "select":
                break;
            case "option":
                innerText = attributes.get("inner-text");
                attributes.remove("inner-text");
                attributes.put("selected", matches(attributes.getOrDefault("value", innerText), value));
                break;
            case "textarea":
                innerText = value;
                break;
            }
            var sb = new StringEditor();
            sb.append("<", tagName);
            attributes.forEach((attrName, attrVal) -> {
                if (!Sys.eq(attrVal, false)) {
                    sb.append(" ", attrName);
                    if (!Sys.eq(attrVal, true)) {
                        sb.append("=\"");
                        if (attrName.equals("name") && attrVal != null) {
                            attrVal = nameof(attrVal.toString());
                        }
                        sb.append(escape(attrVal, "@"));
                        sb.append("\"");
                    }
                }
            });
            sb.append(">");
            if (innerText != null) {
                sb.append(innerText);
            }
            return sb.toString();
        }

        private static boolean matches(Object a, Object b) {
            for (Object v : Sys.each(b)) {
                if (Sys.eq(Sys.toString(a, ""), Sys.toString(v, ""))) {
                    return true;
                }
            }
            return false;
        }
    }
}
