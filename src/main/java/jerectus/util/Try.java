package jerectus.util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jerectus.util.function.ThrowingBiConsumer;
import jerectus.util.function.ThrowingBiFunction;
import jerectus.util.function.ThrowingConsumer;
import jerectus.util.function.ThrowingFunction;
import jerectus.util.function.ThrowingRunnable;
import jerectus.util.function.ThrowingSupplier;

public class Try {
    public static <T, R> Function<T, R> to(ThrowingFunction<T, R> fn) {
        return fn;
    }

    public static <T, R> Function<T, R> to(ThrowingFunction<T, R> fn, BiFunction<Exception, T, R> onCatch) {
        return arg -> {
            try {
                return fn.applyEx(arg);
            } catch (Exception e) {
                return onCatch.apply(e, arg);
            }
        };
    }

    public static <T, U, R> BiFunction<T, U, R> to(ThrowingBiFunction<T, U, R> fn) {
        return fn;
    }

    public static <T, U, R> BiFunction<T, U, R> to(ThrowingBiFunction<T, U, R> fn,
            BiFunction<Exception, Object[], R> onCatch) {
        return (arg1, arg2) -> {
            try {
                return fn.applyEx(arg1, arg2);
            } catch (Exception e) {
                return onCatch.apply(e, new Object[] { arg1, arg2 });
            }
        };
    }

    public static <R> Supplier<R> to(ThrowingSupplier<R> fn) {
        return fn;
    }

    public static <R> Supplier<R> to(ThrowingSupplier<R> fn, Function<Exception, R> onCatch) {
        return () -> {
            try {
                return fn.getEx();
            } catch (Exception e) {
                return onCatch.apply(e);
            }
        };
    }

    public static <T> Consumer<T> to(ThrowingConsumer<T> fn) {
        return fn;
    }

    public static <T, U> BiConsumer<T, U> to(ThrowingBiConsumer<T, U> fn) {
        return fn;
    }

    public static Runnable to(ThrowingRunnable fn) {
        return fn;
    }

    public static <T> T get(ThrowingSupplier<T> fn) {
        try {
            return fn.getEx();
        } catch (Exception e) {
            throw Sys.asRuntimeException(e);
        }
    }

    public static void run(ThrowingRunnable fn) {
        try {
            fn.runEx();
        } catch (Exception e) {
            throw Sys.asRuntimeException(e);
        }
    }
}
