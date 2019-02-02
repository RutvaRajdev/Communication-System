package edu.northeastern.ccs.im.service;

import edu.northeastern.ccs.im.exception.GroupAlreadyExistsException;
import edu.northeastern.ccs.im.model.ChatGroup;

/**
 * Interface to work with Group database
 * @author rohanchitnis
 */
public interface GroupService {

    /**
     * Assumes user already exists
     * @param username User who wishes to create a group
     * @param groupName Group name of the new group
     * @return Newly created group
     * * @throws GroupAlreadyExistsException    When group already exists in database
     */
    ChatGroup createGroup(String username, String groupName) throws GroupAlreadyExistsException;

    /**
     * Searches for a group with given groupName and returns it, if found
     * @param groupName         groupName to search for
     * @return                  Group if found, null otherwise
     */
    ChatGroup findGroupWithGroupName(String groupName);

    /**
     * Changes group name with new name
     * @param oldName           old group name
     * @param newName           new name to be given
     * @return                  Group with new name
     */
    ChatGroup updateGroup(String oldName, String newName);

    /**
     * Deletes group from database
     * @param groupName         given groupName
     * @return                  deleted group
     */
    ChatGroup deleteGroup(String groupName);

    /**
     * Adds given user to given group
     * Assumes that group already exists, user may not exist
     * @return                  New updated group
     */
    ChatGroup addUserToGroup(ChatGroup group, String username);

    /**
     * Removes given user from given group
     * Assumes that group already exists, user may not exist
     * @return                  New updated group
     */
    ChatGroup removeUserFromGroup(ChatGroup group, String username);
}