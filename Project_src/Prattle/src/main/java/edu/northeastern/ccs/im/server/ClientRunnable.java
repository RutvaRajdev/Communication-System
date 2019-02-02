package edu.northeastern.ccs.im.server;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.northeastern.ccs.im.Message;
import edu.northeastern.ccs.im.PrintNetNB;
import edu.northeastern.ccs.im.ScanNetNB;
import edu.northeastern.ccs.im.exception.GroupAlreadyExistsException;
import edu.northeastern.ccs.im.model.*;
import edu.northeastern.ccs.im.service.GroupService;
import edu.northeastern.ccs.im.service.MessageService;
import edu.northeastern.ccs.im.service.SubpeonaServices;
import edu.northeastern.ccs.im.service.UserService;
import net.gpedro.integrations.slack.SlackMessage;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bson.types.ObjectId;

/**
 * Instances of this class handle all of the incoming communication from a
 * single IM client. Instances are created when the client signs-on with the
 * server. After instantiation, it is executed periodically on one of the
 * threads from the thread pool and will stop being run only when the client
 * signs off.
 * 
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0
 * International License. To view a copy of this license, visit
 * http://creativecommons.org/licenses/by-sa/4.0/. It is based on work
 * originally written by Matthew Hertz and has been adapted for use in a class
 * assignment at Northeastern University.
 * 
 * @version 1.3
 */
public class ClientRunnable implements Runnable {
	/**
	 * Number of milliseconds that special responses are delayed before being sent.
	 */
	private static final int SPECIAL_RESPONSE_DELAY_IN_MS = 5000;

	/**
	 * Number of milliseconds after which we terminate a client due to inactivity.
	 * This is currently equal to 5 hours.
	 */
	private static final long TERMINATE_AFTER_INACTIVE_BUT_LOGGEDIN_IN_MS = 100000000;

	/**
	 * Number of milliseconds after which we terminate a client due to inactivity.
	 * This is currently equal to 5 hours.
	 */
	private static final long TERMINATE_AFTER_INACTIVE_INITIAL_IN_MS = 600000;

	/**
	 * The success flag ack to send to the client
	 */
	private static final String SUCCESS = "SUCCESS";

	/**
	 * The fail flag ack to send to the client
	 */
	private static final String FAIL = "FAIL";

	private static final String DATE_FORMAT = "EEE MMM dd hh:mm:ss z yyyy";

	private static final String DATE_ERROR_MESSAGE = "Could not parse date time ";

	/**
	 * Time at which we should send a response to the (private) messages we were
	 * sent.
	 */
	private Date sendResponses;

	/**
	 * The user service to talk with the database
	 */
	private UserService userService = Prattle.getUserService();

	/**
	 * The group service to talk with the database
	 */
	private GroupService groupService = Prattle.getGroupService();

	/**
	 * The message service to talk with the database
	 */
	private MessageService messageService = Prattle.getMessageService();

	private SubpeonaServices subpeonaServices = Prattle.getSubpoenaService();

	/** Time at which the client should be terminated due to lack of activity. */
	private GregorianCalendar terminateInactivity;

	/** Queue of special Messages that we must send immediately. */
	private Queue<Message> immediateResponse;

	/** Queue of special Messages that we will need to send. */
	private Queue<Message> specialResponse;

	/** Socket over which the conversation with the single client occurs. */
	private final SocketChannel socket;

	private boolean isSubpoenaUser;

	/**
	 * Utility class which we will use to receive communication from this client.
	 */
	private ScanNetNB input;

	/** Utility class which we will use to send communication to this client. */
	private PrintNetNB output;

	/** Id for the user for whom we use this ClientRunnable to communicate. */
	private int userId;

	/** Name that the client used when connecting to the server. */
	private String name;

	/** Pass that the client used when connecting to the server. */
	private String pwd;

	private static final String LOGGING = "LOGGING";
	private static final String LOGGING_LEVEL = "LOGGING_LEVEL";

	/**
	 * Flag to check if message sent to single person for testing
	 */
	private boolean oneToOne;

	/**
	 * Flag to check if the message sent to group for testing
	 */
	private boolean toGroup;

	/**
	 * Flag to check if message sent as broadcast
	 */
	private boolean broadcastFlag;

	/**
	 * Whether this client has been initialized, set its user name, and is ready to
	 * receive messages.
	 */
	private boolean initialized;

	/**
	 * Flag to store which message has been sent to the server
	 */
	private String whichMessageSent;

	/**
	 * The future that is used to schedule the client for execution in the thread
	 * pool.
	 */
	private ScheduledFuture<ClientRunnable> runnableMe;

	/** Collection of messages queued up to be sent to this client. */
	private Queue<Message> waitingList;

	/** The logger of this class */
	private static Logger logger = Logger.getLogger(ClientRunnable.class.getName());

	/**
	 * Flag to terminate
	 */
	private boolean terminate;

	/**
	 * Flag is set to true if there is a timeout
	 */
	private boolean shouldTerminate;

