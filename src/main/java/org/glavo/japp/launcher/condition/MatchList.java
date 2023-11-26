package org.glavo.japp.launcher.condition;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public final class MatchList implements Predicate<String> {

    public static MatchList of(String list) {
        if (list == null) {
            return null;
        }

        boolean negative;
        if (list.startsWith("!")) {
            list = list.substring(1);
            negative = true;
        } else {
            negative = false;
        }

        return new MatchList(negative, Arrays.asList(list.split("\\|")));
    }

    private final boolean negative;
    private final List<String> values;

    public MatchList(boolean negative, List<String> values) {
        this.negative = negative;
        this.values = values;
    }

    @Override
    public boolean test(String s) {
        return negative != values.contains(s);
    }

    @Override
    public String toString() {
        return (negative ? "!" : "") + values;
    }
}
