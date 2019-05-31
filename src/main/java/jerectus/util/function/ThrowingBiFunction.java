package jerectus.util.function;

import java.util.function.BiFunction;

import jerectus.util.Try;

@FunctionalInterface
public interface ThrowingBiFunction<T, U, R> extends BiFunction<T, U, R> {
    R applyEx(T t, U u) throws Exception;

    default R apply(T t, U u) {
        return Try.get(() -> applyEx(t, u));
    }
}
