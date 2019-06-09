package jerectus.sql;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.function.Supplier;

import jerectus.sql.template.SqlTemplate;
import jerectus.util.Resources;

public class SqlQuery {
    private SqlTemplate template;

    public SqlQuery(SqlTemplate template) {
        this.template = template;
    }

    public static SqlQuery of(Supplier<String> fn) {
        return new SqlQuery(new SqlTemplate(fn));
    }

    public static SqlQuery of(String sql) {
        return new SqlQuery(new SqlTemplate(sql));
    }

    public static SqlQuery of(Path sqlPath) {
        return new SqlQuery(new SqlTemplate(sqlPath));
    }

    public static SqlQuery fromResource(Object o, String name) {
        return of(Resources.getMember(o, name + ".sql"));
    }

    public <T> TypedSqlQuery<T> as(Class<T> type) {
        return new TypedSqlQuery<T>(type, this);
    }

    public SqlQueryResult execute(Connection conn, Object context) {
        var result = template.process(context);
        return new SqlQueryResult(conn, result.sql, result.parameters);
    }
}
