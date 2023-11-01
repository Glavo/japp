package org.glavo.japp.launcher.condition;

import java.util.ArrayList;
import java.util.List;

public class ConditionParser {
    private final String source;
    private final int end;
    private int index;

    private Token nextToken;

    private int status;

    public ConditionParser(String source) {
        this.source = source;
        this.end = source.length();
    }

    private static boolean isCommonChar(char ch) {
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
        } else if (isCommonChar(ch)) {
            StringBuilder builder = new StringBuilder();
            while (index < end && isCommonChar(ch = source.charAt(index))) {
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

    public static final class Token {
        static final Token AND = new Token("&&");
        static final Token OR = new Token("||");
        static final Token NOT = new Token("!");
        static final Token LEFT = new Token("(");
        static final Token RIGHT = new Token(")");
        static final Token COLON = new Token(":");
        static final Token COMMA = new Token(",");

        final String value;

        private Token(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "Token[" + value + "]";
        }
    }
}
