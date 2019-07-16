package jerectus.tools;

import java.nio.file.Paths;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.MapContext;

public class JexlRunner {
    public static void main(String... args) throws Exception {
        var engine = new JexlBuilder().create();
        var script = engine.createScript(Paths.get(JexlRunner.class.getResource("main.jexl").toURI()).toFile());
        var context = new MapContext();
        context.set("System", System.class);
        context.set("__context__", context);
        script.execute(context);
    }
}
