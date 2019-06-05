package jerectus.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public abstract class YieldIterator<T> implements Iterator<T> {
    private Queue<T> cache = new LinkedList<>();

    @Override
    public boolean hasNext() {
        if (cache.isEmpty()) {
            generate();
        }
        return !cache.isEmpty();
    }

    @Override
    public T next() {
        return hasNext() ? cache.poll() : null;
    }

    public void yield(T value) {
        cache.add(value);
    }

    public abstract void generate();
}
