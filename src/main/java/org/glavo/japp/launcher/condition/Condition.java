package org.glavo.japp.launcher.condition;

import java.util.function.Predicate;

@FunctionalInterface
public interface Condition extends Predicate<JAppRuntimeContext> {
    boolean test(JAppRuntimeContext context);
}
