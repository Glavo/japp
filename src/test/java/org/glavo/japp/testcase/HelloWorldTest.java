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

public final class HelloWorldTest {
    private void test(boolean useModulePath) throws IOException {
        try (JAppTestHelper.FileHolder holder = JAppTestHelper.create(
                useModulePath ? "--module-path" : "--classpath", System.getProperty("japp.testcase.helloworld"),
                "org.glavo.japp.testcase.helloworld.HelloWorld"
        )) {
            assertLines(JAppTestHelper.launch(holder.file), "Hello World!");
        }
    }

    @Test
    public void test() throws IOException {
        test(true);
        test(false);
    }
}
