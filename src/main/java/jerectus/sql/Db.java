package jerectus.sql;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jerectus.io.IO;
import jerectus.sql.internal.TableMeta;
import jerectus.sql.parser.SqlParser;
import jerectus.sql.template.SqlTemplate;
import jerectus.util.Sys;
import jerectus.util.Try;
import jerectus.util.function.ThrowingConsumer;
import jerectus.util.logging.Logger;

public class Db implements AutoCloseable {
    private static final Logger log = Logger.getLogger(Db.class);
    static MetadataManager mdm = new MetadataManager();
    private Connection conn;

    Db(Connection conn) {
        this.conn = conn;
        Try.run(() -> conn.setAutoCommit(false));
    }

    public static Db open(String url, String user, String password) {
        return Try.get(() -> new Db(DriverManager.getConnection(url, user, password)));
    }

    @Override
    public void close() {
        Try.run(() -> conn.close());
    }

    public Connection getConnection() {
        return conn;
    }

    public SqlQueryResult select(SqlQuery q, Object param) {
        return q.execute(conn, param);
    }

    public SqlQueryResult select(SqlQuery q) {
        return select(q, null);
    }

    public <T> TypedSqlQueryResult<T> select(TypedSqlQuery<T> q, Object param) {
        return q.execute(conn, param);
    }

    public <T> TypedSqlQueryResult<T> select(TypedSqlQuery<T> q) {
        return select(q, null);
    }

    public SqlQueryResult select(CharSequence sql, Object... params) {
        return new SqlQueryResult(conn, sql.toString(), params);
    }

    public <T> SqlQueryResult select(Path sqlPath, Object params) {
        return SqlQuery.of(sqlPath).execute(conn, params);
    }

    private <T> PreparedStatement prepare(CharSequence sql, Object... params) {
        try {
            if (params != null && params.length == 1 && params[0] instanceof Collection) {
                params = ((Collection<?>) params[0]).toArray();
            }
            log.info("sql:", sql, params);
            var stmt = conn.prepareStatement(sql.toString());
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
            }
            return stmt;
        } catch (SQLException e) {
            throw Sys.asRuntimeException(e);
        }
    }

    public <T> SqlBuilder<T> from(Class<T> clazz, String alias) {
        return new SqlBuilder<T>(this, clazz, alias);
    }

    public <T> SqlBuilder<T> from(Class<T> clazz) {
        return from(clazz, null);
    }

    public <T> TypedSqlQueryResult<T> select(Class<T> clazz) {
        return from(clazz).execute();
    }

    public <T> TypedSqlQueryResult<T> select(Class<T> clazz, String where, Object... params) {
        if (where.matches("(?i)\\s*order\\s+by\\b.*")) {
            where = "1=1 " + where;
        }
        return from(clazz).where(where, params).execute();
    }

    public <T> T selectById(Class<T> clazz, Object... ids) {
        var table = mdm.getTableMeta(clazz);
        return from(clazz).where(table.idCondition, ids).first();
    }

    public <T> boolean refresh(T entity) {
        var table = mdm.getTableMeta(entity.getClass());
        String sql = "select * from " + table.name + " where " + table.idCondition;
        try (var rs = select(sql, table.getIdValues(entity));) {
            if (!rs.next()) {
                return false;
            }
            table.dataColumns.forEach(c -> {
                c.property.set(entity, rs.get(c.propertyType, c.propertyName));
            });
            return true;
        }
    }

    public int execute(CharSequence sql, Object... params) {
        try (var stmt = prepare(sql, params)) {
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw Sys.asRuntimeException(e);
        }
    }

    public int execute(ThrowingConsumer<SqlBuilder<?>> cn) {
        var sb = new SqlBuilder<Object>(this);
        cn.accept(sb);
        return execute(sb.toSQL(), sb.getParameters());
    }

    public int execute(Path sqlPath, Object params) {
        var sql = IO.load(sqlPath);
        var p = new SqlParser();
        int n = 0;
        for (var s : p.splitStatement(sql)) {
            var t = new SqlTemplate(s);
            var r = t.process(params);
            n += execute(r.sql, r.parameters);
        }
        return n;
    }

    public int execute(Path sqlPath) {
        return execute(sqlPath, null);
    }

    public int insert(Object entity) {
        var table = mdm.getTableMeta(entity.getClass());
        var columns = new ArrayList<String>();
        var params = new ArrayList<>();
        table.columns.forEach(column -> {
            var value = column.getValue(entity);
            if (value != null) {
                columns.add(column.name);
                params.add(value);
            }
        });
        if (columns.size() == 0)
            return 0;
        var sql = new StringBuilder();
        sql.append("insert into ");
        sql.append(table.name);
        sql.append(" (");
        sql.append(Sys.join(columns, ", ", it -> it));
        sql.append(") values (");
        sql.append(Sys.repeat("?", params.size(), ", "));
        sql.append(")");
        return execute(sql, params);
    }

    public int update(Object entity) {
        var table = mdm.getTableMeta(entity.getClass());
        var sql = new StringBuilder();
        sql.append("update ");
        sql.append(table.name);
        sql.append(" set ");
        sql.append(Sys.join(table.dataColumns, ", ", it -> it.name + " = ?"));
        sql.append(" where ");
        sql.append(table.idCondition);
        var params = new ArrayList<>();
        params.addAll(table.getDataValues(entity));
        params.addAll(table.getIdValues(entity));
        return execute(sql, params);
    }

    public int update(Class<?> clazz, Map<String, Object> values, String where, Object... params) {
        var table = mdm.getTableMeta(clazz);
        var allParams = new ArrayList<Object>();
        var set = Sys.join(values.keySet(), ", ", it -> it + " = ?");
        allParams.addAll(values.values());
        allParams.addAll(Arrays.asList(params));
        return execute("update " + table.name + " set " + set + " where " + where, allParams);
    }

    public int update(Class<?> clazz, ThrowingConsumer<Map<String, Object>> cn, String where, Object... params) {
        var values = new LinkedHashMap<String, Object>();
        cn.accept(values);
        return update(clazz, values, where, params);
    }

    public <T> int update(T entity, ThrowingConsumer<T> cn) {
        var table = mdm.getTableMeta(entity.getClass());
        var ids = table.getIdValues(entity);
        var before = table.toMap(entity);
        cn.accept(entity);
        var after = table.toMap(entity);
        var values = new LinkedHashMap<String, Object>();
        after.forEach((name, value) -> {
            if (!Sys.eq(value, before.get(name))) {
                values.put(name, value);
            }
        });
        if (values.isEmpty()) {
            return 0;
        }
        return update(table.clazz, values, table.idCondition, ids.toArray());
    }

    public int delete(Object entity) {
        var table = mdm.getTableMeta(entity.getClass());
        return delete(entity.getClass(), table.idCondition, table.getIdValues(entity));
    }

    public <T> int delete(Class<T> clazz, String where, Object... params) {
        var table = mdm.getTableMeta(clazz);
        return execute("delete from " + table.name + " where " + where, params);
    }

    public <T> int deleteById(Class<T> clazz, Object... ids) {
        var table = mdm.getTableMeta(clazz);
        return delete(clazz, table.idCondition, ids);
    }

    public void merge(Object entity) {

    }

    public void commit() {
        execute("commit");
    }

    public void rollback() {
        execute("rollback");
    }
}

class MetadataManager {
    private ConcurrentHashMap<String, TableMeta> tables = new ConcurrentHashMap<>();

    public TableMeta getTableMeta(Class<?> clazz) {
        return tables.computeIfAbsent(clazz.getName(), it -> new TableMeta(clazz));
    }
}
