package jerectus.util.function;

import java.util.function.Supplier;

import jerectus.util.Sys;

@FunctionalInterface
public interface TrySupplier<R> extends Supplier<R> {
    R tryGet() throws Exception;

    default R get() {
        try {
            return tryGet();
        } catch (Exception e) {
            throw Sys.asRuntimeException(e);
        }
    }
}
