package jerectus.util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jerectus.util.function.TryBiConsumer;
import jerectus.util.function.TryBiFunction;
import jerectus.util.function.TryConsumer;
import jerectus.util.function.TryFunction;
import jerectus.util.function.TryRunnable;
import jerectus.util.function.TrySupplier;

public class Try {
    public static <T, R> Function<T, R> to(TryFunction<T, R> fn) {
        return fn;
    }

    public static <T, R> Function<T, R> to(TryFunction<T, R> fn, Function<T, Function<Exception, R>> onCatch) {
        return t -> {
            try {
                return fn.tryApply(t);
            } catch (Exception e) {
                return onCatch.apply(t).apply(e);
            }
        };
    }

    public static <T, U, R> BiFunction<T, U, R> to(TryBiFunction<T, U, R> fn) {
        return fn;
    }

    public static <T, U, R> BiFunction<T, U, R> to(TryBiFunction<T, U, R> fn,
            BiFunction<T, U, Function<Exception, R>> onCatch) {
        return (t, u) -> {
            try {
                return fn.tryApply(t, u);
            } catch (Exception e) {
                return onCatch.apply(t, u).apply(e);
            }
        };
    }

    public static <R> Supplier<R> to(TrySupplier<R> fn) {
        return fn;
    }

    public static <R> Supplier<R> to(TrySupplier<R> fn, Function<Exception, R> onCatch) {
        return () -> {
            try {
                return fn.tryGet();
            } catch (Exception e) {
                return onCatch.apply(e);
            }
        };
    }

    public static <T> Consumer<T> accept(TryConsumer<T> fn) {
        return fn;
    }

    public static <T, U> BiConsumer<T, U> accept(TryBiConsumer<T, U> fn) {
        return fn;
    }

    public static <T> T get(TrySupplier<T> fn) {
        return fn.get();
    }

    public static void run(TryRunnable fn) {
        fn.run();
    }

    public static RuntimeException asRuntimeException(Exception e, Object... params) {
        return Sys.asRuntimeException(e, params);
    }
}
