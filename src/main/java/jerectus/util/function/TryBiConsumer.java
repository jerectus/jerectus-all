package jerectus.util.function;

import java.util.function.BiConsumer;

import jerectus.util.Try;

@FunctionalInterface
public interface TryBiConsumer<T, U> extends BiConsumer<T, U> {
    void tryAccept(T t, U u) throws Exception;

    default void accept(T t, U u) {
        try {
            tryAccept(t, u);
        } catch (Exception e) {
            throw Try.asRuntimeException(e, t, u);
        }
    }
}
