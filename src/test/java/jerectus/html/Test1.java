package jerectus.html;

import java.util.HashMap;

import org.junit.Test;

import jerectus.html.template.HtmlTemplate;
import jerectus.util.Resources;

public class Test1 {
    @Test
    public void test1() {
        var tmpl = new HtmlTemplate(Resources.get(this, "test1.html"));
        System.out.println(tmpl);
        var vars = new HashMap<>();
        vars.put("name", "Abe");
        vars.put("list", new int[] { 1, 10, 100 });
        tmpl.render(System.out, vars);
        System.out.println();
    }
}
