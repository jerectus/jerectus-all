package jerectus.util.function;

@FunctionalInterface
public interface ThrowableSupplier<R> {
    R get() throws Exception;
}
