package jerectus.html;

import java.io.PrintWriter;
import java.util.HashMap;

import org.junit.Test;

import jerectus.html.template.HtmlTemplate;
import jerectus.util.Resources;

public class Test1 {
    @Test
    public void test1() {
        var tmpl = new HtmlTemplate(Resources.get(this, "test1.html"));
        var vars = new HashMap<>();
        vars.put("name", "Abe");
        tmpl.render(new PrintWriter(System.out, true), vars);
        System.out.println();
    }
}
