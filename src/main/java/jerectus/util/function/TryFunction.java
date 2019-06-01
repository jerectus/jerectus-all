package jerectus.util.function;

import java.util.function.Function;

import jerectus.util.Sys;

@FunctionalInterface
public interface TryFunction<T, R> extends Function<T, R> {
    R tryApply(T t) throws Exception;

    default R apply(T t) {
        try {
            return tryApply(t);
        } catch (Exception e) {
            throw Sys.asRuntimeException(e, t);
        }
    }
}
