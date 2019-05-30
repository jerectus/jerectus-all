package jerectus.util.function;

import java.util.function.Function;

import jerectus.util.Try;

@FunctionalInterface
public interface ThrowableFunction<T, R> extends Function<T, R> {
    R applyEx(T arg) throws Exception;

    default R apply(T arg) {
        return Try.get(() -> applyEx(arg));
    }
}
