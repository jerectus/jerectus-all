package jerectus.validation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Valid {
    public boolean required() default false;

    public int minLength() default 0;

    public int maxLength() default Integer.MAX_VALUE;

    public String[] oneOf() default {};

    public String regex() default "";

    public String expression() default "";

    public String on() default "";

    public interface Fn {
        static void validate(Valid valid, ValidationContext ctx) {
            Required.Fn.validate(newRequired(valid.required()), ctx);
            Length.Fn.validate(newLength(valid.minLength(), valid.maxLength()), ctx);
            OneOf.Fn.validate(newOneOf(valid.oneOf()), ctx);
            Regex.Fn.validate(newRegex(valid.regex()), ctx);
            Expression.Fn.validate(newExpression(valid.expression()), ctx);
        }

        static Required newRequired(boolean required) {
            return !required ? null : new Required() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Required.class;
                }

                @Override
                public String toString() {
                    return "@" + annotationType().getName();
                }
            };
        }

        static Length newLength(int minLength, int maxLength) {
            return new Length() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Length.class;
                }

                @Override
                public int min() {
                    return minLength;
                }

                @Override
                public int max() {
                    return maxLength;
                }

                @Override
                public String toString() {
                    return "@" + annotationType().getName() + "(max=" + max() + ", min=" + min() + ")";
                }
            };
        }

        static OneOf newOneOf(String[] values) {
            return new OneOf() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return OneOf.class;
                }

                @Override
                public String[] value() {
                    return values;
                }

                @Override
                public String toString() {
                    return "@" + annotationType().getName() + "(value=\"" + value() + "\")";
                }
            };
        }

        static Regex newRegex(String pattern) {
            return new Regex() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Regex.class;
                }

                @Override
                public String value() {
                    return pattern;
                }

                @Override
                public String toString() {
                    return "@" + annotationType().getName() + "(value=\"" + value() + "\")";
                }
            };
        }

        static Expression newExpression(String expr) {
            return new Expression() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Expression.class;
                }

                @Override
                public String value() {
                    return expr;
                }

                @Override
                public String toString() {
                    return "@" + annotationType().getName() + "(value=\"" + value() + "\")";
                }
            };
        }
    }
}
