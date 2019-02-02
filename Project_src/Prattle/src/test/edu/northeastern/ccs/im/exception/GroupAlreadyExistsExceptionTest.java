package edu.northeastern.ccs.im.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroupAlreadyExistsExceptionTest {

    @Test
    void testConstructor() {
        assertTrue(new GroupAlreadyExistsException("Group friends already exists") instanceof GroupAlreadyExistsException);
    }

}