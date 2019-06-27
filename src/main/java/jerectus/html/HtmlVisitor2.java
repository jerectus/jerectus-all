package jerectus.html;

import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

public class HtmlVisitor2 implements NodeVisitor {
    @Override
    public void head(Node node, int depth) {
        if (node instanceof Document) {
            // do nothing
        } else if (node instanceof Element) {
            element((Element) node);
        } else if (node instanceof TextNode) {
            text((TextNode) node);
        } else if (node instanceof Comment) {
            comment((Comment) node);
        } else {
            other(node);
        }
    }

    @Override
    public void tail(Node node, int depth) {
        if (node instanceof Element) {
            var elem = (Element) node;
            if (hasEndTag(elem)) {
                endElement(elem);
            }
        }
    }

    public void element(Element elem) {
    }

    public void endElement(Element elem) {
    }

    public void text(TextNode elem) {
    }

    public void comment(Comment elem) {
    }

    public void other(Node node) {
    }

    private static boolean hasEndTag(Element elem) {
        return !(elem instanceof Document)
                && !elem.tagName().matches("br|img|hr|meta|input|embed|area|base|col|keygen|link|param|source");
    }
}
