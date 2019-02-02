package edu.northeastern.ccs.im;

import java.util.Date;

/**
 * Each instance of this class represents a single transmission by our IM
 * clients.
 * 
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0
 * International License. To view a copy of this license, visit
 * http://creativecommons.org/licenses/by-sa/4.0/. It is based on work
 * originally written by Matthew Hertz and has been adapted for use in a class
 * assignment at Northeastern University.
 * 
 * @version 1.3
 */
public class Message {
	/**
	 * List of the different possible message types.
	 */
	protected enum MessageType {
	/**
	 * Message sent by the user attempting to login using a specified username
	 * and password.
	 */
	HELLO("HLO"),
	/** Message sent by the server acknowledging a successful log in. */
	ACKNOWLEDGE("ACK"),
	/** Message sent by the server rejecting a login attempt. */
	NO_ACKNOWLEDGE("NAK"),
	/**
	 * Message sent by the user to start the logging out process and sent by the
	 * server once the logout process completes.
	 */
	QUIT("BYE"),
	/** Message whose contents is broadcast to all connected users. */
	BROADCAST("BCT"),
	/** Message sent by the user attempting to register using specified username
	* and password */
	REGISTER("REG"),
	/** Request to delete the account */
	DELETE("DEL"),
	/** Message sent by the user attempting to change username */
	UPDATE_USERNAME("UUN"),
	/** Message sent by the user attempting to change password */
	UPDATE_PASSWORD("UPW"),
	/** Message sent by the user attempting to send individual message */
	PRIVATE_MSG("PRV"),
	/** Send Image for personal message using this tag */
	PRIVATE_MSG_IMAGE("PRI"),
	/** Send image to a group using this tag */
	GRP_MSG_IMG("GRI"),
	/** Message sent by the user attempting to send group message */
	GRP_MSG("GRP"),
	/** Create a new Group */
	CREATE_GRP("GRC"),
	/** Delete a Group */
	DELETE_GRP("GRD"),
	/** Add users to a group */
	ADD_USER_TO_GRP("GRA"),
	/** Remove users from a group */
	REMOVE_USER_FROM_GRP("GRR"),
	/** Rename a group */
	RENAME_GROUP("GRU"),
	/** View user details */
	USER_DETAILS("UDE"),
	/** View users in a group */
	VIEW_USERS_IN_GROUP("GDE"),
	/** Search messages by user name **/
	SEARCH_BY_UNAME("SUN"),
	/** Search messages by group name **/
	SEARCH_BY_GNAME("SGN"),
	/** Search messages by Timstamp with particular user **/
	SEARCH_BY_TIMEUN("SUT"),
	/** Search messages by Timstamp with particular group  **/
	SEARCH_BY_TIMEGN("SGT"),
	/** Update parental control for a user  **/
	UPDATE_PARENTAL_CONTROL("PCT");

		/** Store the short name of this message type. */
		private String tla;

		/**
		 * Define the message type and specify its short name.
		 *
		 * @param abbrev Short name of this message type, as a String.
		 */
		private MessageType(String abbrev) {
			tla = abbrev;
		}

		/**
		 * Return a representation of this Message as a String.
		 *
		 * @return Three letter abbreviation for this type of message.
		 */
		@Override
		public String toString() {
				return tla;
			}
	}

	/** The string sent when a field is null. */
	private static final String NULL_OUTPUT = "--";

	/** The handle of the message. */
	private MessageType msgType;

	/**
	 * The first argument used in the message. This will be the sender's identifier.
	 */
	private String msgSender;

	/** The second argument used in the message. */
	private String msgText;

	/**
	 * Create a new message that contains actual IM text. The type of distribution
	 * is defined by the handle and we must also set the name of the message sender,
	 * message recipient, and the text to send.
	 * 
	 * @param handle  Handle for the type of message being created.
	 * @param srcName Name of the individual sending this message
	 * @param text    Text of the instant message
	 */
	private Message(MessageType handle, String srcName, String text) {
		msgType = handle;
		// Save the properly formatted identifier for the user sending the
		// message.
		msgSender = srcName;
		// Save the text of the message.
		msgText = text;
	}

	/**
	 * Create simple command type message that does not include any data.
	 * 
	 * @param handle Handle for the type of message being created.
	 */
	private Message(MessageType handle) {
		this(handle, null, null);
	}

