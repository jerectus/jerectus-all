package jerectus.util.function;

@FunctionalInterface
public interface ThrowableConsumer<T> {
    void accept(T arg) throws Exception;
}
