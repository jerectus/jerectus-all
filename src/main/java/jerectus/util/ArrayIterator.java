package jerectus.util;

import java.lang.reflect.Array;
import java.util.Iterator;

public class ArrayIterator<T> implements Iterator<T> {
    private Object array;
    private int n;
    private int i;

    public ArrayIterator(Object o) {
        array = o;
        n = Array.getLength(o);
        i = 0;
    }

    @Override
    public boolean hasNext() {
        return i < n;
    }

    @Override
    public T next() {
        return Sys.cast(Array.get(array, i++));
    }
}
