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

    @Test
    public void test2() {
        var engine = new TemplateEngine();
        var tmpl = engine.createTemplate("<%%for a : 1..3 ; delim=`,`%>\n<%=a%><%%end%>");
        var vars = new HashMap<>();
        vars.put("name", "Abe");
        System.out.println(tmpl.execute(vars));
    }
}