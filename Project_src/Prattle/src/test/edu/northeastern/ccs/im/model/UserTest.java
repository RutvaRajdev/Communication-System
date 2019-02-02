package edu.northeastern.ccs.im.model;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    User user;
    @BeforeEach
    void setUp() {
        user = new User();
        user = new User("team-103","team-103@husky.neu.edu","8675657777","1234", "168.9.9.180");
    }

    @AfterEach
    void tearDown() {
        user =null;
    }

    @Test
    void getName() {
        String name = "team-103";
        assertTrue(user.getName().equalsIgnoreCase(name));
    }

    @Test
    void setName() {
        user.setName("team-103-rocks");
        assertTrue(user.getName().equalsIgnoreCase("team-103-rocks"));
    }

    @Test
    void getEmail() {
        String email = "team-103@husky.neu.edu";
        assertEquals(email, user.getEmail());
    }

    @Test
    void setEmail() {
        String email = "team-103-rocks@husky.neu.edu";
        user.setEmail(email);
        assertEquals(email, user.getEmail());
    }

    @Test
    void getPassword() {
        user = new User("team-other", "1234");
        String paswd = "1234";
        assertEquals(paswd, user.getPassword());
    }

    @Test
    void setPassword() {
        String paswd = "12345";
        user.setPassword(paswd);
        assertEquals(paswd, user.getPassword());
    }

    @Test
    void getPhone() {
        String phone = "8675657777";
        assertEquals(phone, user.getPhone());
    }

    @Test
    void setPhone() {
        String phone = "8675657778";
        user.setPhone(phone);
        assertEquals(phone, user.getPhone());
    }

    @Test
    void getGroupIds() {
        ChatGroup chatGroup = new ChatGroup("team-103-group", user.getId());
        Set<ObjectId> ids = new HashSet<>();
        ids.add(chatGroup.getId());
        user.setGroupIds(ids);
        assertEquals(ids, user.getGroupIds());
    }

    @Test
    void setGroupIds() {
        ChatGroup chatGroup = new ChatGroup("team-103-group1", user.getId());
        Set<ObjectId> ids = new HashSet<>();
        ids.add(chatGroup.getId());
        user.setGroupIds(ids);
        assertEquals(ids, user.getGroupIds());
    }

    @Test
    void getSetIP() {
        user.setIp("1.1.1.1");
        assertEquals("1.1.1.1", user.getIp());
    }

    @Test
    void testId() {
        ObjectId objectId = new ObjectId();
        user.setId(objectId);
        assertEquals(objectId, user.getId());
    }

    @Test
    void testToString() {
        assertEquals("User{name='team-103', email='team-103@husky.neu.edu', phone='8675657777', password='null'}", user.toString());
    }

    @Test
    void testDecryptPwd() throws Exception {
        Method method = User.class.getDeclaredMethod("decryptPassword", String.class);
        method.setAccessible(true);
        String pwd = (String)method.invoke(user, "abc");
        assertNull(pwd);
    }

    @Test
    void parentalControl() {
        user.setParentalControl(true);
        assertTrue(user.isParentalControl());
        user.setParentalControl(false);
        assertFalse(user.isParentalControl());
    }
}