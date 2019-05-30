package jerectus.util.function;

import jerectus.util.Try;

@FunctionalInterface
public interface ThrowableRunnable extends Runnable {
    void runEx() throws Exception;

    default void run() {
        Try.run(() -> runEx());
    }
}
