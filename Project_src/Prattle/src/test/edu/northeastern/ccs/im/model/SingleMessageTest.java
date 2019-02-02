package edu.northeastern.ccs.im.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SingleMessageTest {

    private SingleMessage singleMessage;

    @BeforeEach
    public void setUp() {
        singleMessage = new SingleMessage("message", MessageType.IMAGE);
    }

    @Test
    void getPath() {
        singleMessage.setPath("c://some/path");
        assertEquals("c://some/path", singleMessage.getPath());
    }

    @Test
    void getMessage() {
        assertEquals("message", singleMessage.getMessage());
    }

    @Test
    void setMessage() {
        singleMessage.setMessage("new message");
        assertEquals("new message", singleMessage.getMessage());
    }

    @Test
    void getType() {
        assertEquals(MessageType.IMAGE, singleMessage.getType());
    }

    @Test
    void setType() {
        singleMessage.setType(MessageType.EMOJI);
        assertEquals(MessageType.EMOJI, singleMessage.getType());
        singleMessage.setType(MessageType.TEXT);
        assertEquals(MessageType.TEXT, singleMessage.getType());
        singleMessage.setType(MessageType.VIDEO);
        assertEquals(MessageType.VIDEO, singleMessage.getType());
    }

    @Test
    void senderReceiverIp() {
        singleMessage.setSenderIp("1.1.1.1");
        List<String> list = new LinkedList<>();
        list.add("2.2.2.2");
        singleMessage.setReceiverIp(list);
        assertEquals("1.1.1.1", singleMessage.getSenderIp());
        assertEquals("2.2.2.2", singleMessage.getReceiverIp().get(0));
    }

    @Test
    void testToString() {
        singleMessage.setType(MessageType.TEXT);
        assertEquals("SingleMessage{message='message', type=TEXT}", singleMessage.toString());
    }

    @Test
    void sender() {
        singleMessage.setSender("sender");
        assertEquals("sender", singleMessage.getSender());
    }

    @Test
    void timestamp() {
        singleMessage = new SingleMessage();
        assertNull(singleMessage.getTimestamp());
    }

    @Test
    void setTimestamp() {
        singleMessage = new SingleMessage();
        Date d = new Date();
        singleMessage.setTimestamp(d);
        assertEquals(d, singleMessage.getTimestamp());
    }
}