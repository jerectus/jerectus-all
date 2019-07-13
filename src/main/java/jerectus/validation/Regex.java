package jerectus.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.regex.Pattern;

import jerectus.util.Sys;

@Retention(RetentionPolicy.RUNTIME)
public @interface Regex {
    String value() default "";

    public interface Fn {
        static void validate(Regex regex, ValidationContext ctx) {
            if (!Sys.isEmpty(ctx.value()) && !regex.value().isEmpty()) {
                if (!Pattern.matches(regex.value(), Sys.toString(ctx.value()))) {
                    throw new ValidateException(regex, ctx);
                }
            }
        }
    }
}