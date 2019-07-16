package jerectus.net.http.test1;

import jerectus.util.logging.Logger;
import jerectus.validation.Valid;

public class Index {
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(Index.class);

    @Valid(required = true, size = "3..10")
    public String name = "Abe";

    @Valid(required = true, expression = "validate1()")
    public int num = 1;

    public String validate1() {
        return num == 2 ? "数が2です" : "";
    }

    public void plus() {
        num++;
    }

    public void minus() {
        num--;
    }
}
