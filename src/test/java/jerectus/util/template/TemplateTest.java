package jerectus.util.template;

import java.util.HashMap;

import org.junit.Test;

import jerectus.util.Resources;

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
        var tmpl = engine.createTemplate("<%%for a : 1..3 ; delim=`,`%>\n<%=a%><%%end-for%>");
        var vars = new HashMap<>();
        vars.put("name", "Abe");
        System.out.println(tmpl.execute(vars));
    }

    @Test
    public void test3() {
        var engine = new TemplateEngine();
        var tmpl = engine.createTemplate(
                "<%var l=[{'x':1},{'x':2}]; %><%%for a : l ; delim=`,`%>\n<%=a%> : <%=__ctx.nameof(`a.x`)%><%%end-for%>");
        var vars = new HashMap<>();
        vars.put("name", "Abe");
        // System.out.println(tmpl.execute(vars));
        tmpl.writeTo(vars, System.out);
        // System.out.println();
    }

    @Test
    public void test4() {
        var engine = new TemplateEngine();
        var tmpl = engine.getTemplate(Resources.getMember(this, "test1.txt"));
        System.out.println(tmpl);
        var vars = new HashMap<>();
        tmpl.writeTo(vars, System.out);
    }
}
