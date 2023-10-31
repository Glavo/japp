package org.glavo.japp.launcher.condition;

import java.util.function.Predicate;

public interface Condition extends Predicate<JAppRuntimeContext> {
    String type();

    boolean test(JAppRuntimeContext context);
}
