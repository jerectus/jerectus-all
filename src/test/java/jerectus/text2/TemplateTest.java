package jerectus.text2;

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
        var tmpl = engine.createTemplate(
                "<%var l=[{'x':1},{'x':2}]; %><%%for a : l ; delim=`,`%>\n<%=a%> : <%=__stat.getRealName(`a.x`)%><%%end%>");
        var vars = new HashMap<>();
        vars.put("name", "Abe");
        System.out.println(tmpl.execute(vars));
    }
}