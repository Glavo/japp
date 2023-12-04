/*
 * Copyright 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.japp.util;

public final class ToStringBuilder {
    private final StringBuilder builder = new StringBuilder();
    private final String right;

    private boolean first = true;

    public ToStringBuilder(Object prefix, String left, String right) {
        this.right = right;
        builder.append(prefix).append(left);
    }

    public ToStringBuilder append(String name, Object value) {
        if (first) {
            first = false;
        } else {
            builder.append(", ");
        }
        builder.append(name).append('=').append(value);
        return this;
    }

    public ToStringBuilder appendIfNotNull(String name, Object value) {
        if (value != null) {
            append(name, value);
        }
        return this;
    }

    public String build() {
        return builder.append(right).toString();
    }
}
