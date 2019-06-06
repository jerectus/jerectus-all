package jerectus.sql.parser;

import java.util.ArrayList;
import java.util.List;

import jerectus.text.PatternTokenizer;
import jerectus.util.Sys;

public class SqlTokenizer {
    private static final PatternTokenizer lexer;
    static {
        var b = PatternTokenizer.builder();
        b.pattern("string", "'([^']|'')*'");
        b.pattern("label", "\".*?\"");
        b.pattern("comment", "/\\*.*?\\*/");
        b.pattern("comment1", "\\-\\-[^\\r\\n]*");
        b.pattern("number", "\\d+(\\.\\d+)?|\\.\\d+");
        b.pattern("id", "[a-zA-Z_]\\w*");
        b.pattern("format", "\\+|\\-|\\*\\*?|/|=|!=|<[=>]?|>=?|\\|\\|");
        b.pattern("symbol", "[,\\(\\);:\\?\\.\\[\\]\\|]");
        b.pattern("newline", "\\R");
        b.pattern("space", "\\s+");
        lexer = b.build();
    }

    public List<SqlToken> parse1(String sql) {
        List<SqlToken> result = new ArrayList<>();
        String[] space = { "" };
        lexer.parse(sql).forEach(token -> {
            if (token.is("space")) {
                space[0] = token.value;
            } else {
                result.add(new SqlToken(token.type, token.value, space[0]));
                space[0] = "";
            }
        });
        result.add(new SqlToken("", "", ""));
        return result;
    }

    public Iterable<SqlToken> parse(String sql) {
        var it = lexer.parse(sql).iterator();
        return Sys.iterable(g -> {
            if (!it.hasNext())
                return;
            String space = "";
            do {
                var token = it.next();
                if (!token.is("space")) {
                    g.yield(new SqlToken(token.type, token.value, space));
                    return;
                }
                space += token.value;
            } while (it.hasNext());
            g.yield(new SqlToken("end", "", space));
        });
    }

    public List<String> splitStatement(String sql) {
        var result = new ArrayList<String>();
        var sb = new StringBuilder();
        for (var token : parse(sql + ";")) {
            if (token.matches(";")) {
                var s = sb.toString().trim();
                if (!s.isEmpty()) {
                    result.add(s);
                }
                sb.setLength(0);
            } else {
                sb.append(token);
            }
        }
        return result;
    }
}
