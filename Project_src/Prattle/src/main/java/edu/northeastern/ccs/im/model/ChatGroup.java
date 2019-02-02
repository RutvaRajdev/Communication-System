package edu.northeastern.ccs.im.model;

import org.bson.types.ObjectId;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * The class that implements the functionality related to
 * ChatGroup such as getting ids, name, etc.
 *
 * @author saurabh
 *
 * Date: 11/04/2018
 */
public class ChatGroup implements Serializable {

    private ObjectId id;
    private String name;
    private Set<ObjectId> members = new HashSet<>();

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor
     */
    public ChatGroup() {
        this.id = new ObjectId();
    }

    /**
     * Public Constructor that accepts name and userId to create a chat group
     * @param name      the String representing the name
     * @param username    the username representing the unique user
     */
    public ChatGroup(String name, ObjectId username) {
        this.id = new ObjectId();
        this.name = name;
        this.members.add(username);
    }

    /**
     * Get the user ID
     * @return User ID
     */
    public ObjectId getId() {
        return id;
    }

    /**
     * Sets id with given id
     * @param id
     */
    public void setId(ObjectId id) {
        this.id = id;
    }

    /**
     * Get the user name
     * @return user name in form of a string
     */
    public String getName() {
        return name;
    }

    /**
     * Set the user name
     * @param name      the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get a Set of user Ids
     * @return A Set of User Ids
     */
    public Set<ObjectId> getMembers() {
        return members;
    }

    /**
     * Set user Ids for multiple users
     * @param members       the set of ids for multiple users
     */
    public void setMembers(Set<ObjectId> members) {
        this.members = members;
    }
}
