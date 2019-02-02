package edu.northeastern.ccs.im.model;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChatGroupTest {

    ChatGroup chatGroup;
    @BeforeEach
    void setUp() {
        chatGroup = new ChatGroup("team-103", new ObjectId());
    }

    @AfterEach
    void tearDown() {
        chatGroup =null;
    }

    @Test
    void getName() {
        String name = "team-103";
        assertEquals(name, chatGroup.getName());
    }

    @Test
    void setName() {
        String name = "team-1033";
        chatGroup.setName(name);
        assertEquals(name, chatGroup.getName());
    }

    @Test
    void getIds() {
        chatGroup = new ChatGroup();
        ObjectId username = new ObjectId();
        chatGroup.setId(username);
        assertEquals(username, chatGroup.getId());
    }

    @Test
    void getUserIds() {
        ObjectId username = new ObjectId();
        Set<ObjectId> idSet = new HashSet<>();
        idSet.add(username);
        chatGroup.setMembers(idSet);
        assertEquals(idSet, chatGroup.getMembers());
    }

    @Test
    void setUserIds() {
        // Random user id
        ObjectId username = new ObjectId();
        Set<ObjectId> ids = new HashSet<>();
        ids.add(username);
        chatGroup.setMembers(ids);
        assertEquals(ids, chatGroup.getMembers());
    }
}