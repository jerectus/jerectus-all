package jerectus.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.ObjectContext;

import jerectus.util.Sys;

@Retention(RetentionPolicy.RUNTIME)
public @interface Expression {
    String value() default "";

    public interface Fn {
        static JexlEngine engine = new JexlBuilder().namespaces(Map.of("fn", Functions.class)).create();

        static void validate(Expression expression, ValidationContext ctx) {
            if (!Sys.isEmpty(ctx.value()) && !expression.value().isEmpty()) {
                var je = engine.createExpression(expression.value());
                var jc = new ObjectContext<>(engine, ctx.bean());
                var result = je.evaluate(jc);
                if (Boolean.FALSE.equals(result)) {
                    throw new ValidateException(expression, ctx);
                } else if (result instanceof String && !"".equals(result)) {
                    throw new ValidateException(ctx.bean().getClass(), (String) result);
                }
            }
        }
    }

    public static class Functions {
        public static Class<?> type(String name) {
            try {
                return Class.forName((name.indexOf(".") == -1 ? "java.lang." : "") + name);
            } catch (ClassNotFoundException e) {
                throw Sys.asRuntimeException(e);
            }
        }
    }
}