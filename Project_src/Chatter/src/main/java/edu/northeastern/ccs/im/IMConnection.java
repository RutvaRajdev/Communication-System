package edu.northeastern.ccs.im;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import javax.swing.SwingWorker;

/**
 * This class manages the connection between an the IM client and the IM server.
 * Instances of this class can be relied upon to manage all the details of this
 * connection and sends alerts when appropriate. Instances of this class must be
 * constructed and connected before it can be used to transmit messages.
 * 
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0
 * International License. To view a copy of this license, visit
 * http://creativecommons.org/licenses/by-sa/4.0/. It is based on work
 * originally written by Matthew Hertz and has been adapted for use in a class
 * assignment at Northeastern University.
 * 
 * @version 1.3
 */
public class IMConnection {

	final Logger logger = LoggerFactory.getLogger(IMConnection.class);

	/**
	 * Real Connection instance which this class wraps and makes presentable to the
	 * user
	 */
	private SocketNB socketConnection;

	/**
	 * List of instances that have registered as a listener for connection events.
	 */
	private ArrayList<LinkListener> linkListeners;

	/**
	 * List of instances that have registered as a listener for received message
	 * events.
	 */
	private ArrayList<MessageListener> messageListeners;

	/** Server to which this connection will be made. */
	private String hostName;

	/** Port to which this connection will be made. */
	private int portNum;

	/** Name of the user for which this connection was formed. */
	private String userName;

	/** Password of the user for which this connection was formed. */
	private String password;

	/**
	 * Holds the SwingWorker which is used to read and process all incoming data.
	 */
	private SwingWorker<Void, Message> workerBee;

	/** Instance used to read the messages. */
	private MessageScanner messageScanner;

	/**
	 * Creates an instance that will manage a connection with an IM server, but does
	 * not begin the process of making a connection to the IM server.
	 * 
	 * @param host     The name of the host that this connection is using
	 * @param port     The port number to use.
	 * @param username Name of the user for which this connection is being made.
	 */
	public IMConnection(String host, int port, String username, String pass) {
		if ((username == null) || username.trim().equals("")) {
			username = "TooDumbToEnterRealUsername";
		}
		linkListeners = new ArrayList<>();
		messageListeners = new ArrayList<>();
		userName = username;
		password = pass;
		hostName = host;
		portNum = port;
	}

	/**
	 * Add the given listener to be notified whenever 1 or more Messages are
	 * received from IM server via this connection.
	 * 
	 * @param listener Instance which will begin to receive notifications of any
	 *                 messages received by this IMConnection.
	 * @throws InvalidListenerException Exception thrown when this is called with a
	 *                                  value of null for {@code listener}
	 */
	public void addMessageListener(MessageListener listener) {
		if (listener == null) {
			throw new InvalidListenerException("Cannot add (null) as a listener!");
		}
		messageListeners.add(listener);
	}

	/**
	 * Send a message to log in to the IM server using the given username. For the
	 * moment, you will automatically be logged in to the server, even if there is
	 * already someone with that username.<br/>
	 * Precondition: connectionActive() == false
	 * 
	 * @throws IllegalNameException Exception thrown if we try to connect with an
	 *                              illegal username. Legal usernames can only
	 *                              contain letters and numbers.
	 * @return True if the connection was successfully made; false otherwise.
	 */
	public boolean connect(String action) {
		String name = getUserName();
		String pass = getPassword();
		checkIfRightValue(name);
		checkIfRightValue(pass);
		boolean retVal;
		if(action.equalsIgnoreCase("LOGIN")) {
			retVal = login(Message.MessageType.HELLO, name, pass);
		}
		else {
			retVal = login(Message.MessageType.REGISTER, name, pass);
		}
		MessageScanner rms = MessageScanner.getInstance();
		addMessageListener(rms);
		messageScanner = rms;
		return retVal;
	}

