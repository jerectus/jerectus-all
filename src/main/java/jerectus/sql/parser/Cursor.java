package jerectus.sql.parser;

import java.util.List;
import java.util.function.Predicate;

public class Cursor<T> {
    private List<T> list;
    private int index;

    private Cursor(List<T> list, int index) {
        this.list = list;
        this.index = index;
    }

    public T get() {
        return list.get(index);
    }

    public List<T> list() {
        return list;
    }

    public void moveTo(int i) {
        index = i;
    }

    public boolean moveNext() {
        index++;
        if (index >= list.size()) {
            return false;
        }
        return true;
    }

    public void insertAfter(T elem) {
        list.add(index + 1, elem);
    }

    public void insertBefore(T elem) {
        list.add(index, elem);
        index++;
    }

    public void remove(int step) {
        list.remove(index + step);
    }

    public Cursor<T> next() {
        return of(list, index + 1);
    }

    public Cursor<T> find(Predicate<T> pred, int step) {
        int i = index + step;
        if (step > 0) {
            for (; i < list.size(); i += step) {
                if (pred.test(list.get(i))) {
                    break;
                }
            }
        } else if (step < 0) {
            for (; i >= 0; i += step) {
                if (pred.test(list.get(i))) {
                    break;
                }
            }
        }
        return of(list, i);
    }

    public static <T> Cursor<T> of(List<T> list, int index) {
        return new Cursor<T>(list, index);
    }
}
