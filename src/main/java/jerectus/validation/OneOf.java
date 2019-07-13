package jerectus.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jerectus.util.Sys;

@Retention(RetentionPolicy.RUNTIME)
public @interface OneOf {
    String[] value() default {};

    public interface Fn {
        static void validate(OneOf oneOf, ValidationContext ctx) {
            if (!Sys.isEmpty(ctx.value())) {
                if (oneOf.value().length > 0 && Sys.indexOf(oneOf.value(), Sys.toString(ctx.value())) == -1) {
                    throw new ValidateException(oneOf, ctx);
                }
            }
        }
    }
}