	/**
	 * Create a new message that contains a command sent the server that requires a
	 * single argument. This message contains the given handle and the single
	 * argument.
	 * 
	 * @param handle  Handle for the type of message being created.
	 * @param srcName Argument for the message; at present this is the name used to
	 *                log-in to the IM server.
	 */
	private Message(MessageType handle, String srcName) {
		this(handle, srcName, null);
	}

	/**
	 * Create a new message to continue the logout process.
	 * 
	 * @return Instance of Message that specifies the process is logging out.
	 */
	public static Message makeQuitMessage(String myName) {
		return new Message(MessageType.QUIT, myName, null);
	}

	/**
	 * Create a new message broadcasting an announcement to the world.
	 * 
	 * @param myName Name of the sender of this very important missive.
	 * @param text   Text of the message that will be sent to all users
	 * @return Instance of Message that transmits text to all logged in users.
	 */
	public static Message makeBroadcastMessage(String myName, String text) {
		return new Message(MessageType.BROADCAST, myName, text);
	}

	/**
	 * Create a new message for an individual.
	 *
	 * @param myName Name of the sender of this very important missive.
	 * @param text   Text of the message that will be sent to all users
	 * @param recName Name of the receiver
	 * @return Instance of Message that transmits text to all logged in users.
	 */
	public static Message makePersonalMessage(String myName, String text, String recName) {
		return new Message(MessageType.PRIVATE_MSG, myName, recName+":"+text);
	}

	/**
	 * Create a new message for an individual.
	 *
	 * @param myName Name of the sender of this very important missive.
	 * @param text   Text of the message that will be sent to all users
	 * @return Instance of Message that transmits text to all logged in users.
	 */
	public static Message makePersonalMessage(String myName, String text) {
		return new Message(MessageType.PRIVATE_MSG, myName, text);
	}

	/**
	 * Create a new message containing Image for an individual.
	 *
	 * @param myName Name of the sender of this very important missive.
	 * @param text   Text of the message - Image converted to string text that will be sent to particular user
	 * @param recName Name of the receiver
	 * @return Instance of Message that transmits text to all logged in users.
	 */
	public static Message makePersonalImageMessage(String myName, String text, String recName) {
		return new Message(MessageType.PRIVATE_MSG_IMAGE, myName, recName+":"+text);
	}

	/**
	 * Create a new message containing Image for an individual.
	 *
	 * @param myName Name of the sender of this very important missive.
	 * @param text   Text of the message - Image converted to string text that will be sent to particular user
	 * @return Instance of Message that transmits text to all logged in users.
	 */
	public static Message makePersonalImageMessage(String myName, String text) {
		return new Message(MessageType.PRIVATE_MSG_IMAGE, myName, text);
	}

	/**
	 * Create a new message for a group.
	 *
	 * @param myName Name of the sender of this very important missive.
	 * @param text   Text of the message - Image converted to string text that will be sent to all users
	 * @param grpName Name of the group
	 * @return Instance of Message that transmits text to all logged in users.
	 */
	public static Message makeGroupImageMessage(String myName, String text, String grpName) {
		return new Message(MessageType.GRP_MSG_IMG, myName, grpName+":"+text);
	}

	/**
	 * Create a new message for a group.
	 *
	 * @param myName Name of the sender of this very important missive.
	 * @param text   Text of the message - Image converted to string text that will be sent to all users
	 * @return Instance of Message that transmits text to all logged in users.
	 */
	public static Message makeGroupImageMessage(String myName, String text) {
		return new Message(MessageType.GRP_MSG_IMG, myName, text);
	}


	/**
	 * Create a new message for a group.
	 *
	 * @param myName Name of the sender of this very important missive.
	 * @param text   Text of the message that will be sent to all users
	 * @param grpName Name of the group
	 * @return Instance of Message that transmits text to all logged in users.
	 */
	public static Message makeGroupMessage(String myName, String text, String grpName) {
		return new Message(MessageType.GRP_MSG, myName, grpName+":"+text);
	}

