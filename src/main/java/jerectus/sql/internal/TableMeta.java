package jerectus.sql.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.persistence.Table;

import jerectus.util.BeanProperty;
import jerectus.util.Sys;

public class TableMeta {
    public final String name;
    public final String className;
    public final List<ColumnMeta> columns;
    public final List<ColumnMeta> idColumns;
    public final List<ColumnMeta> dataColumns;
    public final Class<?> clazz;
    public final String idCondition;

    public TableMeta(Class<?> clazz) {
        if (clazz.getSimpleName().isEmpty()) {
            clazz = clazz.getSuperclass();
        }
        this.clazz = clazz;
        className = clazz.getSimpleName();
        name = getTableName(clazz);
        var columns = new ArrayList<ColumnMeta>();
        var idColumns = new ArrayList<ColumnMeta>();
        var dataColumns = new ArrayList<ColumnMeta>();
        for (var p : BeanProperty.getProperties(clazz)) {
            var col = new ColumnMeta(p);
            columns.add(col);
            if (col.idColumn) {
                idColumns.add(col);
            } else {
                dataColumns.add(col);
            }
        }
        this.columns = Collections.unmodifiableList(columns);
        this.idColumns = Collections.unmodifiableList(idColumns);
        this.dataColumns = Collections.unmodifiableList(dataColumns);
        this.idCondition = Sys.join(idColumns, " and ", it -> it.name + " = ?");
    }

    private static String getTableName(Class<?> clazz) {
        var table = clazz.getAnnotation(Table.class);
        return table != null && !table.name().isEmpty() ? table.name()
                : Sys.snakeCase(clazz.getSimpleName()).toUpperCase();
    }

    private static List<Object> getValues(List<ColumnMeta> metaList, Object entity) {
        return metaList.stream().map(it -> it.getValue(entity)).collect(Collectors.toList());
    }

    public List<Object> getValues(Object entity) {
        return getValues(columns, entity);
    }

    public List<Object> getIdValues(Object entity) {
        return getValues(idColumns, entity);
    }

    public List<Object> getDataValues(Object entity) {
        return getValues(dataColumns, entity);
    }

    private static void eachColumn(List<ColumnMeta> metaList, Object entity, BiConsumer<ColumnMeta, Object> cn) {
        metaList.forEach(col -> {
            cn.accept(col, col.getValue(entity));
        });
    }

    public void eachColumn(Object entity, BiConsumer<ColumnMeta, Object> cn) {
        eachColumn(columns, entity, cn);
    }

    public void eachIdColumn(Object entity, BiConsumer<ColumnMeta, Object> cn) {
        eachColumn(idColumns, entity, cn);
    }

    public void eachDataColumn(Object entity, BiConsumer<ColumnMeta, Object> cn) {
        eachColumn(dataColumns, entity, cn);
    }

    public Map<String, Object> toMap(Object entity) {
        var map = new LinkedHashMap<String, Object>();
        columns.forEach(column -> {
            map.put(column.name, column.getValue(entity));
        });
        return map;
    }
}