	/**
	 * Create a new thread with which we will communicate with this single client.
	 * 
	 * @param client SocketChannel over which we will communicate with this new
	 *               client
	 * @throws IOException Exception thrown if we have trouble completing this
	 *                     connection
	 */
	public ClientRunnable(SocketChannel client) throws IOException {
		// Set up the SocketChannel over which we will communicate.
		socket = client;
		socket.configureBlocking(false);
		// Create the class we will use to receive input
		input = new ScanNetNB(socket);
		// Create the class we will use to send output
		output = new PrintNetNB(socket);
		// Mark that we are not initialized
		initialized = false;
		// Create our queue of special messages
		specialResponse = new LinkedList<>();
		// Create the queue of messages to be sent
		waitingList = new ConcurrentLinkedQueue<>();
		// Create our queue of message we must respond to immediately
		immediateResponse = new LinkedList<>();
		// Mark that the client is active now and start the timer until we
		// terminate for inactivity.
		terminateInactivity = new GregorianCalendar();
		// To terminate
		terminate = false;
		// Sets empty message
		whichMessageSent = "";
		// Flag to check if the client should terminte now or not
		shouldTerminate = false;
		// Set to false just for testing
		oneToOne = false;
		// Set to false just for testing
		toGroup = false;
		// Initialize name as empty
		name = "";
		isSubpoenaUser = false;
		// Set to false just for testing
		broadcastFlag = false;
		// Initialize the hashmap to read/write users in this system
		terminateInactivity
				.setTimeInMillis(terminateInactivity.getTimeInMillis() + TERMINATE_AFTER_INACTIVE_INITIAL_IN_MS);
	}

	/**
	 * Determines if this is a special message which we handle differently. It will
	 * handle the messages and return true if msg is "special." Otherwise, it
	 * returns false.
	 * 
	 * @param msg Message in which we are interested.
	 * @return True if msg is "special"; false otherwise.
	 */
	private boolean broadcastMessageIsSpecial(Message msg, List<Message> responses) {
		boolean result = false;
		String text = msg.getText();
		if (text != null) {
			if(responses.isEmpty()) {responses = ServerConstants.getBroadcastResponses(text);}
			if (responses != null) {
				for (Message current : responses) {
					handleSpecial(current);
				}
				result = true;
			}
		}
		return result;
	}

	/**
	 * Check to see for an initialization attempt and process the message sent.
	 */
	private void checkForInitialization() {
		// Check if there are any input messages to read
		if (input.hasNextMessage()) {
			// If a message exists, try to use it to initialize the connection
			Message msg = input.nextMessage();
			proceedIfRightCredentials(msg);
		}
	}

	/**
	 * Checks the username and the password from the Message and proceeds
	 * for initialization only if they are set
	 * @param msg		the message containing username and the password
	 */
	private void proceedIfRightCredentials(Message msg) {
		if(msg.getText() != null) {
			String[] text = msg.getText().split(" ");
			String password = text[0].split("--")[1];
			if (setUserName(msg.getName()) && setPassword(password)) {
				// Update the time until we terminate this client due to inactivity.
				terminateInactivity.setTimeInMillis(
						new GregorianCalendar().getTimeInMillis() + TERMINATE_AFTER_INACTIVE_INITIAL_IN_MS);
				// Set that the client is initialized.
				setInitialization(msg);
				sendAckToSubpoenaUser(msg);
				sendAcknowledgement(msg);
			}
		}
	}

	/**
	 * Sets initialized flag to true if and only if the user is found in the
	 * database or if it is a REG message, otherwise returns false
	 * @param msg	the message to read the user from
	 */
	private void setInitialization(Message msg) {
		if(msg.isInitialization()) {
			String [] text = msg.getText().split(" ");
			String password = text[0].split("--")[1];
			initialized = isSubpoenaUser(msg.getName(), password);
			if(!initialized) {
				initialized = isUserPresent(msg.getName(), password);
			}
			else {
				isSubpoenaUser = true;
			}
		}
		else if(msg.isRegistrationMessage()) {
			initialized = true;
		}
		else {
			initialized = false;
		}
	}

	private boolean isSubpoenaUser(String uname, String password) {
		return subpeonaServices.findSubpeona(uname, decryptEndToEnd(password)) != null;
	}

	/**
	 * Sends broadcast message if the login was successful, or if it failed
	 * @param msg	the message to read and accordingly send the message
	 */
	private void sendAcknowledgement(Message msg) {
		String [] text = msg.getText().split(" ");
		String password = text[0].split("--")[1];
		String ip = text[1].split("--")[1];
		if(initialized && msg.isInitialization() && !isSubpoenaUser) {
			sendMessage(Message.makeBroadcastMessage(name,"LOGIN_SUCCESSFUL"));
			userService.updateIP(msg.getName(), ip);
			this.whichMessageSent = "LOGIN_SUCCESSFUL";
		}
		else if(initialized && msg.isRegistrationMessage() && !isSubpoenaUser) {
			if(registerUser(msg.getName(), password)) {
				sendMessage(Message.makeBroadcastMessage(name,"REGISTRATION_SUCCESSFUL"));
				userService.updateIP(msg.getName(), ip);
				this.whichMessageSent = "REGISTRATION_SUCCESSFUL";
			}
			else {
				sendMessage(Message.makeBroadcastMessage(name,"REGISTRATION_FAILED"));
				Prattle.getSlack().call(new SlackMessage("@"+name+" REGISTRATION FAILED AS USER EXISTS"));
				this.whichMessageSent = "REGISTRATION_FAILED";
			}
		}
		else {
			if(!isSubpoenaUser) {
				sendMessage(Message.makeBroadcastMessage(name, "LOGIN_FAILED"));
				Prattle.getSlack().call(new SlackMessage("@"+name+" LOGIN FAILED"));
				this.whichMessageSent = "LOGIN_FAILED";
			}
		}
	}

	private void sendAckToSubpoenaUser(Message msg) {
		if(initialized && msg.isInitialization() && isSubpoenaUser) {
			sendMessage(Message.makeBroadcastMessage(name,"LOGIN_SUCCESSFUL SUBPOENA"));
		}
	}

	/**
	 * Looks into the database with the given username and password
	 * @param username	the username to check in the database
	 * @param password	the password to check in the database
	 * @return			true if the user was found, otherwise false
	 */
	private boolean isUserPresent(String username, String password) {
	    return userService.findUserWithCredentials(username, decryptEndToEnd(password)) != null;
	}

