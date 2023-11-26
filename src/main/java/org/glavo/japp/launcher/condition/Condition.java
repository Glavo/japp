package org.glavo.japp.launcher.condition;

import org.glavo.japp.launcher.platform.JAppRuntimeContext;

import java.util.function.Predicate;

@FunctionalInterface
public interface Condition extends Predicate<JAppRuntimeContext> {
    boolean test(JAppRuntimeContext context);
}
