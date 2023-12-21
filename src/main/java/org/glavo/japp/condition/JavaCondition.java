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

import org.glavo.japp.platform.JAppRuntimeContext;

import java.util.Map;
import java.util.StringJoiner;

public final class JavaCondition implements Condition {

    public static JavaCondition fromMap(Map<String, String> options) {
        String version = options.remove("version");
        String os = options.remove("os");
        String arch = options.remove("arch");
        String libc = options.remove("libc");

        if (!options.isEmpty()) {
            throw new IllegalArgumentException("Unknown options: " + options.keySet());
        }

        return new JavaCondition(
                version == null ? null : Integer.parseInt(version),
                MatchList.of(os), MatchList.of(arch), MatchList.of(libc)
        );
    }

    private final Integer version;
    private final MatchList os;
    private final MatchList arch;
    private final MatchList libc;

    private JavaCondition(Integer version, MatchList os, MatchList arch, MatchList libc) {
        this.version = version;
        this.os = os;
        this.arch = arch;
        this.libc = libc;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean test(JAppRuntimeContext context) {
        if (version != null && context.getJava().getVersion().major() < version) {
            return false;
        }

        if (os != null && !os.test(context.getJava().getOperatingSystem().getCheckedName())) {
            return false;
        }

        if (arch != null && !arch.test(context.getJava().getArchitecture().getCheckedName())) {
            return false;
        }

        if (libc != null && !libc.test(context.getJava().getLibC().toString())) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "java(", ")");
        if (version != null) {
            joiner.add("version=" + version);
        }
        if (os != null) {
            joiner.add("os=" + os);
        }
        if (arch != null) {
            joiner.add("arch=" + arch);
        }
        if (libc != null) {
            joiner.add("libc=" + libc);
        }
        return joiner.toString();
    }
}