	/**
	 * Create a new message for a group.
	 *
	 * @param myName Name of the sender of this very important missive.
	 * @param text   Text of the message that will be sent to all users
	 * @return Instance of Message that transmits text to all logged in users.
	 */
	public static Message makeGroupMessage(String myName, String text) {
		return new Message(MessageType.GRP_MSG, myName, text);
	}

    /**
     * Create a new group
     *
     * @param myName Name of the sender of this very important missive.
     * @param text   Text of the message that will be sent to all users
     * @return Instance of Message that transmits text to all logged in users.
     */
    public static Message makeGroupCreationMessage(String myName, String text) {
        return new Message(MessageType.CREATE_GRP, myName, text);
    }

	/**
	 * View List of users in the group
	 *
	 * @param myName Name of the sender of this very important missive.
	 * @param text   Text of the message that will be sent to all users
	 * @return Instance of Message that transmits text to all logged in users.
	 */
	public static Message makeViewUsersInGroupMessage(String myName, String text) {
		return new Message(MessageType.VIEW_USERS_IN_GROUP, myName, text);
	}

	/**
	 * Delete a group
	 *
	 * @param myName Name of the sender of this very important missive.
	 * @param text   Text of the message that will be sent to all users
	 * @return Instance of Message that transmits text to all logged in users.
	 */
	public static Message makeGroupDeletionMessage(String myName, String text) {
		return new Message(MessageType.DELETE_GRP, myName, text);
	}


    /**
     * Add a new user to a group
     *
     * @param myName Name of the sender of this very important missive.
     * @param text   Text of the message that will be sent to all users
     * @return Instance of Message that transmits text to all logged in users.
     */
    public static Message makeAddUsersToGrpMessage(String myName, String text) {
        return new Message(MessageType.ADD_USER_TO_GRP, myName, text);
    }

    /**
     * Add a new user to a group
     *
     * @param myName Name of the sender of this very important missive.
     * @param text   Text of the message that will be sent to all users
     * @return Instance of Message that transmits text to all logged in users.
     */
    public static Message makeAddUsersToGrpMessage(String myName, String text, String grpName) {
        return new Message(MessageType.ADD_USER_TO_GRP, myName, grpName+":"+text);
    }

	/**
	 * Remove user from a group
	 *
	 * @param myName Name of the sender of this very important missive.
	 * @param text   Text of the message that will be sent to all users
	 * @return Instance of Message that transmits text to all logged in users.
	 */
	public static Message makeRemoveUserFromGrpMessage(String myName, String text) {
		return new Message(MessageType.REMOVE_USER_FROM_GRP, myName, text);
	}

	/**
	 * Remove user from a group
	 *
	 * @param myName Name of the sender of this very important missive.
	 * @param text   Text of the message that will be sent to all users
	 * @return Instance of Message that transmits text to all logged in users.
	 */
	public static Message makeRemoveUserFromGrpMessage(String myName, String text, String grpName) {
		return new Message(MessageType.REMOVE_USER_FROM_GRP, myName, grpName+":"+text);
	}


	/**
	 * Rename a group
	 *
	 * @param myName Name of the sender of this very important missive.
	 * @param text   Text of the message that will be sent to all users
	 * @return Instance of Message that transmits text to all logged in users.
	 */
	public static Message makeGroupRenameMessage(String myName, String text) {
		return new Message(MessageType.RENAME_GROUP, myName, text);
	}

	/**
	 * Rename a group
	 *
	 * @param myName Name of the sender of this very important missive.
	 * @param text   Text of the message that will be sent to all users
	 * @param grpName Name of the group to be renamed
	 * @return Instance of Message that transmits text to all logged in users.
	 */
	public static Message makeGroupRenameMessage(String myName, String text, String grpName) {
		return new Message(MessageType.RENAME_GROUP, myName, grpName+":"+text);
	}



	/**
	 * Create a new message stating the name with which the user would like to
	 * login.
	 * 
	 * @param text Name the user wishes to use as their screen name.
	 * @return Instance of Message that can be sent to the server to try and login.
	 */
	protected static Message makeHelloMessage(String text) {
		return new Message(MessageType.HELLO, null, text);
	}

