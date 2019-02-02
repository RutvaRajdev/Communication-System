package edu.northeastern.ccs.im.model;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageTest {

    ChatMessage chatMessage;
    @BeforeEach
    void setUp() {
        chatMessage = new ChatMessage();
        SingleMessage singleMessage = new SingleMessage("team-103 initial message", MessageType.IMAGE);
        List<SingleMessage> msgs = new LinkedList<>();
        msgs.add(singleMessage);
        chatMessage = new ChatMessage(msgs, "group1");
    }

    @AfterEach
    void tearDown() {
        chatMessage = null;
    }

    @Test
    void getId() {
        ObjectId messageID = chatMessage.getId();
        assertEquals(messageID, chatMessage.getId());
    }

    @Test
    void setId() {
        ObjectId messageID = new ObjectId();
        chatMessage.setId(messageID);
        assertEquals(messageID, chatMessage.getId());
    }

    @Test
    void getMessage() {
        String message = "team-103 initial message";
        assertEquals(message, chatMessage.getMessages().get(0).getMessage());
    }

    @Test
    void setMessage() {
        SingleMessage singleMessage = new SingleMessage("team-103 second message", MessageType.IMAGE);
        List<SingleMessage> messages = new LinkedList<>();
        messages.add(singleMessage);
        chatMessage.setMessages(messages);
        assertTrue(chatMessage.getMessages().size() == 1);
        assertEquals("team-103 second message", chatMessage.getMessages().get(0).getMessage());
    }

    @Test
    void getGroupId() {
        String groupName = chatMessage.getGroupName();
        assertEquals(groupName, chatMessage.getGroupName());
    }

    @Test
    void setGroupId() {
        String groupName = "group2";
        chatMessage.setGroupName(groupName);
        assertEquals(groupName, chatMessage.getGroupName());
    }
}