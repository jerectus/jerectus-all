package jerectus.sql.template;

import java.util.HashMap;

import org.junit.Test;

import jerectus.util.Resources;
import jerectus.util.logging.Logger;

public class SqlTemplateTest {
    @Test
    public void test1() throws Exception {
        Logger.getLogger("jerectus").setLevel(Logger.DEBUG);
        var tpl = new SqlTemplate(Resources.get(this, "test1.sql"));
        var vars = new HashMap<>();
        // vars.put("id", 1);
        vars.put("names", new String[] { "a", "b" });
        vars.put("orderBy", 2);
        System.out.println(tpl.process(vars));
    }
}