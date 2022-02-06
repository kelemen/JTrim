package org.jtrim2.image;

import java.util.Arrays;
import javax.imageio.metadata.IIOMetadata;
import org.junit.Test;
import org.w3c.dom.Node;

import static org.junit.Assert.*;

public class JavaIIOMetaDataTest {
    private static JavaIIOMetaData create(int width, int height, IIOMetadata metaData, boolean complete) {
        return new JavaIIOMetaData(width, height, metaData, complete);
    }

    private static IIOMetadata createDummyMetadata() {
        return new IIOMetadata() {
            @Override
            public boolean isReadOnly() {
                return false;
            }

            @Override
            public Node getAsTree(String formatName) {
                return null;
            }

            @Override
            public void mergeTree(String formatName, Node root) {
            }

            @Override
            public void reset() {
            }
        };
    }

    @Test
    public void testProperties() {
        for (int width: Arrays.asList(0, 1, 56)) {
            for (int height: Arrays.asList(0, 1, 37)) {
                for (boolean complete: Arrays.asList(false, true)) {
                    for (IIOMetadata wrapped: Arrays.asList(null, createDummyMetadata())) {
                        JavaIIOMetaData metaData = create(width, height, wrapped, complete);
                        assertEquals(width, metaData.getWidth());
                        assertEquals(height, metaData.getHeight());
                        assertEquals(complete, metaData.isComplete());
                        assertSame(wrapped, metaData.getIioMetaData());
                    }
                }
            }
        }
    }
}
