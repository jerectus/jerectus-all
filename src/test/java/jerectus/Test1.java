package jerectus;

import java.io.PrintWriter;

import org.junit.Test;

import jerectus.html.template.HtmlTemplate;
import jerectus.util.Resources;

public class Test1 {
    @Test
    public void test1() {
        var tmpl = new HtmlTemplate(Resources.get(this, "test1.html"));
        tmpl.render(new PrintWriter(System.out), null);
    }
}
