package jerectus.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jerectus.sql.annotation.Computed;
import jerectus.sql.annotation.From;
import jerectus.sql.annotation.Join;
import jerectus.sql.annotation.Query;
import jerectus.sql.internal.TableMeta;
import jerectus.util.BeanProperty;
import jerectus.util.Sys;
import jerectus.util.function.ThrowingBiConsumer;
import jerectus.util.function.ThrowingConsumer;

public class SqlBuilder<T> {
    static class TableInfo {
        TableMeta meta;
        String name;
        String propertyName;

        public TableInfo(TableMeta meta, String alias, String propertyName) {
            this.meta = meta;
            this.name = Sys.ifEmpty(alias, meta.name);
            this.propertyName = Sys.ifEmpty(propertyName, Sys.camelCase(this.name));
        }
    }

    private Db db;
    private Class<T> clazz;
    private StringBuilder sb = new StringBuilder();
    private List<TableInfo> tables = new ArrayList<>();
    private List<Object> bindParams = new ArrayList<>();
    private StringBuilder computed = new StringBuilder();

    public SqlBuilder(Db db, Class<T> clazz, String alias) {
        this.db = db;
        this.clazz = clazz;
        from(clazz, alias);
    }

    public SqlBuilder(Db db) {
        this.db = db;
    }

    TableInfo addTable(Class<?> clazz, String alias, String propertyName) {
        var table = new TableInfo(Db.mdm.getTableMeta(clazz), alias, propertyName);
        tables.add(table);
        return table;
    }

