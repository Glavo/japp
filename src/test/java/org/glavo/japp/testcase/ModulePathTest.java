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
package org.glavo.japp.testcase;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.glavo.japp.testcase.JAppTestHelper.assertLines;

public final class ModulePathTest {
    @Test
    public void test() throws IOException {
        try (JAppTestHelper.FileHolder holder = JAppTestHelper.create(
                "--module-path", System.getProperty("japp.testcase.modulepath"),
                "org.glavo.japp.testcase.modulepath.ModulePath"
        )) {

            assertLines(JAppTestHelper.launch(holder.file),
                    "japp:/modules/org.glavo.japp.testcase.modulepath/org/glavo/japp/testcase/modulepath/ModulePath.class",
                    "japp:/modules/com.google.gson/com/google/gson/Gson.class",
                    "japp:/modules/org.apache.commons.lang3/org/apache/commons/lang3/ObjectUtils.class"
            );
        }
    }
}
