package edu.northeastern.ccs.im.service;

import edu.northeastern.ccs.im.exception.UserAlreadyExistsException;
import edu.northeastern.ccs.im.model.User;
import edu.northeastern.ccs.im.model.UserDTO;
import org.bson.types.ObjectId;

/**
 * Defines CRUD operation on User. Works with user database in mongodb
 * @author rohanchitnis
 */
public interface UserService {

    /**
     * Creates a user with according to the given name, email, phone and password as per the UserDTO object
     * @param userDTO                       the UserDTO representing the user
     * @return                              newly created user
     * @throws UserAlreadyExistsException   if the user already exists in the database
     */
    User createUser(UserDTO userDTO) throws UserAlreadyExistsException;

    /**
     * Updates the old user with a new one as per the new UserDTO object
     * @param oldUser                       the UserDTO consisting of the
     *                                      old username or password
     * @param newUser                       the UserDTO consisting of the
     *                                      new username or password
     * @return                              Updated User
     */
    User updateUser(UserDTO oldUser, UserDTO newUser);

    /**
     * Finds a user from the given user name and deletes it
     * @param userName      given username
     * @return              Deleted User
     */
    User deleteUser(String userName);

    /**
     * Searches for a user with given username and returns it, if found
     * @param username      given username
     * @return              User, if found in database, null otherwise
     */
    User findUserWithUserName(String username);

    /**
     * Searches for a user with given credentials and returns it, if found
     * @param username      given username
     * @param password      given password
     * @return              User, if found in database, null otherwise
     */
    User findUserWithCredentials(String username, String password);

    /**
     * Updates old user with new user as User object
     * @return              Updated user
     */
    User updateUser(User oldUser, User newUser);

    /**
     * Searches the user by given id and returns it
     */
    User findById(ObjectId id);

    /**
     * Updates IP address of the given user in database
     * @param username      Given user
     * @param ip            Given IP to be updated
     * @return              Updated user with new IP address
     */
    User updateIP(String username, String ip);

}
