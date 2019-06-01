package jerectus.util.function;

import java.util.function.BiFunction;

import jerectus.util.Sys;

@FunctionalInterface
public interface TryBiFunction<T, U, R> extends BiFunction<T, U, R> {
    R tryApply(T t, U u) throws Exception;

    default R apply(T t, U u) {
        try {
            return tryApply(t, u);
        } catch (Exception e) {
            throw Sys.asRuntimeException(e, t, u);
        }
    }
}
