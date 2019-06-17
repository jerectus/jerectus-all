package jerectus.text;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Pattern;

import jerectus.util.Sys;

public class EachContext extends TemplateContext implements Iterable<EachContext> {
    EachContext parent;
    Object values;
    String varName;
    String valuesExpr;
    String realBaseName;
    int size;
    int index = -1;
    Object value;

    public EachContext(TemplateContext parent, Object values, String valuesExpr, String varName) {
        super(parent);
        this.parent = parent instanceof EachContext ? (EachContext) parent : null;
        this.values = values;
        this.valuesExpr = valuesExpr;
        this.varName = varName;
        this.size = size(values);
        realBaseName = parent == null ? valuesExpr : parent.nameof(valuesExpr);
    }

    @Override
    public Iterator<EachContext> iterator() {
        var it = Sys.iterator(values);
        return new Iterator<EachContext>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public EachContext next() {
                index++;
                value = it.next();
                return EachContext.this;
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

    public String nameof(String name) {
        var p = Pattern.compile("([_a-zA-Z\\$]\\w*)(.*)");
        var m = p.matcher(name);
        if (m.matches()) {
            String rootName = m.group(1);
            for (var i = this; i != null; i = i.parent) {
                if (i.varName.equals(rootName)) {
                    return i.realBaseName + "[" + i.index + "]" + m.group(2);
                }
            }
        }
        return name;
    }

    private static int size(Object o) {
        if (o == null) {
            return 0;
        } else if (o instanceof Collection) {
            Collection<Object> c = Sys.cast(o);
            return c.size();
        } else if (o.getClass().isArray()) {
            return Array.getLength(o);
        } else {
            return 1;
        }
    }
}
