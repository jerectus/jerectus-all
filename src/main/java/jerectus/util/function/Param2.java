package jerectus.util.function;

public class Param2<T1, T2> {
    private T1 param1;
    private T2 param2;

    public Param2(T1 param1, T2 param2) {
        this.param1 = param1;
        this.param2 = param2;
    }

    public T1 getParam1() {
        return param1;
    }

    public T2 getParam2() {
        return param2;
    }
}
