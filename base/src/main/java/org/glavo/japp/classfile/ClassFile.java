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
package org.glavo.japp.classfile;

public final class ClassFile {

    public static final int MAGIC_NUMBER = 0xcafebabe;

    public static final byte CONSTANT_Utf8 = 1;
    public static final byte CONSTANT_Integer = 3;
    public static final byte CONSTANT_Float = 4;
    public static final byte CONSTANT_Long = 5;
    public static final byte CONSTANT_Double = 6;
    public static final byte CONSTANT_Class = 7;
    public static final byte CONSTANT_String = 8;
    public static final byte CONSTANT_Fieldref = 9;
    public static final byte CONSTANT_Methodref = 10;
    public static final byte CONSTANT_InterfaceMethodref = 11;
    public static final byte CONSTANT_NameAndType = 12;
    public static final byte CONSTANT_MethodHandle = 15;
    public static final byte CONSTANT_MethodType = 16;
    public static final byte CONSTANT_Dynamic = 17;
    public static final byte CONSTANT_InvokeDynamic = 18;
    public static final byte CONSTANT_Module = 19;
    public static final byte CONSTANT_Package = 20;

    public static final byte CONSTANT_EXTERNAL_STRING = -1;
    public static final byte CONSTANT_EXTERNAL_STRING_Class = -2;
    public static final byte CONSTANT_EXTERNAL_STRING_Descriptor = -3;
    public static final byte CONSTANT_EXTERNAL_STRING_Signature = -4;

    public static final byte[] CONSTANT_SIZE = new byte[32];
    static {
        CONSTANT_SIZE[CONSTANT_Integer] = 4;
        CONSTANT_SIZE[CONSTANT_Float] = 4;
        CONSTANT_SIZE[CONSTANT_Long] = 8;
        CONSTANT_SIZE[CONSTANT_Double] = 8;
        CONSTANT_SIZE[CONSTANT_Class] = 2;
        CONSTANT_SIZE[CONSTANT_String] = 2;
        CONSTANT_SIZE[CONSTANT_Fieldref] = 4;
        CONSTANT_SIZE[CONSTANT_Methodref] = 4;
        CONSTANT_SIZE[CONSTANT_InterfaceMethodref] = 4;
        CONSTANT_SIZE[CONSTANT_NameAndType] = 4;
        CONSTANT_SIZE[CONSTANT_MethodHandle] = 3;
        CONSTANT_SIZE[CONSTANT_MethodType] = 2;
        CONSTANT_SIZE[CONSTANT_Dynamic] = 4;
        CONSTANT_SIZE[CONSTANT_InvokeDynamic] = 4;
        CONSTANT_SIZE[CONSTANT_Module] = 2;
        CONSTANT_SIZE[CONSTANT_Package] = 2;
    }

    private ClassFile() {
    }
}
