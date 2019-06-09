package jerectus.sql;

import java.util.List;

import jerectus.util.function.TryBiConsumer;
import jerectus.util.function.TryConsumer;

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

    public void forEach(TryConsumer<T> fn) {
        sqlQueryResult.forEach(resultType, fn);
    }

    public void forEach(TryBiConsumer<T, SqlQueryResult> fn) {
        sqlQueryResult.forEach(resultType, fn);
    }

    public T first() {
        return sqlQueryResult.first(resultType);
    }

    public List<T> toList() {
        return sqlQueryResult.toList(resultType);
    }
}
