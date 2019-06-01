package jerectus.util.function;

import jerectus.util.Sys;

@FunctionalInterface
public interface TryRunnable extends Runnable {
    void tryRun() throws Exception;

    default void run() {
        try {
            tryRun();
        } catch (Exception e) {
            throw Sys.asRuntimeException(e);
        }
    }
}
