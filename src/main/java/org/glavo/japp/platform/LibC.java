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
package org.glavo.japp.platform;

import java.util.Locale;

public enum LibC {
    DEFAULT, MUSL;

    public static LibC parseLibC(String value) {
        switch (value) {
            case "":
            case "default":
            case "gnu":
                return DEFAULT;
            case "musl":
                return MUSL;
            default:
                throw new IllegalArgumentException(value);
        }
    }

    private final String checkedName = this.name().toLowerCase(Locale.ROOT);

    public String getCheckedName() {
        return checkedName;
    }

    @Override
    public String toString() {
        return getCheckedName();
    }
}