	/**
	 * Checks if the given value is a number or a digit, otherwise
	 * throws an IllegalNameException
	 * @param name	the value to be checked if right
	 * @throws IllegalNameException if the given name is not a letter or digit
	 */
	private void checkIfRightValue(String name) {
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (!Character.isLetter(ch) && !Character.isDigit(ch)) {
				throw new IllegalNameException("Cannot log in to the server using: " + name);
			}
		}
	}

	/**
	 * Returns whether the instance is managing an active, logged-in connection
	 * between the client and an IM server.
	 * 
	 * @return True if the client is logged in to the server using this connection;
	 *         false otherwise.
	 */
	public boolean connectionActive() {
		if (socketConnection == null) {
			return false;
		} else {
			return socketConnection.isConnected();
		}
	}

	/**
	 * Break this connection with the IM server. Once this method is called, this
	 * instance will need to be logged back in to the IM server to be usable.<br/>
	 * Precondition: connectionActive() == true
	 */
	public void disconnect() {
		Message quitMessage = Message.makeQuitMessage(getUserName());
		socketConnection.print(quitMessage);
		KeyboardScanner.close();
	}

	/**
	 * Gets an object which can be used to read what the user types in on the
	 * keyboard without waiting. The object returned by this method should be used
	 * rather than {@link Scanner} since {@code Scanner} will cause a program to
	 * halt if there is no input.
	 * 
	 * @return Instance of {@link KeyboardScanner} that can be used to read keyboard
	 *         input for this connection of the server.
	 */
	public KeyboardScanner getKeyboardScanner() {
		return KeyboardScanner.getInstance();
	}

	/**
	 * Gets an object which can be used to get the message sent by the server over
	 * this connection. This is the only object that can be used to retrieve all
	 * these messages.
	 * 
	 * @return Instance of {@link MessageScanner} that can be used to read message
	 *         sent over this connection for this user.
	 */
	public MessageScanner getMessageScanner() {
		if (messageScanner == null) {
			throw new IllegalOperationException("Cannot get a MessageScanner if you have not connected to the server!");
		}
		return messageScanner;
	}

	/**
	 * Get the name of the user for which we have created this connection.
	 * 
	 * @return Current value of the user name and/or the username with which we
	 *         logged in to this IM server.
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * Gets the password of the user for which we have created this connection.
	 *
	 * @return Current value of the password with which we logged in to this
	 * 		   IM server.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Unless this is a &quot;special&quot; server message, this sends the given
	 * message to all of the users logged in to the IM server. <br/>
	 * Precondition: connectionActive() == true
	 * 
	 * @param message Text of the message which will be broadcast to all users.
	 */
	public void sendMessage(String message) {
		if (!connectionActive()) {
			throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
		}
		Message bctMessage = Message.makeBroadcastMessage(userName, message);
		socketConnection.print(bctMessage);
	}

	/**
	 * Unless this is a &quot;special&quot; server message, this sends the given
	 * message to individual user logged in to the IM server. <br/>
	 * Precondition: connectionActive() == true
	 *
	 * @param message Text of the message which will be broadcast to all users.
	 * @param recName Receiver name
	 */
	public void sendMessageToIndividual(String message, String recName) {
		if (!connectionActive()) {
			throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
		}
		Message prvMessage = Message.makePersonalMessage(userName, message, recName);
		socketConnection.print(prvMessage);
	}


	/**
	 * Unless this is a &quot;special&quot; server message, this sends the given
	 * message to individual user logged in to the IM server. <br/>
	 * Precondition: connectionActive() == true
	 *
	 * @param encodedString Text of the message - Image converted to String to be send to indvidual user.
	 * @param recName Receiver name
	 */
	public void sendMessageWithImageToIndividual(String encodedString, String recName) {
		if (!connectionActive()) {
			throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
		}
		Message prvMessage = Message.makePersonalImageMessage(userName, encodedString, recName);
		socketConnection.print(prvMessage);
	}

	/**
	 * Unless this is a &quot;special&quot; server message, this sends the given
	 * message to group of users logged in to the IM server. <br/>
	 * Precondition: connectionActive() == true
	 *
	 * @param message Text of the message which will be broadcast to all users.
	 * @param grpName grpName
	 */
	public void sendMessageToGroup(String message, String grpName) {
		if (!connectionActive()) {
			throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
		}
		Message grpMessage = Message.makeGroupMessage(userName, message, grpName);
		socketConnection.print(grpMessage);
	}

	/**
	 * Unless this is a &quot;special&quot; server message, this sends the given
	 * Image message to group of users logged in to the IM server. <br/>
	 * Precondition: connectionActive() == true
	 *
	 * @param encodedString Text of the Image message which will be broadcast to all users of a group.
	 * @param grpName grpName
	 */
	public void sendMessageToGroupWithImage(String encodedString, String grpName) {
		if (!connectionActive()) {
			throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
		}
		Message grpMessage = Message.makeGroupImageMessage(userName, encodedString, grpName);
		socketConnection.print(grpMessage);
	}

	/**
	 * Unless this is a &quot;special&quot; server message, this sends the given
	 * message to prattle with details of users to be added in a group. <br/>
	 * Precondition: connectionActive() == true
	 *
	 * @param users List of users separated by space
	 * @param grpName grpName
	 */
	public void sendAddUserToGrpMessage(String grpName, String users) {
		if (!connectionActive()) {
			throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
		}
		Message grpMessage = Message.makeAddUsersToGrpMessage(userName, users, grpName);
		socketConnection.print(grpMessage);
	}

	/**
	 * Unless this is a &quot;special&quot; server message, this sends the given
	 * message to prattle with details of users to be removed from a group. <br/>
	 * Precondition: connectionActive() == true
	 *
	 * @param users List of users separated by space
	 * @param grpName grpName
	 */
	public void sendRemoveUserFromGrpMessage(String grpName, String users) {
		if (!connectionActive()) {
			throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
		}
		Message grpMessage = Message.makeRemoveUserFromGrpMessage(userName, users, grpName);
		socketConnection.print(grpMessage);
	}


	/**
	 * Sends the message to the server to update the username
	 * @param newUsername	the new user name to be sent
	 */
	public void sendUpdateUsernameMessage(String newUsername) {
		if (!connectionActive()) {
			throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
		}
		Message bctMessage = Message.makeUpdateUsernameMessage(userName, newUsername);
		socketConnection.print(bctMessage);
	}


	/**
	 * Sends the message to the server to create a group
	 * @param grpName	Name of the new group to be created
	 */
	public void sendCreateGrpMessage(String grpName) {
		if (!connectionActive()) {
			throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
		}
		Message createGrpMSG = Message.makeGroupCreationMessage(userName, grpName);
		socketConnection.print(createGrpMSG);
	}

    /**
     * Sends the message to the server to view users in a group
     * @param grpName	Name of the group
     */
    public void sendViewUsersMessage(String grpName) {
        if (!connectionActive()) {
            throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
        }
        Message usersInGrpMSG = Message.makeViewUsersInGroupMessage(userName, grpName);
        socketConnection.print(usersInGrpMSG);
    }

	/**
	 * Sends the message to the server to delete a group
	 * @param grpName	Name of the new group to be deleted
	 */
	public void sendDeleteGrpMessage(String grpName) {
		if (!connectionActive()) {
			throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
		}
		Message delGrpMSG = Message.makeGroupDeletionMessage(userName, grpName);
		socketConnection.print(delGrpMSG);
	}

	/**
	 * Sends the message to the server to update the password
	 * @param newPassword	the new password to be sent
	 */
	public void sendUpdatePasswordMessage(String newPassword) {
		if (!connectionActive()) {
			throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
		}
		Message bctMessage = Message.makeUpdatePasswordMessage(userName, newPassword);
		socketConnection.print(bctMessage);
	}

    /**
     * Sends the message to server to search messages by user name
     *
     * @param uName
     *
     *
     */
    public void sendSearchMessageByUName(String uName) {
        if (!connectionActive()) {
            throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
        }
        Message searchMessageUName = Message.makeSearchMsgByUNameMessage(userName, uName);
        socketConnection.print(searchMessageUName);
    }

	/**
	 * Sends the message to server to update parental control for a user
	 *
	 * @param uName
	 * @param choice
	 *
	 *
	 */
	public void sendUpdateParentalControlMsg(String uName, String choice) {
		if (!connectionActive()) {
			throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
		}
		Message pcMsg = Message.makeUpdateParentalControlMsg(userName, uName, choice);
		socketConnection.print(pcMsg);
	}


	/**
	 * Sends the message to server to search messages by time stamp for a user
	 *
	 * @param uName
	 */
	public void sendSearchMessageByTimeForUser(String uName, Date startTime, Date endTime) {
		if (!connectionActive()) {
			throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
		}
		Message searchMessageUName = Message.makeSearchMsgByTimeStampForUserMessage(userName, uName, startTime, endTime);
		socketConnection.print(searchMessageUName);
	}

	/**
	 * Sends the message to server to search messages by time stamp for a group
	 *
	 * @param grpName - group name
	 */
	public void sendSearchMessageByTimeForGroup(String grpName, Date startTime, Date endTime) {
		if (!connectionActive()) {
			throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
		}
		Message searchMessageUName = Message.makeSearchMsgByTimeStampForGroupMessage(userName, grpName, startTime, endTime);
		socketConnection.print(searchMessageUName);
	}


    /**
     * Sends the message to server to search messages by group name
     *
     * @param gName
     *
     *
     */
    public void sendSearchMessageByGName(String gName) {
        if (!connectionActive()) {
            throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
        }
        Message searchMessageGName = Message.makeSearchMsgByGNameMessage(userName, gName);
        socketConnection.print(searchMessageGName);
    }

	/**
	 * Sends the delete message
	 */
	public void sendDeleteMessage() {
		if (!connectionActive()) {
			throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
		}
		Message bctMessage = Message.makeDeleteMessage(userName);
		socketConnection.print(bctMessage);
	}

    /**
     * Sends the message to view user details
     */
    public void sendViewUserMessage() {
        if (!connectionActive()) {
            throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
        }
        Message usrViewMessage = Message.makeViewUserMessage(userName);
        socketConnection.print(usrViewMessage);
    }

	/**
	 * Sends the message to rename a group
	 */
	public void sendRenameGrpMessage(String GrpNameOld, String GrpNameNew) {
		if (!connectionActive()) {
			throw new IllegalOperationException("Cannot send a message if you are not connected to a server!\n");
		}
		Message grpRenameMessage = Message.makeGroupRenameMessage(userName, GrpNameOld, GrpNameNew);
		socketConnection.print(grpRenameMessage);
	}

	/**
	 * Checks the type of the message and creates a new message with HLO flag or REG flag
	 * @param messageType	the type of the message object to be created
	 * @param userName		the username with which you want to login/register
	 * @param password		the password with which you want to login/register
	 * @return				Message after adding the username and password to it
	 */
	private Message checkTypeAndCreateMessage(Message.MessageType messageType, String userName, String password, String ip) {
		if(messageType.equals(Message.MessageType.HELLO)) {
			String text = "PASSWORD--"+password+" "+"IP--"+ip;
			return Message.makeLoginMessage(userName, text);
		}
		else if(messageType.equals(Message.MessageType.REGISTER)) {
			String text = "PASSWORD--"+password+" "+"IP--"+ip;
			return Message.makeRegisterMessage(userName, text);
		}
		else {
			return null;
		}
	}

	/**
	 * Send a message to log in to the IM server using the given username. For the
	 * moment, you will automatically be logged in to the server, even if there is
	 * already someone with that username.<br/>
	 * Precondition: connectionActive() == false
	 * 
	 * @return True if the connection was successfully made; false otherwise.
	 */
	private boolean login(Message.MessageType messageType, String userName, String password) {
		// Now log in using this name.
		boolean result = true;
		try {
			socketConnection = new SocketNB(hostName, portNum);
			socketConnection.startIMConnection();
			// Create a new message to send
			Message message = checkTypeAndCreateMessage(messageType, userName, password, socketConnection.getChannel().getLocalAddress().toString().replace("/",""));
			// Send the message to log us into the system.
			socketConnection.print(message);
			// Create the background thread that handles our incoming messages.
			workerBee = new ScanForMessagesWorker(this, socketConnection);
			// Start the worker bee scanning for messages.
			workerBee.execute();
			// Return that we were successful
		} catch (IOException e) {
			// Report the error
			// And print out the problem
			logger.error(String.format("ERROR:  Could not make a connection to: %s at port %s", hostName, portNum), e);
			logger.error("If the settings look correct and your machine is connected to the Internet, report this error to Dr. Jump");
			// Return that the connection could not be made.
			result = false;
		}
		return result;
	}

	@SuppressWarnings({ "unchecked" })
	protected void fireSendMessages(List<Message> mess) {
		ArrayList<MessageListener> targets;
		synchronized (this) {
			targets = (ArrayList<MessageListener>) messageListeners.clone();
		}
		for (MessageListener iml : targets) {
			iml.messagesReceived(mess.iterator());
		}
	}

	@SuppressWarnings("unchecked")
	protected void fireStatusChange(String userName) {
		ArrayList<LinkListener> targets;
		synchronized (this) {
			targets = (ArrayList<LinkListener>) linkListeners.clone();
		}
		for (LinkListener iml : targets) {
			iml.linkStatusUpdate(userName, this);
		}
	}

	protected void loggedOut() {
		socketConnection = null;
	}
}