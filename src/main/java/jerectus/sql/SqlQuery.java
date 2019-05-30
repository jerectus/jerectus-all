package jerectus.sql;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.function.Supplier;

import jerectus.io.IO;
import jerectus.sql.internal.SqlFile;
import jerectus.sql.template.SqlTemplate;

public class SqlQuery {
    private SqlTemplate template;

    public SqlQuery(Supplier<String> fn) {
        template = new SqlTemplate(fn);
    }

    public static SqlQuery of(Supplier<String> fn) {
        return new SqlQuery(fn);
    }

    public static SqlQuery of(String sql) {
        return new SqlQuery(() -> sql);
    }

    public static SqlQuery of(Path sqlPath) {
        return new SqlQuery(() -> IO.load(sqlPath));
    }

    public static SqlQuery fromResource(Object o, String name) {
        return of(SqlFile.get(o, name));
    }

    public <T> TypedSqlQuery<T> as(Class<T> type) {
        return new TypedSqlQuery<T>(type, this);
    }

    public SqlQueryResult execute(Connection conn, Object context) {
        var result = template.process(context);
        return new SqlQueryResult(conn, result.sql, result.parameters);
    }
}
