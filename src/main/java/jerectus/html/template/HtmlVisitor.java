package jerectus.html.template;

import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

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