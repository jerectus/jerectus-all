package jerectus.sql.template;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.Test;

public class SqlTemplateTest {
    @Test
    public void test1() throws Exception {
        var path = Paths.get(getClass().getResource("test1.sql").toURI());
        var tpl = new SqlTemplate(path);
        var vars = new HashMap<>();
        vars.put("IDsx", Arrays.asList(1, 3, 5));
        System.out.println(tpl.process(vars).sql);
    }
}