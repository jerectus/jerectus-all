package jerectus.sql.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Join {
    enum Type {
        INNER, LEFT, FULL
    }

    Type type() default Type.INNER;

    String alias() default "";

    String on();
}
