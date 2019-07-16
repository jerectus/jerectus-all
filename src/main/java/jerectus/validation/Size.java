package jerectus.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jerectus.util.Sys;

@Retention(RetentionPolicy.RUNTIME)
public @interface Size {
    int min() default 0;

    int max() default -1;

    public interface Fn {
        static void validate(Size size, ValidationContext ctx) {
            if (!Sys.isEmpty(ctx.value())) {
                var len = Sys.toString(ctx.value()).length();
                if (len < size.min() || size.max() != -1 && len > size.max()) {
                    throw new ValidateException(size, ctx);
                }
            }
        }
    }
}
