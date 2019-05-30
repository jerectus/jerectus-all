package jerectus.util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jerectus.util.function.Param2;
import jerectus.util.function.Param3;
import jerectus.util.function.ThrowableBiConsumer;
import jerectus.util.function.ThrowableBiFunction;
import jerectus.util.function.ThrowableConsumer;
import jerectus.util.function.ThrowableFunction;
import jerectus.util.function.ThrowableRunnable;
import jerectus.util.function.ThrowableSupplier;

public class Try {
    public static <T1, T2> Param2<T1, T2> param(T1 param1, T2 param2) {
        return new Param2<T1, T2>(param1, param2);
    }

    public static <T1, T2, T3> Param3<T1, T2, T3> param(T1 param1, T2 param2, T3 param3) {
        return new Param3<T1, T2, T3>(param1, param2, param3);
    }

    public static <T, R> Function<T, R> with(ThrowableFunction<T, R> fn) {
        return arg -> {
            try {
                return fn.apply(arg);
            } catch (Exception e) {
                throw Sys.asRuntimeException(e);
            }
        };
    }

    public static <T, R> Function<T, R> with(ThrowableFunction<T, R> fn, BiFunction<Exception, T, R> onCatch) {
        return arg -> {
            try {
                return fn.apply(arg);
            } catch (Exception e) {
                return onCatch.apply(e, arg);
            }
        };
    }

    public static <T, U, R> BiFunction<T, U, R> with(ThrowableBiFunction<T, U, R> fn) {
        return (arg1, arg2) -> {
            try {
                return fn.apply(arg1, arg2);
            } catch (Exception e) {
                throw Sys.asRuntimeException(e);
            }
        };
    }

    public static <T, U, R> BiFunction<T, U, R> with(ThrowableBiFunction<T, U, R> fn,
            BiFunction<Exception, Param2<T, U>, R> onCatch) {
        return (arg1, arg2) -> {
            try {
                return fn.apply(arg1, arg2);
            } catch (Exception e) {
                return onCatch.apply(e, Try.param(arg1, arg2));
            }
        };
    }

    public static <R> Supplier<R> with(ThrowableSupplier<R> fn) {
        return () -> {
            try {
                return fn.get();
            } catch (Exception e) {
                throw Sys.asRuntimeException(e);
            }
        };
    }

    public static <R> Supplier<R> with(ThrowableSupplier<R> fn, Function<Exception, R> onCatch) {
        return () -> {
            try {
                return fn.get();
            } catch (Exception e) {
                return onCatch.apply(e);
            }
        };
    }

    public static <T> Consumer<T> with(ThrowableConsumer<T> fn) {
        return arg -> {
            try {
                fn.accept(arg);
            } catch (Exception e) {
                throw Sys.asRuntimeException(e);
            }
        };
    }

    public static <T, U> BiConsumer<T, U> with(ThrowableBiConsumer<T, U> fn) {
        return (arg1, arg2) -> {
            try {
                fn.accept(arg1, arg2);
            } catch (Exception e) {
                throw Sys.asRuntimeException(e);
            }
        };
    }

    public static Runnable with(ThrowableRunnable fn) {
        return () -> {
            try {
                fn.run();
            } catch (Exception e) {
                throw Sys.asRuntimeException(e);
            }
        };
    }

    public static <T> T get(ThrowableSupplier<T> fn) {
        return with(fn).get();
    }

    public static void run(ThrowableRunnable fn) {
        with(fn).run();
    }
}
