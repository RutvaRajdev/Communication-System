package edu.northeastern.ccs.im.model;

import org.bson.types.ObjectId;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.security.Key;
import java.util.*;

/**
 * User class that is used to set and get user parameters such as id, name, email, etc.
 *
 * @author Team-103
 *
 * Date: 11-04-2018
 */
public class User implements Serializable {

    private ObjectId id;
    private String name;
    private String email;
    private String phone;
    private String password;
    private String ip;
    private boolean parentalControl;

    private static final long serialVersionUID = 1L;
    private Set<ObjectId> groupIds = new HashSet<>();

    /**
     * Default constructor that sets a random ID for user
     */
    public User() {
        this.id = new ObjectId();
    }

    /**
     * Parameterised constructor that sets the given name
     * and password for user
     * @param name          the name to set
     * @param password      the password to set
     */
    public User(String name, String password) {
        this.id = new ObjectId();
        this.name = name;
        this.password = password;
    }

    /**
     * Parameterised constructor that sets the
     * given name, email, phone and password for user
     * @param name          the name to set
     * @param email         the email to set
     * @param phone         the phone number to set
     * @param password      the password to set
     * @param ip            the ip address
     */
    public User(String name, String email, String phone, String password, String ip) {
        this.id = new ObjectId();
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.ip = ip;
    }

    /**
     * @return              parental control flag
     */
    public boolean isParentalControl() {
        return parentalControl;
    }

    /**
     * Sets the parental control flag
     * @param parentalControl   Given flag
     */
    public void setParentalControl(boolean parentalControl) {
        this.parentalControl = parentalControl;
    }

    /**
     * @return  IP address of the user
     */
    public String getIp() {
        return ip;
    }

    /**
     * Sets the ip with given ip
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * @return id of the User
     */
    public ObjectId getId() {
        return id;
    }

    /**
     * Sets the id of the user with given id
     */
    public void setId(ObjectId id) {
        this.id = id;
    }

    /**
     * Returns the user name
     * @return User Name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user name as per given string
     * @param name      the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the user email
     * @return user email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the User Email
     * @param email     the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the user login password
     * @return User login Password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password for user for login
     * @param password      the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the Phone number of user
     * @return Phone number of user
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the user's phone number
     * @param phone     the phone number to set
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Returns all IDs of the groups the user belongs to
     * @return set of group IDs
     */
    public Set<ObjectId> getGroupIds() {
        return groupIds;
    }

    /**
     * Sets the groups the users belongs to, by passing their IDs
     * @param groupIds      the set of group IDs to set
     */
    public void setGroupIds(Set<ObjectId> groupIds) {
        this.groupIds = groupIds;
    }

    private String decryptPassword(String withEncryption) {
        try {
            Key key = new SecretKeySpec("varadrutvasaurab".getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decoded = Base64.getDecoder().decode(withEncryption);
            return new String(cipher.doFinal(decoded));
        }catch (Exception exc) { return null; }
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", password='" + decryptPassword(password) + '\'' +
                '}';
    }
}

