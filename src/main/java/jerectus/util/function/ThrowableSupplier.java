package jerectus.util.function;

import java.util.function.Supplier;

import jerectus.util.Try;

@FunctionalInterface
public interface ThrowableSupplier<R> extends Supplier<R> {
    R getEx() throws Exception;

    default R get() {
        return Try.get(() -> getEx());
    }
}
