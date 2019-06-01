package jerectus.util.function;

import java.util.function.Consumer;

import jerectus.util.Sys;

@FunctionalInterface
public interface TryConsumer<T> extends Consumer<T> {
    void tryAccept(T arg) throws Exception;

    default void accept(T t) {
        try {
            tryAccept(t);
        } catch (Exception e) {
            throw Sys.asRuntimeException(e, t);
        }
    }
}