	/**
	 * Gets the Hex encoded string from the server and decodes it to extract
	 * the original password string
	 * @param password		the password to be decoded
	 * @return				the decoded password
	 */
	private String decryptEndToEnd(String password) {
		try {
			return new String(Hex.decodeHex(password.toCharArray()));
		}catch (DecoderException dex) {
			if ("ON".equals(System.getenv(LOGGING))) {
				logger.log(Level.parse(System.getenv(LOGGING_LEVEL)), "Could not decode");
			}
			Prattle.getSlack().call(new SlackMessage("End-to-end Decryption failed "+password));
			return null;
		}
	}

	/**
	 * Process one of the special responses
	 * 
	 * @param msg Message to add to the list of special responses.
	 */
	private void handleSpecial(Message msg) {
		if (specialResponse.isEmpty()) {
			sendResponses = new Date();
			sendResponses.setTime(sendResponses.getTime() + SPECIAL_RESPONSE_DELAY_IN_MS);
		}
		specialResponse.add(msg);
	}

	/**
	 * Check if the message is properly formed. At the moment, this means checking
	 * that the identifier is set properly.
	 * 
	 * @param msg Message to be checked
	 * @return True if message is correct; false otherwise
	 */
	private boolean messageChecks(Message msg) {
		// Check that the message name matches.
		if (msg != null && getName() != null) {
			return (msg.getName() != null) && (msg.getName().compareToIgnoreCase(getName()) == 0);
		}
		return false;
	}

	/**
	 * Takes the username and password and registers the user by adding it
	 * to the hashmap and returning true
	 * @param username		the username to be registered
	 * @param password		the password to be registered
	 * @return 				true if the user was successfully added to the
	 * 						database, false if the user was already present
	 * 						in the database
	 */
	private boolean registerUser(String username, String password) {
        try {
            userService.createUser(new UserDTO(username, "", "", decryptEndToEnd(password)));
            return true;
        }catch (Exception exc) {
            return false;
        }
	}

	/**
	 * Immediately send this message to the client. This returns if we were
	 * successful or not in our attempt to send the message.
	 * 
	 * @param message Message to be sent immediately.
	 * @return True if we sent the message successfully; false otherwise.
	 */
	private boolean sendMessage(Message message) {
		return output.print(message);
	}

	/**
	 * Try allowing this user to set his/her user name to the given username.
	 * 
	 * @param userName The new value to which we will try to set userName.
	 * @return True if the username is deemed acceptable; false otherwise
	 */
	private boolean setUserName(String userName) {
		// Now make sure this name is legal.
		if (userName != null) {
			// Optimistically set this users ID number.
			setName(userName);
			userId = hashCode();
			return true;
		}
		// Clear this name; we cannot use it. *sigh*
		userId = -1;
		return false;
	}

	/**
	 * Sets the given password to the instance variable
	 * @param password		the password to be set
	 * @return				true if the password is not null
	 */
	private boolean setPassword(String password) {
		if(password != null) {
			setPass(decryptEndToEnd(password));
			return true;
		}
		return false;
	}

	/**
	 * Add the given message to this client to the queue of message to be sent to
	 * the client.
	 * 
	 * @param message Complete message to be sent.
	 */
	public void enqueueMessage(Message message) {
		waitingList.add(message);
	}

	/**
	 * Get the name of the user for which this ClientRunnable was created.
	 * 
	 * @return Returns the name of this client.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the user for which this ClientRunnable was created.
	 * 
	 * @param name The name for which this ClientRunnable.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Sets the given password
	 * @param password	the password to be set
	 */
	public void setPass(String password) {
		this.pwd = password;
	}

	/**
	 * Gets the name of the user for which this ClientRunnable was created.
	 * 
	 * @return Returns the current value of userName.
	 */
	public int getUserId() {
		return userId;
	}

	/**
	 * Return if this thread has completed the initialization process with its
	 * client and is read to receive messages.
	 * 
	 * @return True if this thread's client should be considered; false otherwise.
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * Perform the periodic actions needed to work with this client.
	 * 
	 * @see java.lang.Thread#)
	 */
	public void run() {

		// The client must be initialized before we can do anything else
		if (!initialized) {
			checkForInitialization();
		} else {
			try {
				// Client has already been initialized, so we should first check
				// if there are any input
				// messages.
				if (input.hasNextMessage()) {
					// Get the next message
					Message msg = input.nextMessage();
					// Update the time until we terminate the client for
					// inactivity.
					terminateInactivity.setTimeInMillis(
							new GregorianCalendar().getTimeInMillis() + TERMINATE_AFTER_INACTIVE_BUT_LOGGEDIN_IN_MS);
					listenForCRUDMessages(msg);
					// If the message is a broadcast message, send it out
					showAllBroadcastedMessages(msg);
					// Handle one to one and group messages
					handleCustomMessage(msg);

				}
				performImmediateResponseIfRequired();
				performProcessSpecialIfRequired();
			} finally {
				// When it is appropriate, terminate the current client.
				if (terminate) {
					if ("ON".equals(System.getenv(LOGGING))) {
						logger.log(Level.parse(System.getenv(LOGGING_LEVEL)), this.pwd);
					}
					terminateClient();
				}
			}
		}
		// Finally, check if this client have been inactive for too long and,
		// when they have, terminate
		// the client.
		terminateClientIfTimeout();
	}

	/**
	 * Terminate client if there is timeout
	 */
	private boolean terminateClientIfTimeout() {
		shouldTimeout();
		if (!terminate && this.shouldTerminate) {
			String timeoutMessage = "Timing out or forcing off a user " + name;

			if ("ON".equals(System.getenv(LOGGING))) {
				logger.warning(timeoutMessage);
			}
			terminateClient();
			return true;
		}
		return false;
	}

