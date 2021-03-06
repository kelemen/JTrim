package org.jtrim2.concurrent.query.io;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

import static org.junit.Assert.*;

public class FileChannelOpenerTest {
    private static FileChannelOpener create(Path fileToOpen) {
        return new FileChannelOpener(fileToOpen);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor() {
        create(null);
    }

    @Test(timeout = 30000)
    public void testOpenChanel() throws Exception {
        Path tmpFile = Files.createTempFile("jtrim", ".test");
        try {
            byte[] content = new byte[]{54, 64, 87, 23};
            Files.write(tmpFile, content);

            try (FileChannel channel = create(tmpFile).openChanel()) {
                ByteBuffer buffer = ByteBuffer.allocate(content.length + 1);
                while (channel.read(buffer) >= 0) {
                }

                byte[] received = new byte[content.length];
                buffer.flip();
                buffer.get(received);

                assertArrayEquals(content, received);
            }
        } finally {
            Files.delete(tmpFile);
        }
    }
}
