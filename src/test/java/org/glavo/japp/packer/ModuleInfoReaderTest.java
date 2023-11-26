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