	/**
	 * Returns true if the user is idle for given time
	 * @return		true if the user is idle for given time
	 */
	public void shouldTimeout() {
		if(!this.shouldTerminate) {
			this.shouldTerminate = terminateInactivity.before(new GregorianCalendar());
		}
	}

	/**
	 * Store the object used by this client runnable to control when it is scheduled
	 * for execution in the thread pool.
	 * 
	 * @param future Instance controlling when the runnable is executed from within
	 *               the thread pool.
	 */
	public void setFuture(ScheduledFuture<ClientRunnable> future) {
		runnableMe = future;
	}

	/**
	 * Terminate a client that we wish to remove. This termination could happen at
	 * the client's request or due to system need.
	 */
	public void terminateClient() {
		try {
			// Once the communication is done, close this connection.
			input.close();
			socket.close();
		} catch (IOException e) {
			// If we have an IOException, ignore the problem
			Prattle.getSlack().call(new SlackMessage("Termination of client failed: "+e.getMessage()));
			if ("ON".equals(System.getenv(LOGGING))) {
				logger.warning(e.toString());
			}
		} finally {
			// Remove the client from our client listing.
			Prattle.removeClient(this);
			// And remove the client from our client pool.
			runnableMe.cancel(false);
		}
	}

	/**
	 * Removes the given user from the hashmap
	 * @param username	the username of the user
	 */
	private boolean deleteUser(String username) {
		return userService.deleteUser(username) != null;
	}

	/**
	 * Takes the old user name and the new user name and changes
	 * the old user name with the new one in the hashmap
	 * @param oldName		the old username to look for
	 * @param newName		the new username to change with
	 */
	private boolean updateUsername(String oldName, String newName) {
		return (userService.updateUser(new UserDTO(oldName, "", "", this.pwd), new UserDTO(newName, "", "", this.pwd)) != null);
	}

	/**
	 * Takes the old user name and the new password and changes
	 * the old password with the new one in the hashmap
	 * @param name			the old username to look for
	 * @param newPassword	the new password to change with
	 */
	private boolean updatePassword(String name, String newPassword) {
		return userService.updateUser(new UserDTO(name, "", "", this.pwd), new UserDTO(name, "", "", newPassword)) != null;
	}

	/**
	 * Listens for the CRUD requests from the client and acts
	 * accordingly
	 * @param msg		the received messages
	 */
	private void listenForCRUDMessages(Message msg) {
		if(msg.isDeleteMessage()) {
			performDelete(msg);
		}
		if (msg.isUpdateUsernameType()) {
			performUsernameUpdate(msg);
		}
		if (msg.isUpdatePasswordType()) {
			performPasswordUpdate(msg);
		}
		if (msg.isViewUserDMessage()) {
			getUserDetails(msg);
		}
		if (msg.isCreateGroupMessage()) {
			createGroup(msg);
		}
		if (msg.isDeleteGroupMessage()) {
			deleteGroup(msg);
		}
		if (msg.isAddUsersToGroupMessage()) {
			addUsersToGroup(msg);
		}
		if (msg.isRemoveUsersFromGroupMessage()) {
			removeUserFromGroup(msg);
		}
		if (msg.isGroupRenameMessage()) {
			renameGroup(msg);
		}
		if (msg.isViewUsersInGroupMessage()) {
			getAllUsersInGroup(msg);
		}
		if(msg.isSearchMsgByUNameMsg()) {
			searchUserMessageByReceiverUsername(msg);
		}
		if(msg.isSearchMsgByGNameMsg()) {
		    List<SingleMessage> searchedMessages = messageService.findMessage(msg.getText()).getMessages();
            sendSearchedMessagesForUserBackToClient(msg.getName(), searchedMessages);
		}
		if(msg.ismakeSearchMsgByTimeStampForUserMessage()) {
			searchUserMessagesByTimeStamp(msg);
		}
		if(msg.ismakeSearchMsgByTimeStampForGroupMessage()) {
			searchGroupMessagesByTimeStamp(msg);
		}
		if(msg.isUpdatePCMsg()) {
			toggleParentalControl(msg);
		}
	}

	private void toggleParentalControl(Message msg) {
		if(msg.getText().equalsIgnoreCase("ON")) {
			User user = userService.findUserWithUserName(msg.getName());
			user.setParentalControl(true);
			userService.updateUser(user, user);
			sendMessage(Message.makeBroadcastMessage(msg.getName(), "PARENTAL_ON"));
		} else {
			User user = userService.findUserWithUserName(msg.getName());
			user.setParentalControl(false);
			userService.updateUser(user, user);
			sendMessage(Message.makeBroadcastMessage(msg.getName(), "PARENTAL_OFF"));
		}
	}

	/**
	 * Searches the group messages between the given time stamp
	 * @param msg		the message from the client
	 */
	private void searchGroupMessagesByTimeStamp(Message msg) {
		String [] params = msg.getText().split(", ");
		String receiverName = params[0];
		String startTime = params[1];
		String endTime = params[2];
		try {
			Date start = new SimpleDateFormat(DATE_FORMAT).parse(startTime);
			Date end = new SimpleDateFormat(DATE_FORMAT).parse(endTime);
			List<SingleMessage> allMessages = messageService.searchMessages(start, end, receiverName);
			sendSearchedMessagesForUserBackToClient(msg.getName(), allMessages);
		} catch (ParseException exc) {
			if ("ON".equals(System.getenv(LOGGING))) {
				logger.log(Level.parse(System.getenv(LOGGING_LEVEL)), DATE_ERROR_MESSAGE+startTime+" "+endTime);
			}
			Prattle.getSlack().call(new SlackMessage(DATE_ERROR_MESSAGE+startTime+" "+endTime));
			sendSearchedMessagesForUserBackToClient(msg.getName(), new ArrayList<>());
		}
	}

