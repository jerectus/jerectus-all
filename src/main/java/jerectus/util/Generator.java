package jerectus.util;

@FunctionalInterface
public interface Generator<T> {
    void yield(T value);
}