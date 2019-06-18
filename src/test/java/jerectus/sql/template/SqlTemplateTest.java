package jerectus.sql.template;

import java.util.HashMap;

import org.junit.Test;

import jerectus.util.Resources;

public class SqlTemplateTest {
    @Test
    public void test1() throws Exception {
        var tpl = new SqlTemplate(Resources.get(this, "test1.sql"));
        var vars = new HashMap<>();
        vars.put("id", 1);
        // vars.put("IDs", new int[] { 1, 22, 333 });
        vars.put("name", "be");
        vars.put("orderBy", 2);
        System.out.println(tpl.process(vars));
    }
}