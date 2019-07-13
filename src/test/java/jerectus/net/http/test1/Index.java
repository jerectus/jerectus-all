package jerectus.net.http.test1;

import jerectus.util.logging.Logger;
import jerectus.validation.Valid;

public class Index {
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(Index.class);

    @Valid(required = true, minLength = 3, maxLength = 10)
    public String name = "Abe";

    @Valid(required = true, expression = "validate1()")
    public int num = 1;

    public boolean validate1() {
        return num != 2;
    }

    public void plus() {
        num++;
    }

    public void minus() {
        num--;
    }
}
