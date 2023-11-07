package org.glavo.japp.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface Visibility {
    enum Context {
        BOOT,
        LAUNCHER,
        PACKER
    }

    Context[] value();
}
