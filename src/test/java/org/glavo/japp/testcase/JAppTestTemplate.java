/*
 * Copyright (C) 2024 Glavo
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

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.stream.Stream;

public interface JAppTestTemplate {
    Stream<TestArgument> tests();

    default TestArgument newTest(String name, List<String> argument, List<String> lines) {
        return new TestArgument(name, argument, lines);
    }

    @TestFactory
    default Stream<DynamicTest> testFactory() {
        return tests().map(argument -> DynamicTest.dynamicTest(argument.name, () -> {
            try (JAppTestHelper.FileHolder holder = JAppTestHelper.create(argument.argument.toArray(String[]::new))) {
                JAppTestHelper.assertLines(JAppTestHelper.launch(holder.file), argument.lines.toArray(String[]::new));
            }
        }));
    }

    class TestArgument {
        public final String name;
        public final List<String> argument;
        public final List<String> lines;

        TestArgument(String name, List<String> argument, List<String> lines) {
            this.name = name;
            this.argument = argument;
            this.lines = lines;
        }
    }
}
