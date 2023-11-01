package org.glavo.japp.launcher.condition;

public class ConditionParser {
    private final String source;
    private final int end;
    private int index;

    private Token nextToken;

    private int status;

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
            index++;
            nextToken = Token.LEFT;
        } else if (ch == ')') {
            index++;
            nextToken = Token.RIGHT;
        } else if (ch == ':') {
            index++;
            nextToken = Token.COLON;
        } else if (ch == ',') {
            index++;
            nextToken = Token.COMMA;
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

            index = endIndex + 1;
            nextToken = new Token(source.substring(index + 1, endIndex));
        } else {
            throw new IllegalArgumentException("Invalid condition: " + source);
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


    public ConditionParser(String source) {
        this.source = source;
        this.end = source.length();
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
