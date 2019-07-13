package jerectus.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jerectus.util.Sys;

@Retention(RetentionPolicy.RUNTIME)
public @interface Length {
    int min() default 0;

    int max() default -1;

    public interface Fn {
        static void validate(Length length, ValidationContext ctx) {
            if (!Sys.isEmpty(ctx.value())) {
                var len = Sys.toString(ctx.value()).length();
                if (len < length.min() || length.max() != -1 && len > length.max()) {
                    throw new ValidateException(length, ctx);
                }
            }
        }
    }
}
