package edu.northeastern.ccs.im.exception;

/**
 * Extends exception class to throw exception when user tries to create group that already exists
 * @author rohanchitnis
 */
public class GroupAlreadyExistsException extends Exception {

    /**
     * @param message   message to display when group already exists
     */
    public GroupAlreadyExistsException(String message) {
        super(message);
    }
}
