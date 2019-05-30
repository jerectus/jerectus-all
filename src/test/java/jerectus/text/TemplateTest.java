package jerectus.text;

import java.util.HashMap;

import org.junit.Test;

public class TemplateTest {
    @Test
    public void test1() {
        var engine = new TemplateEngine();
        var tmpl = engine.createTemplate("hello <%= name %>.");
        var vars = new HashMap<>();
        vars.put("name", "Abe");
        System.out.println(tmpl.execute(vars));
    }
}