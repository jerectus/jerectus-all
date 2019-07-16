package jerectus.validation;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jerectus.util.BeanProperty;
import jerectus.util.template.TemplateEngine;

public class ValidateException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public Annotation constraint;
    public Object bean;
    public BeanProperty property;
    public Object value;

    public ValidateException(Annotation constraint, ValidationContext ctx) {
        this.constraint = constraint;
        this.bean = ctx.bean();
        this.property = ctx.property();
        this.value = ctx.value();
    }

    public ValidateException(Class<?> clazz, Object message) {

    }

    private static final TemplateEngine engine = new TemplateEngine();

    @Override
    public String toString() {
        if (property == null || constraint == null) {
            return "error";
        }
        var res = new Properties();
        try (var in = bean.getClass().getResourceAsStream("message.properties")) {
            res.load(in);
            var name = property.getName();
            name = res.getProperty("model." + name, name);
            var msg = res.getProperty(constraint.annotationType().getName());
            if (msg != null) {
                var tmpl = engine.createTemplate(msg);
                return tmpl.execute(new HashMap<>(Map.of("name", name, "constraint", constraint, "$C", constraint)));
            }
        } catch (Exception e) {
        }

        return (property != null ? property.getName() + ":" : "") + constraint + ":" + value;
    }
}
