package jerectus.sql;

import java.util.List;

import jerectus.util.function.ThrowingBiConsumer;
import jerectus.util.function.ThrowingConsumer;

public class TypedSqlQueryResult<T> implements AutoCloseable {
    private Class<T> resultType;
    private SqlQueryResult sqlQueryResult;

    TypedSqlQueryResult(Class<T> resultType, SqlQueryResult sqlQueryResult) {
        this.resultType = resultType;
        this.sqlQueryResult = sqlQueryResult;
    }

    @Override
    public void close() {
        sqlQueryResult.close();
    }

    public TypedSqlQueryResult<T> limit(int offset, int limitRows) {
        sqlQueryResult.limit(offset, limitRows);
        return this;
    }

    public void forEach(ThrowingConsumer<T> fn) {
        sqlQueryResult.forEach(rs -> {
            fn.accept(rs.get(resultType, null));
        });
    }

    public void forEach(ThrowingBiConsumer<T, SqlQueryResult> fn) {
        sqlQueryResult.forEach(rs -> {
            fn.accept(rs.get(resultType, null), rs);
        });
    }

    public T first() {
        return sqlQueryResult.first(resultType);
    }

    public List<T> toList() {
        return sqlQueryResult.toList(resultType);
    }
}
