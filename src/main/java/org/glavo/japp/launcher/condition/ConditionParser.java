package org.glavo.japp.launcher.condition;

import org.glavo.japp.TODO;

import java.util.ArrayList;
import java.util.List;

public class ConditionParser {

    public static Condition parse(String str) {
        ConditionParser parser = new ConditionParser(str);
        if (parser.peek() == null) {
            throw new IllegalArgumentException("Condition cannot be empty");
        }

        throw new TODO();
    }

    public static final class Token {
        static final Token AND = new Token("&&", 20);
        static final Token OR = new Token("||", 10);
        static final Token NOT = new Token("!", 30);
        static final Token LEFT = new Token("(", -1);
        static final Token RIGHT = new Token(")", -1);
        static final Token COLON = new Token(":", -1);
        static final Token COMMA = new Token(",", -1);

        final String value;
        final int precedence;
        final boolean isLiteral;

        private Token(String value) {
            this.value = value;
            this.precedence = -1;
            this.isLiteral = true;
        }

        private Token(String value, int precedence) {
            this.value = value;
            this.precedence = precedence;
            this.isLiteral = false;
        }

        @Override
        public String toString() {
            return isLiteral ? "Literal[" + value + "]" : value;
        }
    }

    private final String source;
    private final int end;
    private int index;

    private Token nextToken;

    public ConditionParser(String source) {
        this.source = source;
        this.end = source.length();
    }

    private static boolean isLiteralStar(char ch) {
        switch (ch) {
            case '*':
            case '^':
            case '.':
            case '<':
            case '>':
            case '-':
            case '/':
            case '#':
            case '@':
            case '~':
                return true;
            default:
                return Character.isJavaIdentifierPart(ch);
        }
    }

    private static boolean isLiteralPart(char ch) {
        return ch == '|' || ch == '&' || ch == '!' || isLiteralStar(ch);
    }

    private void scanToken() {
        if (nextToken != null || index >= end) {
            return;
        }

        char ch = 0;
        while (index < end && Character.isWhitespace(ch = source.charAt(index))) {
            index++;
        }
        if (index == end) {
            return;
        }

        if (ch == '(') {
            nextToken = Token.LEFT;
            index++;
        } else if (ch == ')') {
            nextToken = Token.RIGHT;
            index++;
        } else if (ch == ':') {
            nextToken = Token.COLON;
            index++;
        } else if (ch == ',') {
            nextToken = Token.COMMA;
            index++;
        } else if (ch == '!') {
            nextToken = Token.NOT;
            index++;
        } else if (ch == '&') {
            if (index == end - 1 || source.charAt(index + 1) != '&') {
                throw new IllegalArgumentException("Unknown token: " + index + ":" + index);
            }
            nextToken = Token.AND;
            index += 2;
        } else if (ch == '|') {
            if (index == end - 1 || source.charAt(index + 1) != '|') {
                throw new IllegalArgumentException("Unknown token: " + index + ":" + index);
            }
            nextToken = Token.OR;
            index += 2;
        } else if (isLiteralStar(ch)) {
            StringBuilder builder = new StringBuilder();

            builder.append(ch);
            index++;

            while (index < end && isLiteralPart(ch = source.charAt(index))) {
                builder.append(ch);
                index++;
            }
            nextToken = new Token(builder.toString());
        } else if (ch == '\'') {
            int endIndex = source.indexOf('\'', index + 1);
            if (endIndex < 0) {
                throw new IllegalArgumentException();
            }

            nextToken = new Token(source.substring(index + 1, endIndex));
            index = endIndex + 1;
        } else {
            throw new IllegalArgumentException("Unknown token: " + index + ":" + index);
        }
    }

    public Token peek() {
        scanToken();
        return nextToken;
    }

    public Token next() {
        scanToken();
        Token token = nextToken;
        nextToken = null;
        return token;
    }

    public List<Token> tokens() {
        List<Token> tokens = new ArrayList<>();
        Token token;
        while ((token = next()) != null) {
            tokens.add(token);
        }
        return tokens;
    }
}
