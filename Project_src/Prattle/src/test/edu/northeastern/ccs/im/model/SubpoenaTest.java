package edu.northeastern.ccs.im.model;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class SubpoenaTest {

    private Subpoena subpoena1;
    private Subpoena subpoena2;

    @BeforeEach
    public void setUp() {
        subpoena1 = new Subpoena("u", "pwd","u1", "u2", null, new Date(), new Date());
        subpoena2 = new Subpoena("u", "pwd",null, null, "grp", new Date(), new Date());
    }

    @Test
    void testId() {
        ObjectId id = new ObjectId();
        subpoena1 = new Subpoena();
        subpoena1.setId(id);
        assertEquals(id, subpoena1.getId());
    }

    @Test
    void testUserName() {
        subpoena1.setUsername("uu");
        assertEquals("uu", subpoena1.getUsername());
    }

    @Test
    void testUser1() {
        subpoena1.setUser1("u11");
        assertEquals("u11", subpoena1.getUser1());
    }

    @Test
    void testUser2() {
        subpoena1.setUser2("u22");
        assertEquals("u22", subpoena1.getUser2());
    }

    @Test
    void testGroupname() {
        subpoena2.setGroupName("grp2");
        assertEquals("grp2", subpoena2.getGroupName());
    }


    @Test
    void testPassword() {
        subpoena1.setPassword("pwd");
        assertEquals("pwd", subpoena1.getPassword());
    }

    @Test
    void testTimestamp() {
        Date d1 = new Date();
        subpoena1.setFromTime(d1);
        assertEquals(d1, subpoena1.getFromTime());
        subpoena1.setToTime(d1);
        assertEquals(d1, subpoena1.getToTime());
    }
}