	/**
	 * Searches the message for the requested username by the given
	 * timestamp
	 * @param msg	the message from the chatter
	 */
	private void searchUserMessagesByTimeStamp(Message msg) {
		if(subpeonaServices.findByName(msg.getName()) != null) {
			sendChatHistoryToSubpoenaUser(msg);
		}
		else {
			String[] params = msg.getText().split(", ");
			String receiverName = params[0];
			String startTime = params[1];
			String endTime = params[2];
			String groupName = msg.getName() + "$" + receiverName;
			if (groupName.contains("$") && (groupName.indexOf('$') == groupName.lastIndexOf('$'))) {
				String groupName2 = groupName.split("\\$")[1] + "$" + groupName.split("\\$")[0];
				parseDatesAndSendMessage(startTime, endTime, groupName, groupName2, msg);
			}
		}
	}

	private void parseDatesAndSendMessage(String startTime, String endTime, String groupName, String groupName2, Message msg) {
		List<SingleMessage> chatMessage;
		try {
			Date start = new SimpleDateFormat(DATE_FORMAT).parse(startTime);
			Date end = new SimpleDateFormat(DATE_FORMAT).parse(endTime);
			List<SingleMessage> chatMessage1 = messageService.searchMessages(start, end, groupName);
			chatMessage = chatMessage1.isEmpty() ? messageService.searchMessages(start, end, groupName2) : chatMessage1;
			sendSearchedMessagesForUserBackToClient(msg.getName(), chatMessage);
		} catch (ParseException exc) {
			if ("ON".equals(System.getenv(LOGGING))) {
				logger.log(Level.parse(System.getenv(LOGGING_LEVEL)), DATE_ERROR_MESSAGE+startTime+" "+endTime);
			}
			Prattle.getSlack().call(new SlackMessage(DATE_ERROR_MESSAGE + startTime + " " + endTime));
			sendSearchedMessagesForUserBackToClient(msg.getName(), new ArrayList<>());
		}
	}

	/**
	 * Searches the messages for the requested username by the given
	 * two users
	 * @param msg		the message from the client
	 */
	private void searchUserMessageByReceiverUsername(Message msg) {
		String groupName = msg.getName()+"$"+msg.getText();
		ChatMessage chatMessage;
		if (groupName.contains("$") && (groupName.indexOf('$') == groupName.lastIndexOf('$'))) {
			String groupName2 = groupName.split("\\$")[1] + "$" + groupName.split("\\$")[0];
			ChatMessage chatMessage1 = messageService.findMessage(groupName);
			chatMessage = chatMessage1 == null ? messageService.findMessage(groupName2) : chatMessage1;
			List<SingleMessage> searchedMessages;
			if(chatMessage == null) {
				searchedMessages = new ArrayList<>();
			} else {
				searchedMessages = chatMessage.getMessages();
			}
			sendSearchedMessagesForUserBackToClient(msg.getName(), searchedMessages);

		} else {
			sendSearchedMessagesForUserBackToClient(msg.getName(), new ArrayList<>());
		}
	}

	/**
	 * Takes the message and returns all the members from the specified
	 * group
	 * @param msg	the message from the client to show group members
	 */
	private void getAllUsersInGroup(Message msg) {
		ChatGroup chatGroup = groupService.findGroupWithGroupName(msg.getText());
		if(chatGroup != null) {
			Set<ObjectId> members = chatGroup.getMembers();
			StringBuilder payload = new StringBuilder("");
			for(ObjectId objectId : members) {
				User user = userService.findById(objectId);
				if(user != null) {
					payload.append(user.getName());
					payload.append(" ");
				}
			}
			sendListOfUsersFromGroup(payload.toString(),msg);
		}
		else {
			sendListOfUsersFromGroup("",msg);
		}
	}

	/**
	 * Takes the given string builder and message and sends the string builder
	 * to the client
	 * @param payload		the list of users
	 * @param msg			the message from the client
	 */
	private void sendListOfUsersFromGroup(String payload, Message msg) {
		if(!payload.equalsIgnoreCase("")) {
			sendMessage(Message.makeBroadcastMessage(msg.getName(), payload));
		}
		else {
			sendMessage(Message.makeBroadcastMessage(msg.getName(), FAIL));
		}
	}

    /**
     * Sends all the searched messages back to the requester one by one
     * @param receiverName      the name of the user who requested
     * @param allMessages       list of all the messages retrieved from the DB
     */
	private void sendSearchedMessagesForUserBackToClient(String receiverName, List<SingleMessage> allMessages) {
	    if(!allMessages.isEmpty()) {
            for(SingleMessage message : allMessages) {
                String retrievedMessage = message.getSender() +" : "+message.getTimestamp()+" "+message.getMessage();
                sendMessage(Message.makeBroadcastMessage(receiverName, retrievedMessage));
            }
            sendMessage(Message.makeBroadcastMessage(receiverName, "DONE SEARCHING"));
        } else {
	        sendMessage(Message.makeBroadcastMessage(receiverName, FAIL));
        }
    }

	/**
	 * @param msg 		the request message from client to show user details
	 */
	private void getUserDetails(Message msg) {
		User user = userService.findUserWithUserName(msg.getName());
		if(user != null) {
			sendMessage(Message.makeBroadcastMessage(msg.getName(), user.toString()));
		}
		else {
			sendMessage(Message.makeBroadcastMessage(msg.getName(), FAIL));
		}
	}

