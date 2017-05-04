package org.jtrim2.collections;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *
 * @author Kelemen Attila
 */
public final class SerializationHelper {
    public static byte[] serializeObject(Object obj) throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(4096);
                ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(obj);
            output.flush();

            return bytes.toByteArray();
        }
    }

    public static Object deserializeObject(byte[] serialized) throws IOException, ClassNotFoundException {
        try (InputStream bytes = new ByteArrayInputStream(serialized);
                ObjectInputStream input = new ObjectInputStream(bytes)) {
            return input.readObject();
        }
    }

    private SerializationHelper() {
        throw new AssertionError();
    }
}
