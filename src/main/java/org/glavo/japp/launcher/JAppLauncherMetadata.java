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

            endBuffer.limit(endBufferSize).position(endBufferSize - FILE_END_SIZE);

            int magicNumber = endBuffer.getInt();
            if (magicNumber != 0x5050414a) {
                throw new IOException("Invalid magic number: " + Long.toHexString(magicNumber));
            }

            short majorVersion = endBuffer.getShort();
            short minorVersion = endBuffer.getShort();

            if (majorVersion != MAJOR_VERSION || minorVersion != MINOR_VERSION) {
                throw new IOException("Version number mismatch");
            }

            long flags = endBuffer.getLong();

            long fileContentSize = endBuffer.getLong();
            long metadataOffset = endBuffer.getLong();
            long bootMetadataOffset = endBuffer.getLong();

            assert endBuffer.remaining() == 24;

            if (flags != 0) {
                throw new IOException("Unsupported flags: " + Long.toBinaryString(flags));
            }

            if (fileContentSize > fileSize || fileContentSize < FILE_END_SIZE) {
                throw new IOException("Invalid file size: " + fileContentSize);
            }

            if (metadataOffset >= fileContentSize - FILE_END_SIZE) {
                throw new IOException("Invalid metadata offset: " + metadataOffset);
            }

            long baseOffset = fileSize - fileContentSize;
            long metadataSize = fileContentSize - FILE_END_SIZE - metadataOffset;

            JAppConfigGroup group;
            if (metadataSize < endBufferSize - FILE_END_SIZE) {
                group = JAppConfigGroup.readConfigGroup(endBuffer.array(), (int) (endBufferSize - metadataSize - FILE_END_SIZE), (int) metadataSize);
            } else {
                if (metadataSize > (1 << 30)) {
                    throw new IOException("Metadata is too large");
                }

                ByteBuffer metadataBuffer = ByteBuffer.allocate((int) metadataSize);
                channel.position(baseOffset + metadataOffset);
                IOUtils.readFully(channel, metadataBuffer);

                group = JAppConfigGroup.readConfigGroup(metadataBuffer.array(), 0, (int) metadataSize);
            }

            return new JAppLauncherMetadata(baseOffset, bootMetadataOffset, group);
        }
    }

    private final long baseOffset;
    private final long bootMetadataOffset;

    private final JAppConfigGroup group;

    public JAppLauncherMetadata(long baseOffset, long bootMetadataOffset, JAppConfigGroup group) {
        this.baseOffset = baseOffset;
        this.bootMetadataOffset = bootMetadataOffset;
        this.group = group;
    }

    public long getBaseOffset() {
        return baseOffset;
    }

    public long getBootMetadataOffset() {
        return bootMetadataOffset;
    }

    public JAppConfigGroup getGroup() {
        return group;
    }
}
