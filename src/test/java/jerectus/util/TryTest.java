package jerectus.util;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

public class TryTest {
    @Test
    public void testFunction() {
        new Object() {
            double fn(int a) throws Exception {
                return a * a;
            }

            double fail(int a) throws Exception {
                throw new Exception("fn1f");
            }

            String apply(Function<Integer, Double> fn, int a) {
                return "" + fn.apply(a);
            }

            {
                Assert.assertEquals("4.0", apply(Try.to(this::fn), 2));
                Assert.assertEquals("9.9", apply(Try.to(this::fail, a -> e -> 9.9), 2));
            }
        };
    }

    @Test
    public void testBiFunction() {
        new Object() {
            double fn(int a, long b) throws Exception {
                return a * b;
            }

            double fail(int a, long b) throws Exception {
                throw new Exception("fn2f");
            }

            String apply(BiFunction<Integer, Long, Double> fn, int a, long b) {
                return "" + fn.apply(a, b);
            }

            {
                Assert.assertEquals("6.0", apply(Try.to(this::fn), 2, 3));
                Assert.assertEquals("9.9", apply(Try.to(this::fail, (a, b) -> e -> 9.9), 2, 3));
                try {
                    Assert.assertEquals("***", apply(Try.to(this::fail), 2, 3));
                } catch (RuntimeException e) {
                    Assert.assertEquals("fn2f", e.getCause().getMessage());
                }
            }
        };
    }
}
