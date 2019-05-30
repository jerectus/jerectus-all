package jerectus.util.function;

@FunctionalInterface
public interface ThrowableBiFunction<T1, T2, R> {
    R apply(T1 arg1, T2 arg2) throws Exception;
}
