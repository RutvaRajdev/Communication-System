package edu.northeastern.ccs.im;


import org.junit.jupiter.api.Test;



import java.lang.reflect.Method;
import java.nio.ByteBuffer;


import static org.junit.jupiter.api.Assertions.*;

public class PrintNetNBTest extends ParentServerTest {

    @Test
    public void printWithServerRunning() throws Exception {
        PrintNetNB printNetNB = new PrintNetNB(getSocketNB());
        Message message = Message.makeHelloMessage("Hello");
        assertTrue(printNetNB.print(message));
    }

    @Test
    public void shouldHaveBytesRemaining() throws Exception {
        PrintNetNB printNetNB = new PrintNetNB(getSocketNB());
        Method method = PrintNetNB.class.getDeclaredMethod("hasRemaining", ByteBuffer.class, Integer.class);
        method.setAccessible(true);
        String str = "VARAD";
        ByteBuffer wrapper = ByteBuffer.wrap(str.getBytes());
        Integer sofar = 1;
        Object value = method.invoke(printNetNB, wrapper, sofar);
        boolean returned = (Boolean) value;
        assertEquals(returned, false);
    }

    @Test
    public void shouldCloseTheConnection() throws Exception {
        SocketNB socketNB = new SocketNB("34.224.165.138", 4545);
        socketNB.close();
        assertFalse(socketNB.getSocket().isOpen());
    }


    @Test
    public void throwExceptionInPrint() throws Exception {
        PrintNetNB printNetNBObj = new PrintNetNB(getSocketNB());

        assertThrows(NullPointerException.class,
                ()-> printNetNBObj.print(null));

    }
}