package jerectus.sql.parser;

import org.junit.Assert;
import org.junit.Test;

public class SqlTokenizerTest {
    @Test
    public void test1() {
        var tz = new SqlTokenizer();
        var it = tz.splitStatement("select 1 from dual; ; select 'A;' from dual").iterator();
        Assert.assertEquals("select 1 from dual", it.next());
        Assert.assertEquals("select 'A;' from dual", it.next());
        Assert.assertFalse(it.hasNext());
    }
}