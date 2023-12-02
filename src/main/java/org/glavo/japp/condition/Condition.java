package org.glavo.japp.condition;

import org.glavo.japp.platform.JAppRuntimeContext;

import java.util.function.Predicate;

@FunctionalInterface
public interface Condition extends Predicate<JAppRuntimeContext> {
    boolean test(JAppRuntimeContext context);
}