    SqlBuilder<T> append(Object... values) {
        for (var v : values) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            if (v instanceof TableInfo) {
                var table = (TableInfo) v;
                sb.append(table.meta.name);
                if (!Sys.eq(table.meta.name, table.name)) {
                    sb.append(" ");
                    sb.append(table.name);
                }
            } else {
                sb.append(v);
            }
        }
        return this;
    }

    public SqlBuilder<T> select(String columns) {
        return append("select", columns);
    }

    public SqlBuilder<T> from(Class<?> clazz, String alias, String propertyName) {
        this.clazz = Sys.cast(clazz);
        if (clazz == null)
            return this;
        var q = clazz.getAnnotation(Query.class);
        if (q != null) {
            var f = clazz.getAnnotation(From.class);
            if (f != null) {
                append("from", addTable(clazz.getSuperclass(), f.value(), propertyName));
            }
            BeanProperty.getProperties(clazz).forEach(p -> {
                var j = p.getAnnotation(Join.class);
                if (j != null) {
                    switch (j.type()) {
                    case INNER:
                        join(p.getType(), j.alias(), p.getName());
                        break;
                    case LEFT:
                        leftJoin(p.getType(), j.alias(), p.getName());
                        break;
                    case FULL:
                        fullJoin(p.getType(), j.alias(), p.getName());
                        break;
                    }
                    on(j.on());
                }
                var c = p.getAnnotation(Computed.class);
                if (c != null) {
                    computed.append(", ");
                    computed.append(c.value());
                    computed.append(" \"");
                    computed.append(Sys.snakeCase(p.getName()));
                    computed.append("\"");
                }
            });
            return this;
        }
        return append("from", addTable(clazz, alias, propertyName));
    }

    public SqlBuilder<T> from(Class<?> clazz, String alias) {
        return from(clazz, alias, null);
    }

    public SqlBuilder<T> from(Class<?> clazz) {
        return from(clazz, null, null);
    }

    public SqlBuilder<T> join(String type, Class<?> clazz, String alias, String propertyName) {
        return append(type, "join", addTable(clazz, alias, propertyName));
    }

    public SqlBuilder<T> join(Class<?> clazz, String alias, String propertyName) {
        return join("inner", clazz, alias, propertyName);
    }

    public SqlBuilder<T> join(Class<?> clazz, String alias) {
        return join(clazz, alias, null);
    }

    public SqlBuilder<T> join(Class<?> clazz) {
        return join(clazz, null, null);
    }

    public SqlBuilder<T> leftJoin(Class<?> clazz, String alias, String propertyName) {
        return join("left outer", clazz, alias, propertyName);
    }

    public SqlBuilder<T> leftJoin(Class<?> clazz, String alias) {
        return leftJoin(clazz, alias, null);
    }

    public SqlBuilder<T> leftJoin(Class<?> clazz) {
        return leftJoin(clazz, null, null);
    }

    public SqlBuilder<T> fullJoin(Class<?> clazz, String alias, String propertyName) {
        return join("full outer", clazz, alias, propertyName);
    }

    public SqlBuilder<T> fullJoin(Class<?> clazz, String alias) {
        return fullJoin(clazz, alias, null);
    }

    public SqlBuilder<T> fullJoin(Class<?> clazz) {
        return fullJoin(clazz, null, null);
    }

    public SqlBuilder<T> on(String condition, Object... params) {
        Sys.addAll(bindParams, params);
        return append("on", condition);
    }

    public SqlBuilder<T> and(String condition, Object... params) {
        Sys.addAll(bindParams, params);
        return append("and", condition);
    }

    public SqlBuilder<T> or(String condition, Object... params) {
        Sys.addAll(bindParams, params);
        return append("or", condition);
    }

    public SqlBuilder<T> where(String condition, Object... params) {
        Sys.addAll(bindParams, params);
        return append("where", condition);
    }

    public SqlBuilder<T> in(Object... params) {
        Sys.addAll(bindParams, params);
        return append("in (" + Sys.repeat("?", params.length, ", ") + ")");
    }

    public SqlBuilder<T> in(ThrowingConsumer<SqlBuilder<?>> cn) {
        SqlBuilder<?> subquery = new SqlBuilder<Object>(db, null, null);
        cn.accept(subquery);
        return append("in (" + subquery + ")");
    }

    public SqlBuilder<T> groupBy(String columns) {
        return append("group by", columns);
    }

    public SqlBuilder<T> having(String condition, Object... params) {
        bindParams.addAll(Arrays.asList(params));
        return append("having", condition);
    }

    public SqlBuilder<T> orderBy(String columns) {
        return append("order by", columns);
    }

    public SqlBuilder<T> union() {
        return append("union");
    }

    public SqlBuilder<T> unionAll() {
        return append("union all");
    }

    private String lastCommand = "";

    public SqlBuilder<T> update(Class<?> clazz) {
        var table = Db.mdm.getTableMeta(clazz);
        lastCommand = "update";
        return append("update", table.name);
    }

    public SqlBuilder<T> set(String sql, Object... params) {
        if (params != null && params.length == 1 && sql.matches("[a-zA-Z]\\w*")) {
            sql += " = ?";
        }
        Sys.addAll(bindParams, params);
        append(lastCommand.equals("update") ? "set" : ",");
        lastCommand = "set";
        return append(sql);
    }

    public SqlBuilder<T> with(ThrowingConsumer<SqlBuilder<T>> cn) {
        cn.accept(this);
        return this;
    }

    public String toSQL() {
        String sql = sb.toString();
        if (clazz == null || sql.matches("select\\b.*")) {
            return sql;
        }
        sb.setLength(0);
        sb.append("select ");
        for (int i = 0; i < tables.size(); i++) {
            TableInfo table = tables.get(i);
            if (i > 0) {
                sb.append(", ' ' \"@");
                sb.append(table.propertyName);
                sb.append("\", ");
            }
            sb.append(table.name);
            sb.append(".*");
        }
        sb.append(", ' ' \"@\"");
        sb.append(computed);
        sb.append(" ");
        sb.append(sql);
        return sb.toString();
    }

    public List<Object> getParameters() {
        return bindParams;
    }

    public TypedSqlQueryResult<T> execute() {
        return new TypedSqlQueryResult<T>(clazz, db.select(toSQL(), bindParams));
    }

    public TypedSqlQueryResult<T> limit(int offset, int limitRows) {
        return execute().limit(offset, limitRows);
    }

    public void forEach(ThrowingConsumer<T> cn) {
        execute().forEach(cn);
    }

    public void forEach(ThrowingBiConsumer<T, SqlQueryResult> cn) {
        execute().forEach(cn);
    }

    public T first() {
        return execute().first();
    }

    public List<T> toList() {
        return execute().toList();
    }

    @Override
    public String toString() {
        return toSQL();
    }
}
