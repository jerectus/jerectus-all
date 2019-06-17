package jerectus.text2;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.jexl3.MapContext;

import jerectus.util.Sys;

public class TemplateContext extends MapContext {
    public TemplateContext(Object vars) {
        super(vars instanceof Map ? Sys.cast(vars) : Sys.populate(vars));
    }

    @Override
    public Object get(String name) {
        return "__ctx".equals(name) ? this : super.get(name);
    }

    @Override
    public boolean has(String name) {
        return "__ctx".equals(name) || super.has(name);
    }

    public EachStat each(EachStat parent, Object values, String valuesExpr, String varExpr) {
        return new EachStat(parent, values, valuesExpr, varExpr);
    }

    public static class EachStat implements Iterable<EachStat> {
        EachStat parent;
        Object values;
        int size;
        int index = -1;
        Object value;
        String varName;
        String valuesExpr;
        String realBaseName;

        public EachStat(EachStat parent, Object values, String valuesExpr, String varName) {
            this.parent = parent;
            this.values = values;
            this.size = size(values);
            this.valuesExpr = valuesExpr;
            this.varName = varName;
            realBaseName = parent == null ? valuesExpr : parent.getRealName(valuesExpr);
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

        public String getRealName(String name) {
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
}
