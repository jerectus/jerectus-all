package jerectus.util;

import java.util.Arrays;

import org.junit.Test;

public class TryTest {
    @Test
    public void test1() {
        Arrays.asList(1, 2, 3).forEach(Try.with(it -> {
            TryTest.class.getMethod("test1");
        }));
    }
}
