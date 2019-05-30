package jerectus.util.function;

import java.util.function.Consumer;

import jerectus.util.Try;

@FunctionalInterface
public interface ThrowableConsumer<T> extends Consumer<T> {
    void acceptEx(T arg) throws Exception;

    default void accept(T arg) {
        Try.run(() -> acceptEx(arg));
    }
}
