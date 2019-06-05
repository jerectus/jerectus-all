package jerectus.sql.parser;

import jerectus.text.PatternTokenizer;

public class SqlToken extends PatternTokenizer.Token {
    public String frontSpace;

    public SqlToken(String type, String value, String frontSpace) {
        super(type, value);
        this.frontSpace = frontSpace;
    }

    public String getContent() {
        if (is("comment")) {
            return value.substring(2, value.length() - 2);
        } else if (is("comment1")) {
            return value.substring(2);
        }
        return value;
    }

    @Override
    public String toString() {
        return frontSpace + value;
    }
}
