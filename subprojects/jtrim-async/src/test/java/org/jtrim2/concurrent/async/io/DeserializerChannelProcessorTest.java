package org.jtrim2.concurrent.async.io;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.jtrim2.concurrent.async.AsyncDataListener;
import org.jtrim2.concurrent.async.AsyncDataState;
import org.jtrim2.concurrent.async.AsyncReport;
import org.jtrim2.concurrent.async.io.ChannelProcessor.StateListener;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.jtrim2.concurrent.async.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DeserializerChannelProcessorTest {

    public DeserializerChannelProcessorTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testProcessChannel() throws IOException {
        TestObj testObj = new TestObj("DeserializerChannelProcessorTest.testProcessChannel - 543467575");

        Path tmpFile = Files.createTempFile("jtrim", ".test");
        try {
            try (OutputStream fileOutput = Files.newOutputStream(tmpFile);
                    OutputStream bufferedOutput = new BufferedOutputStream(fileOutput, 64 * 1024);
                    ObjectOutputStream objOutput = new ObjectOutputStream(bufferedOutput)) {
                objOutput.writeObject(testObj);
            }

            try (FileChannel channel = FileChannel.open(tmpFile)) {
                AsyncDataListener<Object> listener = mockListener();
                StateListener stateListener = mock(ChannelProcessor.StateListener.class);

                DeserializerChannelProcessor processor = new DeserializerChannelProcessor();
                processor.processChannel(channel, listener, stateListener);

                ArgumentCaptor<AsyncReport> report = ArgumentCaptor.forClass(AsyncReport.class);

                InOrder inOrder = inOrder(listener);
                inOrder.verify(listener).onDataArrive(eq(testObj));
                inOrder.verify(listener, atLeast(0)).onDoneReceive(report.capture());
                inOrder.verifyNoMoreInteractions();

                if (report.getAllValues().size() > 0) {
                    assertTrue(report.getValue().isSuccess());
                }

                ArgumentCaptor<AsyncDataState> state = ArgumentCaptor.forClass(AsyncDataState.class);
                verify(stateListener, atLeastOnce()).setState(state.capture());
                assertEquals(1.0, state.getValue().getProgress(), 0.000001);
            }
        } finally {
            Files.delete(tmpFile);
        }
    }

    private static class TestObj implements Serializable {
        private static final long serialVersionUID = 842443504475914392L;

        private final String field;

        public TestObj(String field) {
            this.field = field;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 97 * hash + Objects.hashCode(this.field);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final TestObj other = (TestObj)obj;
            return Objects.equals(this.field, other.field);
        }
    }
}
