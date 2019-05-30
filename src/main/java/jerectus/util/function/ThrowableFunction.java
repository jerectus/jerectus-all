package jerectus.util.function;

@FunctionalInterface
public interface ThrowableFunction<T, R> {
    R apply(T arg) throws Exception;
}
