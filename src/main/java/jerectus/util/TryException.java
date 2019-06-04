package jerectus.util;

public class TryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private Object[] params;

    public TryException(Throwable t, Object... params) {
        super(t);
        this.params = params;
    }

    public Object[] getParameters() {
        return params;
    }
}