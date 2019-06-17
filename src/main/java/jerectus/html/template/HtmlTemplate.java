package jerectus.html.template;

import java.io.Writer;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

import jerectus.text.PatternMatcher;
import jerectus.text.StringEditor;
import jerectus.text.Template;
import jerectus.text.TemplateContext;
import jerectus.text.TemplateEngine;
import jerectus.text.TemplateEngine.TemplateFunctions;
import jerectus.util.Sys;

class HtmlVisitor implements NodeVisitor {
    @Override
    public void head(Node node, int depth) {
        if (node instanceof Document) {
            visit((Document) node);
        } else if (node instanceof Element) {
            visit((Element) node);
        } else if (node instanceof TextNode) {
            visit((TextNode) node);
        } else if (node instanceof Comment) {
            visit((Comment) node);
        } else {
            visit(node);
        }
    }

    @Override
    public void tail(Node node, int depth) {
        if (node instanceof Element) {
            var elem = (Element) node;
            leave(elem, hasEndTag(elem));
        }
    }

    public void visit(Document elem) {
    }

    public void leave(Document elem) {
    }

    public void visit(Element elem) {
    }

    public void leave(Element elem, boolean hasEndTag) {
        if (hasEndTag(elem)) {
            leave(elem);
        }
    }

    public void leave(Element elem) {
    }

    public void visit(TextNode elem) {
    }

    public void visit(Comment elem) {
    }

    public void visit(Node node) {
    }

    private static boolean hasEndTag(Element elem) {
        return !(elem instanceof Document)
                && !elem.tagName().matches("br|img|hr|meta|input|embed|area|base|col|keygen|link|param|source");
    }
}

public class HtmlTemplate {
    private static final TemplateEngine engine = new TemplateEngine(Statics.class);
    private Template tmpl;

    public HtmlTemplate(Path path) {
        try {
            var doc = Jsoup.parse(path.toFile(), "UTF-8", "");
            doc.outputSettings().prettyPrint(false);
            var html = doc.selectFirst("html");
            var body = html.selectFirst("body");
            for (int i = body.childNodeSize() - 1; i >= 0; i--) {
                var t = body.childNode(i);
                if (t instanceof TextNode) {
                    t.remove();
                    html.insertChildren(0, t);
                    break;
                }
            }
            html.appendText("\n");
            doc.select("script").forEach(elem -> {
                if (elem.attr("type").equals("text/groovy")) {
                    elem.replaceWith(new Comment("%" + elem.html()));
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
                    elem.select("option").forEach(opt -> {
                        opt.attr("v:model", model);
                        opt.attr("innerText", opt.text());
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
                    if (elem.hasAttr("v:model") || elem.hasAttr("v:attr")) {
                        String model = Sys.ifEmpty(elem.attr("v:model"), "null");
                        elem.removeAttr("v:model");
                        elem.removeAttr("v:attr");
                        sb.append("<%=tf:tag(__ctx, `", elem.tagName(), "`, {");
                        String[] sep = { "" };
                        elem.attributes().forEach(attr -> {
                            sb.append(sep[0]);
                            sep[0] = ", ";
                            if (attr.getKey().startsWith("v:attr:")) {
                                sb.append("`", attr.getKey().substring(7), "`");
                                sb.append(":");
                                sb.append(attr.getValue());
                            } else {
                                sb.append("`", attr.getKey(), "`");
                                sb.append(":");
                                sb.append(attr.getValue() == null ? "true" : TemplateEngine.escape(attr.getValue()));
                            }
                        });
                        sb.append("}, ", model, ")%>");
                    } else {
                        sb.append("<");
                        sb.append(elem.tagName());
                        elem.attributes().forEach(attr -> {
                            sb.append(" ");
                            sb.append(attr.getKey());
                            if (attr.getValue() != null) {
                                sb.append("=\"");
                                sb.append(expand(attr.getValue(), "@"));
                                sb.append("\"");
                            }
                        });
                        sb.append(">");
                    }
                }

                public void leave(Element elem) {
                    sb.append("</");
                    sb.append(elem.tagName());
                    sb.append(">");
                }

                public void visit(TextNode textNode) {
                    sb.append(expand(textNode.outerHtml(), ""));
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
            });
            var src = sb.toString();
            System.out.println(src);
            tmpl = engine.createTemplate(src);
        } catch (Exception e) {
            throw Sys.asRuntimeException(e);
        }
    }

    public void render(Writer out, Object self) {
        tmpl.writeTo(self, out);
    }

    private String expand(String s, String ctx) {
        final Pattern ptn = Pattern.compile("(?s)\\\\|\\$(\\{(.*?)\\})?");
        Function<String, String> fn = ctx.equals("@") ? t -> Statics.decode(t) : t -> t;
        if (ctx.equals("@")) {
            s = Statics.encode(s, "@");
        }
        return Sys.replace(s, ptn, m -> {
            switch (m.group()) {
            case "\\":
                return "\\\\";
            case "$":
            case "${}":
                return "\\$";
            default:
                return makeScript("=" + ctx + fn.apply(m.group(2)));
            }
        });
    }

    private void applyBind(Element elem, String bind) {
        // Pattern ptn = Pattern.compile("\"[]\"");
        Stream.of(bind.split(";")).forEach(it -> {
            PatternMatcher m = new PatternMatcher();
            if (m.matches(it, "\\s*model\\s*:\\s*(.*)\\s*")) {
                elem.attr("v:model", m.group(1));
            } else if (m.matches(it, "\\s*@(\\w+)\\s*:\\s*(.*)\\s*")) {
                elem.attr("v:attr", true);
                elem.attr("v:attr:" + m.group(1), m.group(2));
            } else if (m.matches(it, "\\s*(if|for)\\s*:\\s*(.*)\\s*")) {
                Node node = elem.previousSibling();
                if (!(node instanceof TextNode) || !((TextNode) node).text().matches("\\s*")) {
                    node = elem;
                }
                node.before(new Comment("%%" + m.group(1) + " " + m.group(2)));
                elem.after(new Comment("%%end-" + m.group(1)));
            }
        });
    }

    private String makeScript(String code) {
        var m = new PatternMatcher();
        if (m.matches(code, "==(.*)")) {
            return "<%=" + m.group(1) + "%>";
        } else if (m.matches(code, "=@(.*)")) {
            return "<%=tf:encode(" + m.group(1).trim() + ", '@')%>";
        } else if (m.matches(code, "=(.*)")) {
            return "<%=tf:encode(" + m.group(1).trim() + ", '')%>";
        }
        return "<%" + code + "%>";
    }

    public static class Statics extends TemplateFunctions {
        public static String encode(Object v, String ctx) {
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

        public static String decode(String s) {
            s = s.replace("&lt;", "<");
            s = s.replace("&gt;", ">");
            s = s.replace("&quot;", "\"");
            s = s.replace("&amp;", "&");
            return s;
        }

        public static String tag(TemplateContext ctx, String tagName, Map<String, Object> attributes, Object value) {
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
                    attributes.put("checked", matched(attributes.get("value"), value));
                    break;
                }
            case "select":
                break;
            case "option":
                innerText = attributes.get("innerText");
                attributes.remove("innerText");
                attributes.put("selected", matched(attributes.getOrDefault("value", innerText), value));
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
                            attrVal = ctx.nameof(attrVal.toString());
                        }
                        sb.append(Statics.encode(attrVal, "@"));
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

        private static boolean matched(Object a, Object b) {
            for (Object v : Sys.each(b)) {
                if (Sys.eq(Sys.toString(a, ""), Sys.toString(v, ""))) {
                    return true;
                }
            }
            return false;
        }
    }
}
