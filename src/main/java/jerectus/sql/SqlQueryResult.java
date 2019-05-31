package jerectus.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jerectus.sql.internal.Classes;
import jerectus.util.Sys;
import jerectus.util.function.ThrowingConsumer;
import jerectus.util.logging.Logger;

public class SqlQueryResult implements AutoCloseable {
    private static final Logger log = Logger.getLogger(SqlQueryResult.class);
    private static final ObjectMapper om = new ObjectMapper();
    static {
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    private Connection conn;
    private String sql;
    private List<Object> params;
    private int offset_;
    private int limit_;
    private int count_;
    private PreparedStatement stmt;
    private ResultSet rs;
    private Map<String, Object> resultMap;

    public SqlQueryResult(Connection conn, String sql, Object... params) {
        this.conn = conn;
        this.sql = sql;
        if (params != null && params.length == 1 && params[0] instanceof Collection) {
            Collection<Object> c = Sys.cast(params[0]);
            this.params = new ArrayList<>(c);
        } else {
            this.params = new ArrayList<>();
            Sys.addAll(this.params, params);
        }
    }

    public SqlQueryResult(ResultSet rs) {
        this.rs = rs;
    }

    public <T> TypedSqlQueryResult<T> as(Class<T> clazz) {
        return new TypedSqlQueryResult<T>(clazz, this);
    }

    @Override
    public void close() {
        try {
            if (rs != null) {
                rs.close();
                rs = null;
            }
            if (stmt != null) {
                stmt.close();
                stmt = null;
            }
        } catch (SQLException e) {
            throw Sys.asRuntimeException(e);
        }
    }

    public SqlQueryResult limit(int offset, int limitRows) {
        offset_ = offset;
        limit_ = limitRows;
        return this;
    }

    private void execute() throws SQLException {
        log.info("sql:", sql, params);
        stmt = conn.prepareStatement(sql);
        if (params != null && params.size() > 0) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
        }
        rs = stmt.executeQuery();
    }

    public boolean next() {
        resultMap = null;
        if (limit_ > 0 && count_ >= limit_) {
            return false;
        }
        count_++;
        try {
            if (rs == null) {
                execute();
                if (offset_ > 0) {
                    return rs.absolute(offset_ + 1);
                }
            }
            return rs.next();
        } catch (SQLException e) {
            throw Sys.asRuntimeException(e);
        }
    }

    public Map<String, Object> get() {
        if (resultMap != null)
            return resultMap;
        try {
            var meta = rs.getMetaData();
            var tpl = new LinkedHashMap<String, Object>();
            var ent = tpl;
            var entName = (String) null;
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                var colName = meta.getColumnName(i);
                if (colName.equals("@")) {
                    entName = null;
                    ent = tpl;
                } else if (colName.startsWith("@")) {
                    entName = colName.substring(1);
                    ent = Sys.cast(tpl.computeIfAbsent(entName, _0 -> new LinkedHashMap<String, Object>()));
                } else if (ent != null) {
                    Object value = rs.getObject(i);
                    if (ent != tpl && ent.isEmpty() && value == null) {
                        tpl.put(entName, null);
                        ent = null;
                    } else {
                        ent.put(Sys.camelCase(colName), value);
                    }
                }
            }
            resultMap = tpl;
            return tpl;
        } catch (SQLException e) {
            throw Sys.asRuntimeException(e);
        }
    }

    public <T> T get(Class<T> type, String name) {
        return om.convertValue(name == null ? get() : get().get(name), type);
    }

    public <T> T get(Class<T> type) {
        var result = get();
        var name = Sys.uncapitalize(Classes.getSimpleName(type));
        return om.convertValue(result.containsKey(name) ? result.get(name) : result, type);
    }

    public void forEach(ThrowingConsumer<SqlQueryResult> fn) {
        try {
            while (next()) {
                fn.accept(this);
            }
        } finally {
            close();
        }
    }

    public Map<String, Object> first() {
        try {
            return next() ? get() : null;
        } finally {
            close();
        }
    }

    public <T> T first(Class<T> type) {
        try {
            return next() ? get(type, null) : null;
        } finally {
            close();
        }
    }

    public <T> List<T> toList(Class<T> type) {
        List<T> result = new ArrayList<>();
        forEach(rs -> result.add(rs.get(type, null)));
        return result;
    }
}