	/**
	 * Checks if group is present, otherwise creates the group
	 * @param msg		the message containing the group name
	 */
	private void createGroup(Message msg) {
		try {
			groupService.createGroup(msg.getName(), msg.getText());
			sendMessage(Message.makeBroadcastMessage(msg.getName(),SUCCESS));
		} catch (GroupAlreadyExistsException exc) {
			if ("ON".equals(System.getenv(LOGGING))) {
				logger.log(Level.parse(System.getenv(LOGGING_LEVEL)), "Could not create group as it already exists");
			}
			Prattle.getSlack().call(new SlackMessage("Could not create group as it already exists"));
			sendMessage(Message.makeBroadcastMessage(msg.getName(),FAIL));
		}
	}

	/**
	 * Checks if the given group name exists, then adds the given list of users to the group
	 * @param msg
	 */
	private void addUsersToGroup(Message msg) {
		String [] parsed = msg.getText().split(":");
		String groupName = parsed[0];
		String [] listOfUsers = parsed[1].split(" ");
		ChatGroup group = groupService.findGroupWithGroupName(groupName);
		if(group != null) {
			for(String user : listOfUsers) {
				groupService.addUserToGroup(group, user);
			}
			sendMessage(Message.makeBroadcastMessage(msg.getName(), SUCCESS));
		}
		else {
			sendMessage(Message.makeBroadcastMessage(msg.getName(), FAIL));
		}
	}

	/**
	 * Checks if group exists, then removes user from the group
	 * @param msg		the group name and the list of user to be removed
	 */
	private void removeUserFromGroup(Message msg) {
		String [] parsed = msg.getText().split(":");
		String groupName = parsed[0];
		String [] listOfUsers = parsed[1].split(" ");
		ChatGroup group = groupService.findGroupWithGroupName(groupName);
		if(group != null) {
			for(String user : listOfUsers) {
				groupService.removeUserFromGroup(group, user);
			}
			sendMessage(Message.makeBroadcastMessage(msg.getName(), SUCCESS));
		} else {
			sendMessage(Message.makeBroadcastMessage(msg.getName(), FAIL));
		}
	}

	/**
	 * Checks if the given group is present, then deletes it
	 * @param msg		the message from the client
	 */
	private void deleteGroup(Message msg) {
		String groupName = msg.getText();
		if(groupService.deleteGroup(groupName) != null) {
			sendMessage(Message.makeBroadcastMessage(msg.getName(), SUCCESS));
		} else {
			sendMessage(Message.makeBroadcastMessage(msg.getName(), FAIL));
		}
	}

	/**
	 * Checks if the given group exists, then renames it
	 * @param msg		the message from the client
	 */
	private void renameGroup(Message msg) {
		String [] parsed = msg.getText().split(":");
		if(groupService.updateGroup(parsed[0], parsed[1]) != null) {
			sendMessage(Message.makeBroadcastMessage(msg.getName(), SUCCESS));
		} else {
			sendMessage(Message.makeBroadcastMessage(msg.getName(), FAIL));
		}
	}

	/**
	 * Checks if user is present to delete and if the user is present,
	 * deletes the user and sends quit message
	 * @param msg		the message from the client
	 */
	private void performDelete(Message msg) {
		if(deleteUser(msg.getName())) {
			enqueueMessage(Message.makeQuitMessage(msg.getName()));
		}
	}

	/**
	 * Checks if the username is present to update and if the user is present
	 * updates the username with the given user name
	 * @param msg		the message from the client
	 */
	private void performUsernameUpdate(Message msg) {
		if(updateUsername(msg.getName(), msg.getText())) {
			enqueueMessage(Message.makeQuitMessage(msg.getName()));
		}
	}

	/**
	 * Checks if the username is present to update and if the user is present
	 * updates the password with the given user name
	 * @param msg		the message from the client
	 */
	private void performPasswordUpdate(Message msg) {
		if(updatePassword(msg.getName(), msg.getText())) {
			enqueueMessage(Message.makeQuitMessage(msg.getName()));
		}
	}

	/**
	 * Displays all the broadcasted messages
	 * @param msg		the received message
	 */
	private void showAllBroadcastedMessages(Message msg) {
		List<Message> responses = new ArrayList<>();
		if (msg.isDisplayMessage()) {
			// Check if the message is legal formatted
			if (messageChecks(msg)) {
				// Check for our "special messages"
				if ((msg.isBroadcastMessage()) && (!broadcastMessageIsSpecial(msg, responses))) {
					quitIfBombMessage(msg);
				}
			} else {
				Message sendMsg;
				sendMsg = Message.makeBroadcastMessage(ServerConstants.BOUNCER_ID,
						"Last message was rejected because it specified an incorrect user name.");
				enqueueMessage(sendMsg);
			}
		} else if (msg.terminate()) {
			// Stop sending the poor client message.
			terminate = true;
			// Reply with a quit message.
			enqueueMessage(Message.makeQuitMessage(name));
		}
	}

	/**
	 * Checks if the given message is a BOMB message, and if it is
	 * then quits the channel otherwise broadcasts the message
	 * @param msg		the message to be checked if it is bomb message
	 */
	private void quitIfBombMessage(Message msg) {
		if ((msg.getText() != null)
				&& (msg.getText().compareToIgnoreCase(ServerConstants.BOMB_TEXT) == 0)) {
			initialized = false;
			Prattle.broadcastMessage(Message.makeQuitMessage(name));
		} else {
			Prattle.broadcastMessage(msg);
		}
	}

	/**
	 * Sends immediate response if criteria is matched
	 */
	private void performImmediateResponseIfRequired() {
		if (!immediateResponse.isEmpty()) {
			while (!immediateResponse.isEmpty()) {
				sendMessage(immediateResponse.remove());
			}
		}
	}