	/**
	 * Given a handle, name and text, return the appropriate message instance or an
	 * instance from a subclass of message.
	 * 
	 * @param handle  Handle of the message to be generated.
	 * @param srcName Name of the originator of the message (may be null)
	 * @param text    Text sent in this message (may be null)
	 * @return Instance of Message (or its subclasses) representing the handle,
	 *         name, & text.
	 */
	protected static Message makeMessage(String handle, String srcName, String text) {
		Message result = null;
		if (handle.compareTo(MessageType.QUIT.toString()) == 0) {
			result = makeQuitMessage(srcName);
		} else if (handle.compareTo(MessageType.HELLO.toString()) == 0) {
			result = makeLoginMessage(srcName,text);
		} else if (handle.compareTo(MessageType.BROADCAST.toString()) == 0) {
			result = makeBroadcastMessage(srcName, text);
		} else if (handle.compareTo(MessageType.ACKNOWLEDGE.toString()) == 0) {
			result = makeAcknowledgeMessage(srcName);
		} else if (handle.compareTo(MessageType.NO_ACKNOWLEDGE.toString()) == 0) {
			result = makeNoAcknowledgeMessage();
		} else if (handle.compareTo(MessageType.REGISTER.toString()) == 0) {
			result = makeRegisterMessage(srcName,text);
		} else if (handle.compareTo(MessageType.DELETE.toString()) == 0) {
			result = makeDeleteMessage(srcName);
		} else if (handle.compareTo(MessageType.UPDATE_USERNAME.toString()) == 0) {
			result = makeUpdateUsernameMessage(srcName, text);
		} else if (handle.compareTo(MessageType.UPDATE_PASSWORD.toString()) == 0) {
			result = makeUpdatePasswordMessage(srcName, text);
		} else if (handle.compareTo(MessageType.USER_DETAILS.toString()) == 0) {
			result = makeViewUserMessage(srcName);
		} else if (handle.compareTo(MessageType.PRIVATE_MSG.toString()) == 0) {
			result = makePersonalMessage(srcName, text);
		} else if (handle.compareTo(MessageType.GRP_MSG.toString()) == 0) {
			result = makeGroupMessage(srcName, text);
		} else if (handle.compareTo(MessageType.CREATE_GRP.toString()) == 0) {
        	result = makeGroupCreationMessage(srcName, text);
        } else if (handle.compareTo(MessageType.ADD_USER_TO_GRP.toString()) == 0) {
        	result = makeAddUsersToGrpMessage(srcName, text);
        } else if (handle.compareTo(MessageType.REMOVE_USER_FROM_GRP.toString()) == 0) {
			result = makeRemoveUserFromGrpMessage(srcName, text);
		} else if (handle.compareTo(MessageType.DELETE_GRP.toString()) == 0) {
			result = makeGroupDeletionMessage(srcName, text);
		} else if (handle.compareTo(MessageType.RENAME_GROUP.toString()) == 0) {
			result = makeGroupRenameMessage(srcName, text);
		}else if (handle.compareTo(MessageType.PRIVATE_MSG_IMAGE.toString()) == 0) {
			result = makePersonalImageMessage(srcName, text);
		} else if (handle.compareTo(MessageType.GRP_MSG_IMG.toString()) == 0) {
			result = makeGroupImageMessage(srcName, text);
		} else if (handle.compareTo(MessageType.VIEW_USERS_IN_GROUP.toString()) == 0) {
			result = makeViewUsersInGroupMessage(srcName, text);
		} else if (handle.compareTo(MessageType.SEARCH_BY_UNAME.toString()) == 0) {
			result = makeSearchMsgByUNameMessage(srcName, text);
		} else if (handle.compareTo(MessageType.SEARCH_BY_GNAME.toString()) == 0) {
			result = makeSearchMsgByGNameMessage(srcName, text);
		} else if (handle.compareTo(MessageType.SEARCH_BY_TIMEUN.toString()) == 0) {
			result = makeSearchMsgByTimeStampForUserMessage(srcName, text);
		} else if (handle.compareTo(MessageType.SEARCH_BY_TIMEGN.toString()) == 0) {
			result = makeSearchMsgByTimeStampForGroupMessage(srcName, text);
		} else if (handle.compareTo(MessageType.UPDATE_PARENTAL_CONTROL.toString()) == 0) {
			result = makeUpdateParentalControlMsg(srcName, text);
		}
		return result;
	}

