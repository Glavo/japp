package org.glavo.japp.packer.compressor.classfile;

import org.glavo.japp.classfile.ClassFile;
import org.glavo.japp.util.MUTF8;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.glavo.japp.classfile.ClassFile.*;

final class ClassFileReader {
    private final ByteBuffer buffer;
    final byte[] tags;
    final int[] positions;
    final String[] strings;

    final int minorVersion;
    final int majorVersion;

    final int cpCount;
    final int tailPosition;
    final int tailLen;
    final int accessFlags;
    final int thisClass;
    final int superClass;

    ClassFileReader(ByteBuffer buffer) throws IOException {
        this.buffer = buffer;

        int magic = buffer.getInt();
        if (magic != ClassFile.MAGIC_NUMBER) {
            throw new IOException("Invalid magic number: " + Integer.toHexString(magic));
        }

        minorVersion = Short.toUnsignedInt(buffer.getShort());
        majorVersion = Short.toUnsignedInt(buffer.getShort());

        cpCount = readU2();
        tags = new byte[cpCount];
        positions = new int[cpCount];
        strings = new String[cpCount];

        scanConstantPool();

        tailPosition = buffer.position();
        tailLen = buffer.remaining();

        accessFlags = Short.toUnsignedInt(buffer.getShort());
        thisClass = Short.toUnsignedInt(buffer.getShort());
        superClass = Short.toUnsignedInt(buffer.getShort());

        int interfacesCount = Short.toUnsignedInt(buffer.getShort());
        buffer.position(buffer.position() + interfacesCount * 2); // interfaces

        // scan fields
        scanFieldsOrMethods();

        // scan methods
        scanFieldsOrMethods();

        scanAttributes();
    }

    private int readU1() {
        return Short.toUnsignedInt(buffer.get());
    }

    private int readU2() {
        return Short.toUnsignedInt(buffer.getShort());
    }

    private IOException notUtf8(int cpIndex) {
        return new IOException("Constant #" + cpIndex + " is not CONSTANT_Utf8");
    }

    private void markString(int index, byte tag) throws IOException {
        byte oldTag = tags[index];

        if (oldTag > 0 && oldTag != CONSTANT_Utf8) {
            throw notUtf8(index);
        }

        boolean update;
        if (oldTag < 0 && oldTag != tag) {
            if (oldTag == CONSTANT_EXTERNAL_STRING_Class && tag == CONSTANT_EXTERNAL_STRING_Descriptor) {
                // array type
                update = true;
            } else if (oldTag == CONSTANT_EXTERNAL_STRING_Descriptor) {
                update = false;
            } else if (oldTag == CONSTANT_EXTERNAL_STRING_Signature && tag == CONSTANT_EXTERNAL_STRING_Descriptor) {
                update = true;
            } else {
                throw new IOException(String.format("index: %d, old: %s, new: %s", index, oldTag, tag));
            }
        } else {
            update = oldTag != tag;
        }

        if (update) {
            tags[index] = tag;
        }
    }

    private void markDescriptor(int descriptorIndex) throws IOException {
        markString(descriptorIndex, CONSTANT_EXTERNAL_STRING_Descriptor);
    }

    private void markSignature(int signatureIndex) throws IOException {
        markString(signatureIndex, CONSTANT_EXTERNAL_STRING_Signature);
    }

    private void markClass(int classIndex) throws IOException {
        markString(classIndex, CONSTANT_EXTERNAL_STRING_Class);
    }

    private void skip(int n) {
        buffer.position(buffer.position() + n);
    }

    private String getString(int cpIndex) throws IOException {
        byte tag = tags[cpIndex];
        if (tag != CONSTANT_Utf8 && tag >= 0) {
            throw notUtf8(cpIndex);
        }

        String str;
        if ((str = strings[cpIndex]) != null) {
            return str;
        }

        int position = positions[tag];
        int len = Short.toUnsignedInt(buffer.getShort(position));
        str = strings[cpIndex] = MUTF8.stringFromMUTF8(buffer.array(), buffer.arrayOffset() + position + 2, len);
        return str;
    }

