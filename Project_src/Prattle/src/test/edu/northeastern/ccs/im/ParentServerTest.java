package edu.northeastern.ccs.im;

import edu.northeastern.ccs.im.server.Prattle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.nio.channels.SocketChannel;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParentServerTest {

    private static Thread thread;
    private static boolean serverStarted = false;
    private static SocketNB socketNB;

    @BeforeAll
    public static void setUpServer() throws Exception {
        if (!serverStarted) {
            thread = new Thread(new PrattleRunnable());
            thread.start();
            // 18.191.168.130
            socketNB = new SocketNB("34.224.165.138", 4545);
        }
        serverStarted = true;
    }

    public static SocketNB getSocketNB() {
        return socketNB;
    }

    public SocketChannel getSocketChannel() {
        return socketNB.getSocket();
    }

    public SocketChannel getTempSocketChannel() throws Exception {
        SocketNB socketNB = new SocketNB("34.224.165.138", 4545);
        return socketNB.getSocket();
    }

    @Test
    public void test() {
        assertTrue(true);
    }

    @AfterAll
    public static void cleanUp() throws Exception {
        thread.interrupt();
    }

    static class PrattleRunnable implements Runnable {
        public void run() {
            try {
                String[] args = new String[10];
                Prattle.main(args);
            } catch (Exception exc) {
                // todo add Logger
            }
        }
    }

}
