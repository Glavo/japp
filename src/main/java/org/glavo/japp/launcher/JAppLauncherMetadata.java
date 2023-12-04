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
package org.glavo.japp.launcher;

import org.glavo.japp.JAppConfigGroup;
import org.glavo.japp.util.IOUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

public final class JAppLauncherMetadata {

    public static final short MAJOR_VERSION = -1;
    public static final short MINOR_VERSION = 0;

    public static final int FILE_END_SIZE = 64;

    public static JAppLauncherMetadata readFile(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file)) {
            long fileSize = channel.size();

            if (fileSize < FILE_END_SIZE) {
                throw new IOException("File is too small");
            }

            int endBufferSize = (int) Math.min(fileSize, 8192);
            ByteBuffer endBuffer = ByteBuffer.allocate(endBufferSize).order(ByteOrder.LITTLE_ENDIAN);

            channel.position(fileSize - endBufferSize);
            IOUtils.readFully(channel, endBuffer);
            endBuffer.position(endBufferSize - FILE_END_SIZE);

            int magicNumber = endBuffer.getInt();
            short majorVersion = endBuffer.getShort();
            short minorVersion = endBuffer.getShort();
            long flags = endBuffer.getLong();
            long fileContentSize = endBuffer.getLong();
            long bootMetadataOffset = endBuffer.getLong();
            long launcherMetadataOffset = endBuffer.getLong();

            assert endBuffer.remaining() == 24; // reserved

            if (magicNumber != 0x5050414a) {
                throw new IOException("Invalid magic number: " + Long.toHexString(magicNumber));
            }

            if (majorVersion != MAJOR_VERSION || minorVersion != MINOR_VERSION) {
                throw new IOException("Version number mismatch");
            }

            if (flags != 0) {
                throw new IOException("Unsupported flags: " + Long.toBinaryString(flags));
            }

            if (fileContentSize > fileSize || fileContentSize < FILE_END_SIZE) {
                throw new IOException("Invalid file size: " + fileContentSize);
            }

            if (launcherMetadataOffset >= fileContentSize - FILE_END_SIZE) {
                throw new IOException("Invalid metadata offset: " + launcherMetadataOffset);
            }

            long baseOffset = fileSize - fileContentSize;
            long metadataSize = fileContentSize - FILE_END_SIZE - launcherMetadataOffset;

            JAppConfigGroup group;
            if (metadataSize < endBufferSize - FILE_END_SIZE) {
                int endBufferPosition = (int) (endBufferSize - metadataSize - FILE_END_SIZE);
                endBuffer.position(endBufferPosition).limit(endBufferPosition + (int) metadataSize);
                group = JAppConfigGroup.readFrom(endBuffer);
            } else {
                if (metadataSize > (1 << 30)) {
                    throw new IOException("Metadata is too large");
                }

                ByteBuffer metadataBuffer = ByteBuffer.allocate((int) metadataSize);
                channel.position(baseOffset + launcherMetadataOffset);
                IOUtils.readFully(channel, metadataBuffer);
                metadataBuffer.flip();

                group = JAppConfigGroup.readFrom(metadataBuffer);
            }

            return new JAppLauncherMetadata(baseOffset, bootMetadataOffset, launcherMetadataOffset - bootMetadataOffset, group);
        }
    }

    private final long baseOffset;
    private final long bootMetadataOffset;
    private final long bootMetadataSize;

    private final JAppConfigGroup group;

    public JAppLauncherMetadata(long baseOffset, long bootMetadataOffset, long bootMetadataSize, JAppConfigGroup group) {
        this.baseOffset = baseOffset;
        this.bootMetadataOffset = bootMetadataOffset;
        this.bootMetadataSize = bootMetadataSize;
        this.group = group;
    }

    public long getBaseOffset() {
        return baseOffset;
    }

    public long getBootMetadataOffset() {
        return bootMetadataOffset;
    }

    public long getBootMetadataSize() {
        return bootMetadataSize;
    }

    public JAppConfigGroup getGroup() {
        return group;
    }
}
