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

import java.util.List;
import java.util.stream.Stream;

public final class HelloWorldTest implements JAppTestTemplate {
    public static final String FILE = JAppTestHelper.getTestCase("helloworld");
    public static final String MAIN_CLASS = "org.glavo.japp.testcase.helloworld.HelloWorld";

    @Override
    public Stream<TestArgument> tests() {
        return Stream.of(
                newTest("module path", List.of("--module-path", FILE, MAIN_CLASS), List.of("Hello World!")),
                newTest("classpath", List.of("--classpath", FILE, MAIN_CLASS), List.of("Hello World!")),
                newTest("module path with end zip", List.of("-Tappend-boot-jar", "--module-path", FILE, MAIN_CLASS), List.of("Hello World!"))
        );
    }
}
