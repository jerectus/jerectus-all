package jerectus.sql.parser;

import java.util.ArrayList;
import java.util.List;

import jerectus.sql.internal.Cursor;
import jerectus.text.PatternTokenizer;

public class SqlParser {
    public static class Token extends PatternTokenizer.Token {
        public String frontSpace;

        public Token(String type, String value, String frontSpace) {
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

    private PatternTokenizer lexer = new PatternTokenizer();
    {
        lexer.addTokenPattern("string", "'([^']|'')*'");
        lexer.addTokenPattern("label", "\".*?\"");
        lexer.addTokenPattern("comment", "/\\*.*?\\*/");
        lexer.addTokenPattern("comment1", "\\-\\-[^\\r\\n]*");
        lexer.addTokenPattern("number", "\\d+(\\.\\d+)?|\\.\\d+");
        lexer.addTokenPattern("id", "[a-zA-Z_]\\w*");
        lexer.addTokenPattern("format", "\\+|\\-|\\*\\*?|/|=|!=|<[=>]?|>=?|\\|\\|");
        lexer.addTokenPattern("symbol", "[,\\(\\);:\\?\\.\\[\\]\\|]");
        lexer.addTokenPattern("newline", "\\R");
        lexer.addTokenPattern("space", "\\s+");
    }

    public Cursor<Token> parse(String sql) {
        List<Token> result = new ArrayList<>();
        String[] space = { "" };
        lexer.parse(sql, token -> {
            if (token.type.equals("space")) {
                space[0] = token.value;
            } else {
                result.add(new Token(token.type, token.value, space[0]));
                space[0] = "";
            }
        });
        result.add(new Token("", "", ""));
        return Cursor.of(result, -1);
    }

    public List<String> splitStatement(String sql) {
        var result = new ArrayList<String>();
        var c = parse(sql + ";");
        var sb = new StringBuilder();
        while (c.moveNext()) {
            if (c.get().matches(";")) {
                var s = sb.toString().trim();
                if (!s.isEmpty()) {
                    result.add(s);
                }
                sb.setLength(0);
            } else {
                sb.append(c.get());
            }
        }
        return result;
    }

}
