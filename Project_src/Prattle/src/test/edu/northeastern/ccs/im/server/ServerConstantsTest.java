package edu.northeastern.ccs.im.server;

import edu.northeastern.ccs.im.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;

public class ServerConstantsTest {

    private GregorianCalendar cal = new GregorianCalendar();
    private ArrayList<Message> messages;

    @BeforeEach
    public void setUp() {
        messages = new ArrayList<>();
    }

    @Test
    public void getBroadcastResponsesDateCommand() {
        // test server response for "What is the date?"
        String date = (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DATE) + "/" + cal.get(Calendar.YEAR);
        messages.add(Message.makeBroadcastMessage(ServerConstants.NIST_ID, date));
        assertEquals(messages.get(0).toString(),
                ServerConstants.getBroadcastResponses("What is the date?").get(0).toString());
    }

    @Test
    public void getBroadcastResponsesTimeCommand() {
        // test server response for "What time is it?"
        String time = cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE);
        messages.add(Message.makeBroadcastMessage(ServerConstants.NIST_ID, time));
        assertEquals(messages.get(0).toString(),
                ServerConstants.getBroadcastResponses("What time is it?").get(0).toString());
    }

    @Test
    public void getBroadcastResponsesImpatientCommand() {
        // test server response for "What time is it Mr. Fox?"
        String time = cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE);
        messages.add(Message.makeBroadcastMessage("BBC", "The time is now"));
        messages.add(Message.makeBroadcastMessage("Mr. Fox", time));
        assertEquals(messages.get(0).toString(),
                ServerConstants.getBroadcastResponses("What time is it Mr. Fox?").get(0).toString());
        assertEquals(messages.get(1).toString(),
                ServerConstants.getBroadcastResponses("What time is it Mr. Fox?").get(1).toString());
    }

    @Test
    public void getBroadcastResponsesOtherCommand() {
        // test server response for "How are you?"
        messages.add(Message.makeBroadcastMessage(ServerConstants.SERVER_NAME,
                "Why are you asking me this?"));
        messages.add(Message.makeBroadcastMessage(ServerConstants.SERVER_NAME,
                "I am a computer program. I run."));
        assertEquals(messages.get(0).toString(),
                ServerConstants.getBroadcastResponses("How are you?").get(0).toString());
        assertEquals(messages.get(1).toString(),
                ServerConstants.getBroadcastResponses("How are you?").get(1).toString());

        // test server response for "Hello"
        messages = new ArrayList<>();
        messages.add(Message.makeBroadcastMessage(ServerConstants.SERVER_NAME,
                "Hello.  How are you?"));
        messages.add(Message.makeBroadcastMessage(ServerConstants.SERVER_NAME,
                "I can communicate with you to test your code."));
        assertEquals(messages.get(0).toString(),
                ServerConstants.getBroadcastResponses("Hello").get(0).toString());
        assertEquals(messages.get(1).toString(),
                ServerConstants.getBroadcastResponses("Hello").get(1).toString());

        // test server response for "WTF"
        messages = new ArrayList<>();
        messages.add(Message.makeBroadcastMessage(ServerConstants.SERVER_NAME,
                "OMG ROFL TTYL"));
        assertEquals(messages.get(0).toString(),
                ServerConstants.getBroadcastResponses("WTF").get(0).toString());
    }

    @Test
    public void getBroadcastResponseNull() {

        assertEquals(null,
                ServerConstants.getBroadcastResponses(""));
    }
}