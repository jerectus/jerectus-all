package jerectus.text;

import java.util.Iterator;

import jerectus.util.Sys;

public class EachStat implements Iterable<EachStat> {
    EachStat parent;
    Object values;
    String valuesExpr;
    String varName;
    String baseName;
    int size;
    int index = -1;
    Object value;

    public EachStat(EachStat parent, Object values, String valuesExpr, String varName, String baseName) {
        this.parent = parent;
        this.values = values;
        this.valuesExpr = valuesExpr;
        this.varName = varName;
        this.baseName = baseName;
        this.size = Sys.size(values);
    }

    @Override
    public Iterator<EachStat> iterator() {
        var it = Sys.iterator(values);
        return new Iterator<EachStat>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public EachStat next() {
                index++;
                value = it.next();
                return EachStat.this;
            }
        };
    }

    public Object getValue() {
        return value;
    }

    public int getIndex() {
        return index;
    }

    public int getCount() {
        return index + 1;
    }

    public boolean isFirst() {
        return index == 0;
    }

    public boolean isLast() {
        return index == size - 1;
    }

    public boolean isOdd() {
        return index % 2 == 0;
    }

    public boolean isEven() {
        return index % 2 == 1;
    }
}