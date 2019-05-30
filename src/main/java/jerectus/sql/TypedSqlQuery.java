package jerectus.sql;

import java.sql.Connection;
import java.util.function.Supplier;

public class TypedSqlQuery<T> {
    private SqlQuery sqlQuery;
    private Class<T> resultType;

    TypedSqlQuery(Class<T> type, SqlQuery query) {
        resultType = type;
        sqlQuery = query;
    }

    TypedSqlQuery(Class<T> type, Supplier<String> fn) {
        this(type, new SqlQuery(fn));
    }

    public TypedSqlQueryResult<T> execute(Connection conn, Object param) {
        return new TypedSqlQueryResult<>(resultType, sqlQuery.execute(conn, param));
    }
}