	/**
	 * Sends the given message to one user
	 */
	private void handleCustomMessage(Message msg) {

		if(msg.getText() != null && (msg.isPrivateMessage() || msg.isPersonalContaingImageMessage())) {
			sendMessageToOneUser(msg);
		}
		else if(msg.getText() != null && (msg.isGroupMessage() || msg.isGroupContainingImageMessage()) ) {
			sendMessageToGroup(msg);
		}
		else {
			if ("ON".equals(System.getenv(LOGGING))) {
				logger.log(Level.parse(System.getenv(LOGGING_LEVEL)),
						"A custom message recieved, not categorized into any category : "+msg.getText());
			}
			logger.info("A custom message recieved, not categorized into any category : "+msg.getText());
			Prattle.broadcastMessage(msg);
			broadcastFlag = true;
		}
	}

	/**
	 * Takes the given message, finds the receiver name and then sends it
	 * to that user
	 * @param msg		the message to be sent
	 */
	private void sendMessageToOneUser(Message msg) {
		String[] splitted = msg.getText().split(":");
		// This is personal message
		if (splitted.length >= 2) {
			String rec = splitted[0];
			String payload = splitted[1];
			Message toSend;
			if (msg.isPersonalContaingImageMessage()) {
				String senderIP = userService.findUserWithUserName(msg.getName()).getIp();
				String receiverIP = userService.findUserWithUserName(rec).getIp();
				List<String> list = new LinkedList<>();
				list.add(receiverIP);
				messageService.createMessage(msg.getName()+"$"+rec,payload, MessageType.IMAGE,
						senderIP, list, msg.getName());
				toSend = Message.makePersonalImageMessage(msg.getName(), msg.getText(), rec);
			}
			else {
				String senderIP = userService.findUserWithUserName(msg.getName()).getIp();
				String receiverIP = userService.findUserWithUserName(rec).getIp();
				List<String> list = new LinkedList<>();
				list.add(receiverIP);
				messageService.createMessage(msg.getName()+"$"+rec,payload, MessageType.TEXT,
						senderIP, list, msg.getName());
				toSend = Message.makePersonalMessage(msg.getName(), payload);
			}
			Prattle.sendUserMessageToSubpoena(msg, rec);
			if(isParentalControlOnForUsers(msg.getName(), rec)) {
				toSend = filteredMessage(toSend);
			}
			Prattle.isUserOnline(msg.getName(), rec);
			Prattle.sendOneToOne(toSend, rec);
			oneToOne = true;
		}
	}

	/**
	 * Sends the chat history to subpoena user
	 * @param msg		the message from the client
	 */
	private void sendChatHistoryToSubpoenaUser(Message msg) {
		String [] parsed = msg.getText().split(", ");
		String startTime = parsed[1];
		String endTime = parsed[2];
		Subpoena subpoena  = subpeonaServices.findByName(msg.getName());
		try {
			Date start = new SimpleDateFormat(DATE_FORMAT).parse(startTime);
			Date end = new SimpleDateFormat(DATE_FORMAT).parse(endTime);
			if(start.compareTo(subpoena.getFromTime()) >= 0 && end.compareTo(subpoena.getToTime()) <= 0) {
				sendHistoryMessageBackToSubpoenaUser(msg, startTime, endTime, subpoena);
			}
			else {
				sendMessage(Message.makeBroadcastMessage("PRATTLE", "NOT AUTHORIZED TO SEE IN THIS TIME FRAME"));
			}
		} catch (Exception exc) {
			if ("ON".equals(System.getenv(LOGGING))) {
				logger.log(Level.parse(System.getenv(LOGGING_LEVEL)),
						DATE_ERROR_MESSAGE+startTime+" "+endTime);
			}
		}

	}

	private void sendHistoryMessageBackToSubpoenaUser(Message msg, String startTime, String endTime, Subpoena subpoena) {
		if(subpoena.getGroupName() == null) {
			String groupName = subpoena.getUser1()+"$"+subpoena.getUser2();
			String groupName2 = subpoena.getUser2()+"$"+subpoena.getUser1();
			parseDatesAndSendMessage(startTime, endTime, groupName, groupName2, msg);
		} else {
			String groupName = subpoena.getGroupName();
			List<SingleMessage> chatMessage;
			try {
				Date start = new SimpleDateFormat(DATE_FORMAT).parse(startTime);
				Date end = new SimpleDateFormat(DATE_FORMAT).parse(endTime);
				chatMessage = messageService.searchMessages(start, end, groupName);
				sendSearchedMessagesForUserBackToClient(msg.getName(), chatMessage);
			}catch (ParseException exc) {
				if ("ON".equals(System.getenv(LOGGING))) {
					logger.log(Level.parse(System.getenv(LOGGING_LEVEL)),
							DATE_ERROR_MESSAGE+startTime+" "+endTime);
				}
				Prattle.getSlack().call(new SlackMessage(DATE_ERROR_MESSAGE+startTime+" "+endTime));
				sendSearchedMessagesForUserBackToClient(msg.getName(), new ArrayList<>());
			}
		}
	}

	/**
	 * Check if any user has set parental control on
	 * @param sec	the name of the sender
	 * @param rec	the name of the receiver
	 * @return		true if any user has sent it to on, else false
	 */
	private boolean isParentalControlOnForUsers(String sec, String rec) {
		boolean isOnForSender = userService.findUserWithUserName(sec).isParentalControl();
		boolean isOnForReceiver = userService.findUserWithUserName(rec).isParentalControl();
		return isOnForSender || isOnForReceiver;
	}

	/**
	 * Replaces the message with the filtered message if it contains
	 * any abusive words
	 * @param msg		the message from the client
	 * @return			the replaced message
	 */
	private Message filteredMessage(Message msg) {
		StringBuilder nonAbusiveMsg = new StringBuilder(" ** This message contains abusive language ** ");
		for(String word : msg.getText().split(" ")) {
			if(Prattle.getAbusiveWords().contains(word.toLowerCase())) return Message.makePersonalMessage(msg.getName(),nonAbusiveMsg.toString());
		}
		return msg;
	}

