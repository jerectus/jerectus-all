package jerectus.util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jerectus.util.function.ThrowableBiConsumer;
import jerectus.util.function.ThrowableBiFunction;
import jerectus.util.function.ThrowableConsumer;
import jerectus.util.function.ThrowableFunction;
import jerectus.util.function.ThrowableRunnable;
import jerectus.util.function.ThrowableSupplier;

public class Try {
    public static <T, R> Function<T, R> with(ThrowableFunction<T, R> fn) {
        return fn;
    }

    public static <T, R> Function<T, R> with(ThrowableFunction<T, R> fn, BiFunction<Exception, T, R> onCatch) {
        return arg -> {
            try {
                return fn.applyEx(arg);
            } catch (Exception e) {
                return onCatch.apply(e, arg);
            }
        };
    }

    public static <T, U, R> BiFunction<T, U, R> with(ThrowableBiFunction<T, U, R> fn) {
        return fn;
    }

    public static <T, U, R> BiFunction<T, U, R> with(ThrowableBiFunction<T, U, R> fn,
            BiFunction<Exception, Object[], R> onCatch) {
        return (arg1, arg2) -> {
            try {
                return fn.applyEx(arg1, arg2);
            } catch (Exception e) {
                return onCatch.apply(e, new Object[] { arg1, arg2 });
            }
        };
    }

    public static <R> Supplier<R> with(ThrowableSupplier<R> fn) {
        return fn;
    }

    public static <R> Supplier<R> with(ThrowableSupplier<R> fn, Function<Exception, R> onCatch) {
        return () -> {
            try {
                return fn.getEx();
            } catch (Exception e) {
                return onCatch.apply(e);
            }
        };
    }

    public static <T> Consumer<T> with(ThrowableConsumer<T> fn) {
        return fn;
    }

    public static <T, U> BiConsumer<T, U> with(ThrowableBiConsumer<T, U> fn) {
        return fn;
    }

    public static Runnable with(ThrowableRunnable fn) {
        return fn;
    }

    public static <T> T get(ThrowableSupplier<T> fn) {
        try {
            return fn.getEx();
        } catch (Exception e) {
            throw Sys.asRuntimeException(e);
        }
    }

    public static void run(ThrowableRunnable fn) {
        try {
            fn.runEx();
        } catch (Exception e) {
            throw Sys.asRuntimeException(e);
        }
    }
}
