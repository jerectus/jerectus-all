package jerectus.sql.internal;

import javax.persistence.Column;
import javax.persistence.Id;

import jerectus.util.BeanProperty;
import jerectus.util.Sys;
import jerectus.util.Try;

public class ColumnMeta {
    public final String name;
    public final String propertyName;
    public final boolean idColumn;
    public final BeanProperty property;
    public final Class<?> propertyType;

    public ColumnMeta(BeanProperty property) {
        idColumn = property.getAnnotation(Id.class) != null;
        propertyName = property.getName();
        var column = property.getAnnotation(Column.class);
        name = column != null && !column.name().isEmpty() ? column.name() : Sys.snakeCase(propertyName).toUpperCase();
        this.property = property;
        this.propertyType = property.getType();
    }

    public Object getValue(Object entity) {
        return Try.get(() -> property.get(entity));
    }
}
