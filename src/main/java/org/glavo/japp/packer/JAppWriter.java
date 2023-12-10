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

import com.github.luben.zstd.Zstd;
import org.glavo.japp.CompressionMethod;
import org.glavo.japp.boot.JAppBootMetadata;
import org.glavo.japp.boot.JAppResource;
import org.glavo.japp.boot.JAppResourceField;
import org.glavo.japp.boot.JAppResourceGroup;
import org.glavo.japp.io.ByteBufferOutputStream;
import org.glavo.japp.io.LittleEndianDataOutput;
import org.glavo.japp.launcher.JAppConfigGroup;
import org.glavo.japp.launcher.JAppLauncherMetadata;
import org.glavo.japp.launcher.JAppResourceGroupReference;
import org.glavo.japp.packer.compressor.CompressContext;
import org.glavo.japp.packer.compressor.Compressor;
import org.glavo.japp.packer.compressor.Compressors;
import org.glavo.japp.packer.compressor.classfile.ByteArrayPoolBuilder;
import org.glavo.japp.util.XxHash64;
import org.glavo.japp.util.ZstdUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.util.*;

public final class JAppWriter implements CompressContext, Closeable {

    private static final int MAGIC_NUMBER = 0x5050414a;
    private static final short MAJOR_VERSION = -1;
    private static final short MINOR_VERSION = 0;

    private final LittleEndianDataOutput output;

    // launcher
    private final Deque<JAppConfigGroup> configStack = new ArrayDeque<>();
    private JAppConfigGroup current;

    // boot
    final List<Map<String, JAppResourceInfo>> groups = new ArrayList<>();
    final Compressor compressor = Compressors.DEFAULT;
    private final ByteArrayPoolBuilder pool = new ByteArrayPoolBuilder();

    public JAppWriter(LittleEndianDataOutput output) throws IOException {
        this(output, new JAppConfigGroup());
    }

    public JAppWriter(LittleEndianDataOutput output, JAppConfigGroup root) throws IOException {
        this.output = output;
        output.writeInt(MAGIC_NUMBER);

        this.current = root;
        configStack.push(current);
    }

    public LittleEndianDataOutput getOutput() {
        return output;
    }

    public long getCurrentOffset() {
        return output.getTotalBytes();
    }

    @Override
    public ByteArrayPoolBuilder getPool() {
        return pool;
    }

    public void beginConfigGroup(JAppConfigGroup group) {
        configStack.push(group);
        current = group;
    }

    public void endConfigGroup() {
        configStack.pop();
        if (configStack.isEmpty()) {
            throw new IllegalStateException();
        }
    }

    public JAppResourcesWriter createResourcesWriter(String name, boolean isModulePath) {
        return new JAppResourcesWriter(this, name, isModulePath ? current.modulePath : current.classPath);
    }

    public void addReference(JAppResourceGroupReference reference, boolean isModulePath) {
        Objects.requireNonNull(reference);

        if (isModulePath) {
            current.modulePath.add(reference);
        } else {
            current.classPath.add(reference);
        }
    }

    private static void writeResourceFileTimeField(ByteBufferOutputStream output, JAppResourceField field, FileTime time) {
        if (time != null) {
            output.writeByte(field.id());
            output.writeLong(time.toMillis());
        }
    }

    private static void writeResource(JAppResourceInfo resource, ByteBufferOutputStream groupBodyBuilder) throws IOException {
        byte[] nameBytes = resource.name.getBytes(StandardCharsets.UTF_8);

        groupBodyBuilder.writeByte(JAppResource.MAGIC_NUMBER);
        groupBodyBuilder.writeByte(resource.method.id());
        groupBodyBuilder.writeUnsignedShort(nameBytes.length);
        groupBodyBuilder.writeInt(0);
        groupBodyBuilder.writeLong(resource.size);
        groupBodyBuilder.writeLong(resource.compressedSize);
        groupBodyBuilder.writeLong(resource.offset);
        groupBodyBuilder.writeBytes(nameBytes);

        if (resource.checksum != null) {
            groupBodyBuilder.writeByte(JAppResourceField.CHECKSUM.id());
            groupBodyBuilder.writeLong(resource.checksum);
        }

        writeResourceFileTimeField(groupBodyBuilder, JAppResourceField.FILE_CREATE_TIME, resource.creationTime);
        writeResourceFileTimeField(groupBodyBuilder, JAppResourceField.FILE_LAST_MODIFIED_TIME, resource.lastModifiedTime);
        writeResourceFileTimeField(groupBodyBuilder, JAppResourceField.FILE_LAST_ACCESS_TIME, resource.lastAccessTime);

        groupBodyBuilder.writeByte(JAppResourceField.END.id());
    }

    private void writeBootMetadata() throws IOException {
        output.writeInt(JAppBootMetadata.MAGIC_NUMBER);
        output.writeInt(groups.size());
        pool.writeTo(output);
        for (Map<String, JAppResourceInfo> group : groups) {
            ByteBufferOutputStream groupBodyBuilder = new ByteBufferOutputStream();
            for (JAppResourceInfo resource : group.values()) {
                writeResource(resource, groupBodyBuilder);
            }
            byte[] groupBody = groupBodyBuilder.toByteArray();

            CompressionMethod method = null;
            byte[] compressed = null;
            int compressedLength = -1;

            if (groupBody.length >= 16) {
                byte[] res = new byte[ZstdUtils.maxCompressedLength(groupBody.length)];
                long n = Zstd.compressByteArray(res, 0, res.length, groupBody, 0, groupBody.length, 8);
                if (n < groupBody.length - 4) {
                    method = CompressionMethod.ZSTD;
                    compressed = res;
                    compressedLength = (int) n;
                }
            }

            if (method == null) {
                method = CompressionMethod.NONE;
                compressed = groupBody;
                compressedLength = groupBody.length;
            }

            long checksum = XxHash64.hash(groupBody);

            output.writeByte(JAppResourceGroup.MAGIC_NUMBER);
            output.writeByte(method.id());
            output.writeShort((short) 0); // reserved
            output.writeInt(groupBody.length);
            output.writeInt(compressedLength);
            output.writeInt(group.size());
            output.writeLong(checksum);
            output.writeBytes(compressed, 0, compressedLength);
        }
    }

    private void writeLauncherMetadata() throws IOException {
        current.writeTo(output);
    }

    private void writeFileEnd(long bootMetadataOffset, long launcherMetadataOffset) throws IOException {
        long fileSize = output.getTotalBytes() + JAppLauncherMetadata.FILE_END_SIZE;

        // magic number
        output.writeInt(MAGIC_NUMBER);

        // version number
        output.writeShort(MAJOR_VERSION);
        output.writeShort(MINOR_VERSION);

        // flags
        output.writeLong(0L);

        // file size
        output.writeLong(fileSize);

        // boot metadata offset
        output.writeLong(bootMetadataOffset);

        // launcher metadata offset
        output.writeLong(launcherMetadataOffset);

        // reserved
        output.writeLong(0L);
        output.writeLong(0L);
        output.writeLong(0L);

        if (output.getTotalBytes() != fileSize) {
            throw new AssertionError();
        }
    }

    @Override
    public void close() throws IOException {
        long bootMetadataOffset = getCurrentOffset();
        writeBootMetadata();

        long launcherMetadataOffset = getCurrentOffset();
        writeLauncherMetadata();

        writeFileEnd(bootMetadataOffset, launcherMetadataOffset);
    }
}