	/**
	 * Create a new message to request that the user wants to update his
	 * account
	 *
	 * @param srcName Name the user who wants to update the password
	 * @return Instance of Message that requests the update password
	 */
	public static Message makeUpdatePasswordMessage(String srcName, String newPassword) {
		return new Message(MessageType.UPDATE_PASSWORD, srcName, newPassword);
	}

	/**
	 * Create a new message to reject the bad login attempt.
	 * 
	 * @return Instance of Message that rejects the bad login attempt.
	 */
	public static Message makeNoAcknowledgeMessage() {
		return new Message(MessageType.NO_ACKNOWLEDGE);
	}

	/**
	 * Create a new message to request that the user wants to update his
	 * account
	 * 
	 * @param srcName Name the user who wants to delete the account
	 * @return Instance of Message that requests the delete
	 */
	public static Message makeUpdateUsernameMessage(String srcName, String newName) {
		return new Message(MessageType.UPDATE_USERNAME, srcName, newName);
	}

	/**
	 * Create a new message to request searching messages by username
	 *
	 * @param srcName Name the user who wants to search message
	 * @param uName by which message needs to be searched
	 * @return Instance of Message that requests the search
	 */
	public static Message makeSearchMsgByUNameMessage(String srcName, String uName) {
		return new Message(MessageType.SEARCH_BY_UNAME, srcName, uName);
	}

	/**
	 * Create a new message to request updating parental control
	 *
	 * @param srcName Name the user who wants to search message
	 * @param uName by which message needs to be searched
	 * @param choice
	 * @return Instance of Message that requests the search
	 */
	public static Message makeUpdateParentalControlMsg(String srcName, String uName, String choice) {
		return new Message(MessageType.UPDATE_PARENTAL_CONTROL, srcName, uName+choice);
	}

	/**
	 * Create a new message to request updating parental control
	 *
	 * @param srcName Name the user who wants to search message
	 * @param text Text of message
	 * @return Instance of Message that requests the search
	 */
	public static Message makeUpdateParentalControlMsg(String srcName, String text) {
		return new Message(MessageType.UPDATE_PARENTAL_CONTROL, srcName, text);
	}

	/**
	 * Create a new message to request searching messages by timestamp for a user
	 *
	 * @param srcName Name the user who wants to search message
	 * @param text : contain modified string seperated by space
	 * @return Instance of Message that requests the search
	 */
	public static Message makeSearchMsgByTimeStampForUserMessage(String srcName, String text) {
		return new Message(MessageType.SEARCH_BY_TIMEUN, srcName, text);
	}

	/**
	 * Create a new message to request searching messages by timestamp for a user
	 *
	 * @param srcName Name the user who wants to search message
	 * @param uName by which message needs to be searched
	 * @param startTime starttime
	 * @param endTime endtime
	 * @return Instance of Message that requests the search
	 */
	public static Message makeSearchMsgByTimeStampForUserMessage(String srcName, String uName, Date startTime, Date endTime) {
		return new Message(MessageType.SEARCH_BY_TIMEUN, srcName, uName+", "+startTime+", "+endTime);
	}

	/**
	 * Create a new message to request searching messages by timestamp for a user
	 *
	 * @param srcName Name the user who wants to search message
	 * @param text : contain modified string to be searched
	 * @return Instance of Message that requests the search
	 */
	public static Message makeSearchMsgByTimeStampForGroupMessage(String srcName, String text) {
		return new Message(MessageType.SEARCH_BY_TIMEGN, srcName, text);
	}

	/**
	 * Create a new message to request searching messages by timestamp for a user
	 *
	 * @param srcName Name the user who wants to search message
	 * @param gName by which message needs to be searched
	 * @param startTime starttime
	 * @param endTime endtime
	 * @return Instance of Message that requests the search
	 */
	public static Message makeSearchMsgByTimeStampForGroupMessage(String srcName, String gName, Date startTime, Date endTime) {
		return new Message(MessageType.SEARCH_BY_TIMEGN, srcName, gName+", "+startTime+", "+endTime);
	}


	/**
	 * Create a new message to request searching messages by group name
	 *
	 * @param srcName Name the user who wants to search message
	 * @param gName by which message needs to be searched
	 * @return Instance of Message that requests the search
	 */
	public static Message makeSearchMsgByGNameMessage(String srcName, String gName) {
		return new Message(MessageType.SEARCH_BY_GNAME, srcName, gName);
	}

