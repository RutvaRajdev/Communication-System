package edu.northeastern.ccs.im;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.nio.channels.Selector;
import java.util.LinkedList;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

public class ScanNetNBTest extends ParentServerTest {

    @Mock
    Selector selector;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void hasNextMessage() throws Exception {
        ScanNetNB scanNetNB = new ScanNetNB(getSocketNB());
        Field message = ScanNetNB.class.getDeclaredField("messages");
        message.setAccessible(true);
        Queue<Message> messages = new LinkedList<>();
        messages.add(Message.makeQuitMessage("BYE"));
        message.set(scanNetNB, messages);
        assertTrue(scanNetNB.hasNextMessage());
    }

    @Test
    public void testNextMessage() throws Exception {
        ScanNetNB scanNetNB = new ScanNetNB(getSocketNB());
        Field message = ScanNetNB.class.getDeclaredField("messages");
        message.setAccessible(true);
        Queue<Message> messages = new LinkedList<>();
        messages.add(Message.makeQuitMessage("BYE"));
        message.set(scanNetNB, messages);
        assertTrue(scanNetNB.nextMessage().terminate());
    }

    @Test
    public void shouldThrowException() {
        ScanNetNB scanNetNB = new ScanNetNB(getSocketNB());
        try {
            scanNetNB.nextMessage();
        }
        catch (Exception e) {
            assertThat(e, instanceOf(NextDoesNotExistException.class));
        }
    }

    @Test
    public void close() throws Exception {
        SocketNB socketNb = new SocketNB("34.224.165.138", 4545);
        ScanNetNB scanNetNB = new ScanNetNB(socketNb);
        doThrow(new IOException()).when(selector).close();
        scanNetNB.close();
    }

}