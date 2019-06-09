package jerectus.sql;

import java.time.OffsetDateTime;

import org.junit.Test;

import jerectus.util.Sys;

public class DbTest {
    @Test
    public void test1() {
        try (var db = Db.open();) {
            db.from(User.class).with(q -> {
                q.where("name like '%a'");
                q.orderBy("name desc");
            }).forEach(user -> {
                System.out.println(Sys.populate(user));
            });
        }
    }

    @Test
    public void test2() {
        var db = Db.open();
        var q = SqlQuery.fromResource(this, "test2").as(User.class);
        try (db) {
            db.select(q).forEach(user -> {
                System.out.println(Sys.populate(user));
            });
        }
    }

    public static class User {
        public Long id;
        public String name;
        public OffsetDateTime createDate;
    }
}