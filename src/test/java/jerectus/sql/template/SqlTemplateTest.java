package jerectus.sql.template;

import java.nio.file.Paths;
import java.util.HashMap;

import org.junit.Test;

public class SqlTemplateTest {
    @Test
    public void test1() throws Exception {
        var path = Paths.get(getClass().getResource("test1.sql").toURI());
        var tpl = new SqlTemplate(path);
        var vars = new HashMap<>();
        // vars.put("id", 1);
        vars.put("IDs", new int[] { 1, 22, 333 });
        System.out.println(tpl.process(vars));
    }
}