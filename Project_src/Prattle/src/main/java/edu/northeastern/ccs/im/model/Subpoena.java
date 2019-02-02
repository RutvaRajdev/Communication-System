package edu.northeastern.ccs.im.model;

import org.bson.types.ObjectId;

import java.util.Date;

/**
 * Subpoena is a law order to tap communication
 *      1. between 2 users or
 *      2. In a group
 *
 * This is a special kind of user with addition privileges
 *
 * @author rohanchitnis
 */
public class Subpoena {

    private ObjectId id;
    private String username;
    private String password;
    private String user1;
    private String user2;
    private String groupName;
    private Date fromTime;
    private Date toTime;
    private static final long serialVersionUID = 1L;

    /**
     * No-arg constructor
     */
    public Subpoena() {
        this.id = new ObjectId();
    }

    /**
     * Parameterized constructor
     */
    public Subpoena(String username, String password, String user1, String user2, String groupName, Date fromTime, Date toTime) {
        this.id = new ObjectId();
        this.username = username;
        this.password = password;
        this.user1 = user1;
        this.user2 = user2;
        this.groupName = groupName;
        this.fromTime = fromTime;
        this.toTime = toTime;
    }

    /**
     * @return          From timestamp
     */
    public Date getFromTime() {
        return fromTime;
    }

    /**
     * Sets the From timestamp
     */
    public void setFromTime(Date fromTime) {
        this.fromTime = fromTime;
    }

    /**
     * @return          To timestamp
     */
    public Date getToTime() {
        return toTime;
    }

    /**
     * Sets the To timestamp
     */
    public void setToTime(Date toTime) {
        this.toTime = toTime;
    }

    /**
     * @return          The id of the subpoena
     */
    public ObjectId getId() {
        return id;
    }

    /**
     * Sets the id of the subpoena
     * @param id        given id
     */
    public void setId(ObjectId id) {
        this.id = id;
    }

    /**
     * @return          username of this subpoena
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets username of this subpoena
     * @param username  Given username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return          Password of this subpoena
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets password of this subpoena
     * @param password  Given password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return          first user in the communication to be tapped
     */
    public String getUser1() {
        return user1;
    }

    /**
     * Sets the first user in the communication to be tapped
     * @param user1     given username
     */
    public void setUser1(String user1) {
        this.user1 = user1;
    }

    /**
     * @return          second user in the communication to be tapped
     */
    public String getUser2() {
        return user2;
    }

    /**
     * Sets the second user in the communication to be tapped
     * @param user2     given username
     */
    public void setUser2(String user2) {
        this.user2 = user2;
    }

    /**
     * @return          Name of the group whose communication is to be tapped
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Sets the group name whose communication is to be tapped
     * @param groupName Given groupName
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
