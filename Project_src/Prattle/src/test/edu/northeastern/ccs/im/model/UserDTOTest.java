package edu.northeastern.ccs.im.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserDTOTest {

    private UserDTO user;

    @BeforeEach
    public void setUp() {
        user = new UserDTO("team-103","team-103@husky.neu.edu","8675657777","1234");
    }

    @Test
    void getName() {
        String name = "team-103";
        assertTrue(user.getName().equalsIgnoreCase(name));
    }

    @Test
    void getEmail() {
        String email = "team-103@husky.neu.edu";
        assertEquals(email, user.getEmail());
    }

    @Test
    void getPhone() {
        String phone = "8675657777";
        assertEquals(phone, user.getPhone());
    }

    @Test
    void getPassword() {
        String password = "1234";
        assertEquals(password, user.getPassword());
    }

}