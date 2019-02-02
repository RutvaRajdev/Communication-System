package edu.northeastern.ccs.im.service;


import edu.northeastern.ccs.im.model.Subpoena;

import java.util.List;

/**
 * Interface contains methods used to communicate with subpoena database
 * Subpoena allows user to tap communication between 2 entities or a group. Tapped messages can be
 * in the past or it can also be a live communication.
 *
 * @author rohanchitnis
 */
public interface SubpeonaServices {

    /**
     * Creates new subpoena with given parameters
     * @param username          Given username of subpoena
     * @param pwd               Given password
     * @param user1             Given username of user1
     * @param user2             Given username of user2
     * @param groupName         Given name of group
     * @return                  Newly created Subpoena
     */
    Subpoena createSubpoena(String username, String pwd, String user1, String user2, String groupName);

    /**
     * Finds and returns Subpoena by name
     * @param username          Given name of subpoena
     * @return                  Subpoena with given name
     */
    Subpoena findByName(String username);

    /**
     * Finds and returns Subpoena by users it monitors
     * @param user1             Name of first user
     * @param user2             Name of second user
     * @return                  List of Subpoena that monitors given users
     */
    List<Subpoena> findByUsersMonitored(String user1, String user2);

    /**
     * Finds and returns Subpoena by group it monitors
     * @param groupName         Name of the group that is monitored
     * @return                  List of Subpoena that monitors given group
     */
    List<Subpoena> findByGroupMonitored(String groupName);

    /**
     * Find user by name and password and returns the same
     * @param username          Given username
     * @param password          Given password
     * @return                  User that matches given criteria
     */
    Subpoena findSubpeona(String username, String password);

}