	/**
	 * Takes the given message, finds the received group and then sends the
	 * message to all the members of the group
	 * @param msg		the message to be sent to the group
	 */
	private void sendMessageToGroup(Message msg) {
		String[] splitted = msg.getText().split(":");
		if (splitted.length >= 2) {
			String rec = splitted[0];
			String payload = splitted[1];
			Message toSend;
			if (msg.isGroupContainingImageMessage()) {
				toSend = addIPAddressToEachMIMEGroupMessage(msg, rec, payload);
			}
			else {
				toSend = addIPAddressToEachTEXTGroupMessage(msg, rec, payload);
			}
			Prattle.sendGroupMessageToSubpoena(msg, rec);
			Prattle.isUserOnlineToReceiveGroup(msg.getName(), rec);
			Set<ObjectId> members = groupService.findGroupWithGroupName(rec).getMembers();
			for (ObjectId id : members) {
				User user = userService.findById(id);
				if(user != null) {
					if(isParentalControlOnForUsers(msg.getName(), user.getName())) {
						Message filtered = filteredMessage(toSend);
						Prattle.sendToGroup(filtered, user.getName(), rec);
					} else {
						Prattle.sendToGroup(toSend, user.getName(), rec);
					}
				}
			}
			toGroup = true;
		}
	}

	/**
	 * Adds IP address to each MIME Group Message
	 * @param msg		the message from client
	 * @param rec		the receiver name
	 * @param payload	the message payload
	 * @return			the message after parsing
	 */
	private Message addIPAddressToEachMIMEGroupMessage(Message msg, String rec, String payload) {
		String senderIP = userService.findUserWithUserName(msg.getName()).getIp();
		List<String> receiverIP = new LinkedList<>();
		Set<ObjectId> members = groupService.findGroupWithGroupName(rec).getMembers();
		for (ObjectId id : members) {
			User user = userService.findById(id);
			if(user != null) {
				receiverIP.add(user.getIp());
			}
		}
		messageService.createMessage(rec, payload, MessageType.IMAGE, senderIP, receiverIP, msg.getName());
		return Message.makeGroupImageMessage(msg.getName(), payload, rec);
	}

	/**
	 * Adds IP address to each Text Group Message
	 * @param msg		the message from client
	 * @param rec		the receiver name
	 * @param payload	the message payload
	 * @return			the message after parsing
	 */
	private Message addIPAddressToEachTEXTGroupMessage(Message msg, String rec, String payload) {
		String senderIP = userService.findUserWithUserName(msg.getName()).getIp();
		List<String> receiverIP = new LinkedList<>();
		Set<ObjectId> members = groupService.findGroupWithGroupName(rec).getMembers();
		for (ObjectId id : members) {
			User user = userService.findById(id);
			if(user != null) {
				receiverIP.add(user.getIp());
			}
		}
		messageService.createMessage(rec,payload, MessageType.TEXT, senderIP, receiverIP, msg.getName());
		return Message.makeGroupMessage(msg.getName(),payload);
	}

	/**
	 * Processes the special request if criteria is matched
	 */
	private void performProcessSpecialIfRequired() {
		boolean processSpecial = !specialResponse.isEmpty()
				&& ((!initialized) || (!waitingList.isEmpty()) || sendResponses.before(new Date()));
		boolean keepAlive = !processSpecial;
		// Send the responses to any special messages we were asked.
		if (processSpecial) {
			// Send all of the messages and check that we get valid
			// responses.
			while (!specialResponse.isEmpty()) {
				keepAlive |= sendMessage(specialResponse.remove());
			}
		}
		if (!waitingList.isEmpty()) {
			if (!processSpecial) {
				keepAlive = false;
			}
			// Send out all of the message that have been added to the
			// queue.
			do {
				Message msg = waitingList.remove();
				boolean sentGood = sendMessage(msg);
				keepAlive |= sentGood;
			} while (!waitingList.isEmpty());
		}
		terminate |= !keepAlive;
	}

	/**
	 * Returns the waiting queue
	 * @return	the waiting queue
	 */
	public Queue<Message> getWaitingQueue() {
		return this.waitingList;
	}

	/**
	 * Returns the immediate response queue
	 * @return	the immediate response queue
	 */
	public Queue<Message> getImmediateResponseQueue() {
		return this.immediateResponse;
	}

	/**
	 * Adds the given message to immediate response queue
	 * @param message		the message to be added
	 */
	public void addImmediateResponse(Message message) {
		this.immediateResponse.add(message);
	}

	/**
	 * Returns the immediate response queue
	 * @return	the immediate response queue
	 */
	public Queue<Message> getSpecialResponseQueue() {
		return this.specialResponse;
	}

	/**
	 * Returns the name of the message sent to the client
	 * @return		the message that was sent to the client
	 */
	public String getWhichMessageSent() {
		return this.whichMessageSent;
	}

	/**
	 * Returns the flag specifying oneToOne message was sent or not
	 * should be used only for testing
	 * @return		the flag specifying if one to one message was sent or not
	 */
	public boolean isOneToOne() {
		return this.oneToOne;
	}

	/**
	 * Returns the flag specifiying broadcast message was sent or not
	 * should be used only for testing
	 * @return		the flag specifying if broadcast message was sent or not
	 */
	public boolean isBroadcastFlag() {
		return this.broadcastFlag;
	}

	/**
	 * Returns the flag specifying oneToOne message was sent or not
	 * should be used only for testing
	 * @return		the flag specifying if one to one message was sent or not
	 */
	public boolean isToGroup() {
		return this.toGroup;
	}
}
