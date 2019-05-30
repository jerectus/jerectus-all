package jerectus.util.function;

@FunctionalInterface
public interface ThrowableRunnable {
    void run() throws Exception;
}