	/**
	 * Create a new message to request that the user wants to delete his
	 * account
	 *
	 * @param srcName Name the user who wants to delete the account
	 * @return Instance of Message that requests the delete
	 */
	public static Message makeDeleteMessage(String srcName) {
		return new Message(MessageType.DELETE, srcName);
	}

	/**
	 * Create a new message to request that the user wants to view his details
	 * @param srcName Name the user who wants to view his details
	 * @return Instance of Message that requests to view details
	 */
	public static Message makeViewUserMessage(String srcName) {
		return new Message(MessageType.USER_DETAILS, srcName);
	}

	/**
	 * Create a new message to acknowledge that the user successfully logged as the
	 * name <code>srcName</code>.
	 *
	 * @param srcName Name the user was able to use to log in.
	 * @return Instance of Message that acknowledges the successful login.
	 */
	public static Message makeAcknowledgeMessage(String srcName) {
		return new Message(MessageType.ACKNOWLEDGE, srcName);
	}

	/**
	 * Create a new message for the early stages when the user logs in without all
	 * the special stuff.
	 * 
	 * @param myName 	Name of the user who has just logged in.
	 * @param password	Password of the user who has just logged in
	 * @return Instance of Message specifying a new friend has just logged in.
	 */
	public static Message makeLoginMessage(String myName, String password) {
		return new Message(MessageType.HELLO, myName, password);
	}

	/**
	 * Create a new message for the early stages when the user registers in without
	 * all the special stuff.
	 *
	 * @param myName	Name of the user who wants to register
	 * @param password	Password of the user who wants to register
	 * @return Instance of Message specifying a new friend wants to register
	 */
	public static Message makeRegisterMessage(String myName, String password) {
		return new Message(MessageType.REGISTER, myName, password);
	}
	
	/**
	 * Return the type of this message.
	 * 
	 * @return MessageType for this message.
	 */
	public MessageType getType() {
		return msgType;
	}

	/**
	 * Return the name of the sender of this message.
	 * 
	 * @return String specifying the name of the message originator.
	 */
	public String getSender() {
		return msgSender;
	}

	/**
	 * Return the text of this message.
	 * 
	 * @return String equal to the text sent by this message.
	 */
	public String getText() {
		return msgText;
	}

	/**
	 * Determine if this message is an acknowledgement message.
	 * 
	 * @return True if the message is an acknowledgement message; false otherwise.
	 */
	public boolean isAcknowledge() {
		return (msgType == MessageType.ACKNOWLEDGE);
	}

	/**
	 * Determine if this message is broadcasting text to everyone.
	 * 
	 * @return True if the message is a broadcast message; false otherwise.
	 */
	public boolean isBroadcastMessage() {
		return (msgType == MessageType.BROADCAST);
	}

	/**
	 * Determine if this message contains text which the recipient should display.
	 * 
	 * @return True if the message is an actual instant message; false if the
	 *         message contains data
	 */
	public boolean isDisplayMessage() {
		return (msgType == MessageType.BROADCAST ||
				msgType == MessageType.PRIVATE_MSG ||
				msgType == MessageType.GRP_MSG ||
				msgType == MessageType.GRP_MSG_IMG ||
				msgType == MessageType.PRIVATE_MSG_IMAGE ||
				msgType == MessageType.SEARCH_BY_TIMEUN ||
				msgType == MessageType.SEARCH_BY_TIMEGN ||
				msgType == MessageType.SEARCH_BY_UNAME ||
				msgType == MessageType.SEARCH_BY_GNAME);
	}

	/**
	 * Determine if this message is sent by a new client to log-in to the server.
	 * 
	 * @return True if the message is an initialization message; false otherwise
	 */
	public boolean isInitialization() {
		return (msgType == MessageType.HELLO);
	}


	/**
	 * Determine if this message is sent by a user to view his details
	 *
	 * @return True if the message is message to view user details; false otherwise
	 */
	public boolean isViewUserDMessage() {
		return (msgType == MessageType.USER_DETAILS);
	}

	/**
	 * Determine if this message is sent by a new client to register to the server.
	 *
	 * @return True if the message is an registration message; false otherwise
	 */
	public boolean isRegistrationMessage() { return (msgType == MessageType.REGISTER); }

