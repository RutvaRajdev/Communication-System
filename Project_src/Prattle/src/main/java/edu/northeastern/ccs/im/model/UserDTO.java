package edu.northeastern.ccs.im.model;

/**
 * The DTO class for User that contains user data such as name, email, phone and password and
 * provides corresponding methods to extract them
 *
 * @author saurabh
 *
 * Date: 11-04-2018
 */
public class UserDTO {

    private String name;
    private String email;
    private String phone;
    private String password;

    /**
     * A default constructor that creates a user DTO object out
     * of given name, email, phone and password
     * @param name          the name of the user
     * @param email         the email address of the user
     * @param phone         the phone number of the user
     * @param password      the password of the user
     */
    public UserDTO(String name, String email, String phone, String password) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

    /**
     * Returns the name of user
     * @return User Name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the email of a user
     * @return User email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the user phone number
     * @return Phone number of user
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Returns the password of user
     * @return Password
     */
    public String getPassword() {
        return password;
    }
}