    private void scanConstantPool() throws IOException {
        for (int i = 1; i < cpCount; i++) {
            final byte tag = buffer.get();
            positions[i] = buffer.position();

            if (tags[i] == 0) {
                tags[i] = tag;
            } else if (tag != CONSTANT_Utf8) {
                throw notUtf8(i);
            }

            switch (tag) {
                case CONSTANT_Utf8:
                    int strLen = readU2();
                    strings[i] = MUTF8.stringFromMUTF8(buffer.array(), buffer.arrayOffset() + buffer.position(), strLen);
                    skip(strLen);
                    break;
                case CONSTANT_NameAndType: {
                    int nameIndex = readU2();
                    int descriptorIndex = readU2();
                    markDescriptor(descriptorIndex);
                    break;
                }
                case CONSTANT_MethodType: {
                    int descriptorIndex = readU2();
                    markDescriptor(descriptorIndex);
                    break;
                }
                case CONSTANT_Class: {
                    int nameIndex = readU2();
                    markClass(nameIndex);
                    break;
                }
                case CONSTANT_Long:
                case CONSTANT_Double: {
                    i++;
                    buffer.getLong();
                    break;
                }
                default:
                    if (tag >= ClassFile.CONSTANT_SIZE.length || tag <= 0 || ClassFile.CONSTANT_SIZE[tag] <= 0) {
                        throw new IOException("Invalid constant pool item: tag=" + tag);
                    }

                    skip(ClassFile.CONSTANT_SIZE[tag]);
            }
        }
    }

    private void scanFieldsOrMethods() throws IOException {
        int count = readU2();
        for (int i = 0; i < count; i++) {
            int accessFlags = readU2();
            int nameIndex = readU2();
            int descriptorIndex = readU2();

            markDescriptor(descriptorIndex);

            scanAttributes();
        }
    }

    private static void assertAttributeLength(int expected, int actual) throws IOException {
        if (expected != actual) {
            throw new IOException("expected: " + expected + ", actual: " + actual);
        }
    }

    private void scanAttributes() throws IOException {
        int attributesCount = readU2();
        for (int i = 0; i < attributesCount; i++) {
            int attributeNameIndex = readU2();
            int attributeLength = buffer.getInt();
            if (attributeLength < 0 || attributeLength > buffer.remaining()) {
                throw new IOException("attribute length is too large");
            }

            String name = getString(attributeNameIndex);
            switch (name) {
                case "Signature": {
                    assertAttributeLength(attributeLength, 2);

                    int signatureIndex = readU2();
                    markSignature(signatureIndex);
                    break;
                }
                case "RuntimeVisibleAnnotations":
                case "RuntimeInvisibleAnnotations": {
                    int numAnnotations = readU2();
                    for (int j = 0; j < numAnnotations; j++) {
                        scanAnnotation();
                    }
                    break;
                }
                case "RuntimeVisibleParameterAnnotations":
                case "RuntimeInvisibleParameterAnnotations": {
                    int numParameters = readU1();
                    for (int j = 0; j < numParameters; j++) {
                        int numAnnotations = readU2();
                        for (int k = 0; k < numAnnotations; k++) {
                            scanAnnotation();
                        }
                    }
                    break;
                }
                case "LocalVariableTable": {
                    int localVariableTableLength = readU2();
                    for (int j = 0; j < localVariableTableLength; j++) {
                        int startPc = readU2();
                        int length = readU2();
                        int nameIndex = readU2();
                        int descriptorIndex = readU2();
                        int index = readU2();

                        markDescriptor(descriptorIndex);
                    }
                    break;
                }
                case "LocalVariableTypeTable": {
                    int localVariableTypeTableLength = readU2();
                    for (int j = 0; j < localVariableTypeTableLength; j++) {
                        int startPc = readU2();
                        int length = readU2();
                        int nameIndex = readU2();
                        int signatureIndex = readU2();
                        int index = readU2();

                        markSignature(signatureIndex);
                    }
                    break;
                }
                default:
                    skip(attributeLength);
            }
        }
    }

    private void scanAnnotation() throws IOException {
        int typeIndex = readU2();
        markDescriptor(typeIndex);

        int numElementValuePairs = readU2();
        for (int i = 0; i < numElementValuePairs; i++) {
            int elementNameIndex = readU2();
            scanElementValue();
        }
    }

    /*
     * element_value {
     *     u1 tag;
     *     union {
     *         u2 const_value_index;
     *
     *         {   u2 type_name_index;
     *             u2 const_name_index;
     *         } enum_const_value;
     *
     *         u2 class_info_index;
     *
     *         annotation annotation_value;
     *
     *         {   u2            num_values;
     *             element_value values[num_values];
     *         } array_value;
     *     } value;
     * }
     */
    private void scanElementValue() throws IOException {
        byte tag = buffer.get();
        switch (tag) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'Z':
            case 's': {
                int constValueIndex = readU2();
                break;
            }
            case 'e': {
                int typeNameIndex = readU2();
                int constNameIndex = readU2();
                break;
            }
            case 'c': {
                int classInfoIndex = readU2();
                break;
            }
            case '@': {
                scanAnnotation();
                break;
            }
            case '[': {
                int numValues = readU2();
                for (int i = 0; i < numValues; i++) {
                    scanElementValue();
                }
            }
            default:
                throw new IOException(String.format("Unknown element value: 0x%02x", tag));
        }
    }
}
