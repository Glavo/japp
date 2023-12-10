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
package org.glavo.japp.packer.processor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PathListParserTest {
    static final String PS = File.pathSeparator;

    private record Pair(Map<String, String> options, String path) {
    }

    private static List<Pair> parse(String list) {
        List<Pair> res = new ArrayList<>();
        PathListParser parser = new PathListParser(list);
        while (parser.scanNext()) {
            res.add(new Pair(parser.options, parser.path));
        }
        return res;
    }

    @Test
    public void test() {
        Assertions.assertEquals(List.of(), parse(""));
        Assertions.assertEquals(List.of(), parse(PS));
        Assertions.assertEquals(List.of(), parse(PS + PS));
        Assertions.assertEquals(
                List.of(new Pair(Map.of(), "A"), new Pair(Map.of(), "B"), new Pair(Map.of(), "C")),
                parse(String.join(PS, "A", "B", "", "C", ""))
        );
        Assertions.assertEquals(
                List.of(new Pair(Map.of(), "A"),
                        new Pair(Map.of("type", "maven", "repo", "local"), "B"),
                        new Pair(Map.of("type", "local"), "C")),
                parse(String.join(PS, "[]A", "[type=maven,repo=local]B", "[type='local']C", ""))
        );

    }
}
