package jerectus.util.function;

import java.util.function.BiConsumer;

import jerectus.util.Try;

@FunctionalInterface
public interface ThrowingBiConsumer<T, U> extends BiConsumer<T, U> {
    void acceptEx(T t, U u) throws Exception;

    default void accept(T t, U u) {
        Try.run(() -> acceptEx(t, u));
    }
}
