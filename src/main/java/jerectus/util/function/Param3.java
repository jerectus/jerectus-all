package jerectus.util.function;

public class Param3<T1, T2, T3> extends Param2<T1, T2> {
    private T3 param3;

    public Param3(T1 param1, T2 param2, T3 param3) {
        super(param1, param2);
        this.param3 = param3;
    }

    public T3 getParam3() {
        return param3;
    }
}