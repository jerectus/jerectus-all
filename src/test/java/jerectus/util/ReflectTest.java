package jerectus.util;

import org.junit.Test;

public class ReflectTest {
    @Test
    public void test1() {
        var a = new A();
        Reflect.invoke(a, "hello", 1);
    }

    public static class A {
        public void hello(int n) {
            System.out.println("n=" + n);
        }
    }
}