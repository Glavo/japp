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
package org.glavo.japp.packer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ModuleInfoReaderTest {

    @Test
    public void deriveAutomaticModuleNameTest() {
        assertEquals("a", ModuleInfoReader.deriveAutomaticModuleName("a.jar"));
        assertEquals("a", ModuleInfoReader.deriveAutomaticModuleName("a-0.1.0.jar"));
        assertEquals("a", ModuleInfoReader.deriveAutomaticModuleName("...a-0.1.0.jar"));
        assertEquals("a", ModuleInfoReader.deriveAutomaticModuleName("...a...-0.1.0.jar"));
        assertEquals("a.b", ModuleInfoReader.deriveAutomaticModuleName("a-b-0.1.0.jar"));
        assertEquals("a.b", ModuleInfoReader.deriveAutomaticModuleName("a--b-0.1.0.jar"));
    }
}
