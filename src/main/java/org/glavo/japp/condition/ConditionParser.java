/*
 * Copyright (C) 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.japp.condition;

import java.util.*;
import java.util.function.Function;

public class ConditionParser {

    private static final Map<String, Function<Map<String, String>, ? extends Condition>> factories =
            Collections.singletonMap("java", JavaCondition::fromMap);

    public static Condition parse(String str) {
        ConditionParser parser = new ConditionParser(str);
        Condition condition = parser.expr1();

        if (parser.peek() != null) {
            throw new IllegalArgumentException(str);
        }

        return condition;
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

    private boolean inCondition = false;

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
            case '[':
            case ']':
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

        if (inCondition ? isLiteralPart(ch) : isLiteralStar(ch)) {
            StringBuilder builder = new StringBuilder();

            builder.append(ch);
            index++;

            while (index < end && isLiteralPart(ch = source.charAt(index))) {
                builder.append(ch);
                index++;
            }
            nextToken = new Token(builder.toString());
        } else if (ch == '(') {
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

    private IllegalArgumentException fail() {
        return new IllegalArgumentException(source);
    }

    private Condition expr1() {
        Condition left = expr2();
        if (peek() != Token.OR) {
            return left;
        }

        ArrayList<Condition> conditions = new ArrayList<>(2);
        conditions.add(left);
        while (peek() == Token.OR) {
            next();
            conditions.add(expr2());
        }

        return new OrCondition(conditions);
    }

    private Condition expr2() {
        Condition left = term();
        if (peek() != Token.AND) {
            return left;
        }

        ArrayList<Condition> conditions = new ArrayList<>(2);
        conditions.add(left);
        while (peek() == Token.AND) {
            next();
            conditions.add(term());
        }

        return new AndCondition(conditions);
    }

    private Condition term() {
        Token token = next();
        if (token == Token.LEFT) {
            Condition condition = expr1();

            if (condition == null || next() != Token.RIGHT) {
                throw fail();
            }

            return condition;
        } else if (token == Token.NOT) {
            Condition condition = expr1();
            if (condition == null) {
                throw fail();
            }
            return new NotCondition(condition);
        } else if (token.isLiteral) {
            if (next() != Token.LEFT) {
                throw fail();
            }

            inCondition = true;

            Function<Map<String, String>, ? extends Condition> factory = factories.get(token.value);
            if (factory == null) {
                throw fail();
            }

            Map<String, String> map = new HashMap<>();

            boolean first = true;
            while (peek() != Token.RIGHT) {
                if (!first && next() != Token.COMMA) {
                    throw fail();
                }
                first = false;

                Token key = next();
                Token sep = next();
                Token value = next();

                if (!key.isLiteral || sep != Token.COLON || !value.isLiteral) {
                    throw fail();
                }

                if (map.put(key.value, value.value) != null) {
                    throw fail();
                }
            }
            next();
            inCondition = false;
            return factory.apply(map);
        } else {
            throw fail();
        }
    }

    public static void main(String[] args) {
        System.out.println(parse(args[0]));
    }
}
