
package edu.northeastern.ccs.im.exception;

/**
 * Extension of the Exception class that provides an exception
 * if the user already exists in the file
 *
 * @author  rohanchitnis
 */
public class UserAlreadyExistsException extends Exception {

    /**
     * Constructor of the <code>UserAlreadyExistsException</code>
     * @param message       the message
     */
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
