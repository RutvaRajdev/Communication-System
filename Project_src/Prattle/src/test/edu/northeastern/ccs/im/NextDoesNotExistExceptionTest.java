package edu.northeastern.ccs.im;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NextDoesNotExistExceptionTest {

    @Test
    public void testException() {
        assertTrue(new NextDoesNotExistException("user entry does not exists") instanceof RuntimeException);
    }
}