package jerectus.util;

import org.junit.Test;

public class TryTest {
    @Test
    public void test1() {
        System.out.println(Try.get(() -> this.getClass().getMethod("test1")));
    }
}