    /**
     * Determine if this message is sent by a new client to create a new group
     *
     * @return True if the message is an new group creation message; false otherwise
     */
    public boolean isCreateGroupMessage() { return (msgType == MessageType.CREATE_GRP); }

	/**
	 * Determine if this message is sent by a new client to search for a message by timestamp with a user
	 *
	 * @return True if the message is an new group creation message; false otherwise
	 */
	public boolean ismakeSearchMsgByTimeStampForUserMessage() { return (msgType == MessageType.SEARCH_BY_TIMEUN); }

	/**
	 * Determine if this message is sent by a new client to search for a message by timestamp with a user
	 *
	 * @return True if the message is an new group creation message; false otherwise
	 */
	public boolean ismakeSearchMsgByTimeStampForGroupMessage() { return (msgType == MessageType.SEARCH_BY_TIMEGN); }



	/**
	 * Determine if this message is sent by a new client to send a personal message containing Image
	 *
	 * @return True if the message is an new group creation message; false otherwise
	 */
	public boolean isPersonalContaingImageMessage() { return (msgType == MessageType.PRIVATE_MSG_IMAGE); }

	/**
	 * Determine if this message is sent by a new client to update parental control for a user
	 *
	 * @return True if the message is an new parental control update message; false otherwise
	 */
	public boolean isUpdatePCMsg() { return (msgType == MessageType.UPDATE_PARENTAL_CONTROL); }


	/**
	 * Determine if this message is sent by a new client to create a new group
	 *
	 * @return True if the message is an new group creation message; false otherwise
	 */
	public boolean isGroupContainingImageMessage() { return (msgType == MessageType.GRP_MSG_IMG); }

	/**
	 * Determine if this message is sent by a user to view list of users in a group
	 *
	 * @return True if the message is an viewing user list in a group message; false otherwise
	 */
	public boolean isViewUsersInGroupMessage() { return (msgType == MessageType.VIEW_USERS_IN_GROUP); }


	/**
	 * Determine if this message is sent by a new client to rename a group
	 *
	 * @return True if the message is a group renaming message; false otherwise
	 */
	public boolean isGroupRenameMessage() { return (msgType == MessageType.RENAME_GROUP); }

    /**
     * Determine if this message is sent by a new client to add users a new group
     *
     * @return True if the message is an add users to group message; false otherwise
     */
    public boolean isAddUsersToGroupMessage() { return (msgType == MessageType.ADD_USER_TO_GRP); }

	/**
	 * Determine if this message is sent by a new client to remove users from a group
	 *
	 * @return True if the message is a remove users from group message; false otherwise
	 */
	public boolean isRemoveUsersFromGroupMessage() { return (msgType == MessageType.REMOVE_USER_FROM_GRP); }

	/**
	 * Determine if this message is sent by a new client to search messages by user name
	 *
	 * @return True if the message is a search messages by user name message; false otherwise
	 */
	public boolean isSearchMsgByUNameMsg() { return (msgType == MessageType.SEARCH_BY_UNAME); }

	/**
	 * Determine if this message is sent by a new client to search messages by group name
	 *
	 * @return True if the message is a search messages by group name message; false otherwise
	 */
	public boolean isSearchMsgByGNameMsg() { return (msgType == MessageType.SEARCH_BY_GNAME); }

	/**
	 * Determine if this message is a message signing off from the IM server.
	 * 
	 * @return True if the message is sent when signing off; false otherwise
	 */
	public boolean terminate() {
		return (msgType == MessageType.QUIT);
	}

	/**
	 * Representation of this message as a String. This begins with the message
	 * handle and then contains the length (as an integer) and the value of the next
	 * two arguments.
	 * 
	 * @return Representation of this message as a String.
	 */
	@Override
	public String toString() {
		String result = msgType.toString();
		if (msgSender != null) {
			result += " " + msgSender.length() + " " + msgSender;
		} else {
			result += " " + NULL_OUTPUT.length() + " " + NULL_OUTPUT;
		}
		if (msgText != null) {
			result += " " + msgText.length() + " " + msgText;
		} else {
			result += " " + NULL_OUTPUT.length() + " " + NULL_OUTPUT;
		}
		return result;
	}
}
