package jerectus.util;

import java.util.Iterator;

public class ArrayIterable<T> implements Iterable<T> {
    private Object array;

    public ArrayIterable(Object array) {
        this.array = array;
    }

    @Override
    public Iterator<T> iterator() {
        return new ArrayIterator<T>(array);
    }
}
