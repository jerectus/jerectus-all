package jerectus.util.function;

@FunctionalInterface
public interface ThrowableBiConsumer<T1, T2> {
    void accept(T1 arg1, T2 arg2) throws Exception;
}
