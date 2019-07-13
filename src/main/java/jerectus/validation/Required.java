package jerectus.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jerectus.util.Sys;

@Retention(RetentionPolicy.RUNTIME)
public @interface Required {
    public class Fn {
        static void validate(Required required, ValidationContext ctx) {
            if (required != null && Sys.isEmpty(ctx.value())) {
                throw new ValidateException(required, ctx);
            }
        }
    }
}
