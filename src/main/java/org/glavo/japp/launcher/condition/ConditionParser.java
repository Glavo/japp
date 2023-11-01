package org.glavo.japp.launcher.condition;

public class ConditionParser {
    private final String source;
    private final int end;
    private int index;

    public ConditionParser(String source, int end) {
        this.source = source;
        this.end = end;
    }

    private static final class Token {
        static final Token AND = new Token("&&");
        static final Token OR = new Token("||");
        static final Token NOT = new Token("!");
        static final Token LEFT = new Token("(");
        static final Token RIGHT = new Token(")");
        static final Token COLON = new Token(":");

        final String value;

        private Token(String value) {
            this.value = value;
        }
    }
}
