package edu.northeastern.ccs.im.chatter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.*;
import java.sql.SQLOutput;
import java.util.*;
import java.util.logging.Level;

import edu.northeastern.ccs.im.*;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

/**
 * Class which can be used as a command-line IM client.
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0
 * International License. To view a copy of this license, visit
 * http://creativecommons.org/licenses/by-sa/4.0/. It is based on work
 * originally written by Matthew Hertz and has been adapted for use in a class
 * assignment at Northeastern University.
 *
 * @version 1.3
 */
public class CommandLineMain {

	private static String uName;
	private static boolean hasLogged;
	private static boolean hasReceivedError;
	private static boolean hasCreatedGroup;
	private static boolean gotSearchedMessages;
	private static boolean toggledParental;
	private static IMConnection connect;
	private static MessageScanner mess;
	private static KeyboardScanner scan;
    private static boolean isSubpoenaUser;


	static {
		uName = "";
		hasLogged = false;
		hasReceivedError = false;
		hasCreatedGroup = false;
		gotSearchedMessages = false;
        isSubpoenaUser = false;
        toggledParental = false;
	}

	/**
	 * This main method will perform all of the necessary actions for this phase of
	 * the course project
	 *
	 * @param args Command-line arguments which we ignore
	 */
	public static void main(String[] args) {
		@SuppressWarnings("resource")
		Scanner in = new Scanner(System.in);

		Constant.abusiveWords = getAbusiveWordsFromFile();

		System.out.println("1. Login");
		System.out.println("2. Sign up");
		System.out.println("Enter your choice: ");
		String choice = in.nextLine();
		switch (Integer.valueOf(choice)) {
			case 1:
				do {
					connect = loginOrRegisterAction(args[0], args[1]);
				} while (!connect.connect("LOGIN"));
				break;
			case 2:
				do {
					connect = loginOrRegisterAction(args[0], args[1]);
				} while (!connect.connect("REGISTER"));
				break;
			default:
				throw new IllegalArgumentException();
		}
		// Create the objects needed to read & write IM messages.
		 scan = connect.getKeyboardScanner();
		 mess = connect.getMessageScanner();
		listenForMessage();

		if (isSubpoenaUser) {
            showMenuForSubpoenaUser();
        }
        else {
            showMenuAfterLogin();

        }
	}

	/**
	 * Loads the abusive words into a HashSet from CSV File
	 */
	public static Set<String> getAbusiveWordsFromFile(){
		Set<String> abusiveWords = new HashSet<>();

		String csvFile = "src/main/java/edu/northeastern/ccs/im/abusiveWords/abusiveWords.csv";
		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(csvFile));

			String line = "";

			while ((line = br.readLine()) != null) {

				// use comma as separator
				String word = line;

				abusiveWords.add(word);

			}
		}  catch (FileNotFoundException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	} finally {
		if (br != null) {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return abusiveWords;
	}
	}

	/**
	 * Waits for the message from the server and then sets the flag if the
	 * user is registered to login, otherwise breaks.
	 */
	private static void listenForMessage() {
		while (!hasLogged && !hasReceivedError) {
			if (mess.hasNext()) {
				Message message = mess.next();
				performLoginOrRegistration(message);
			}
		}
	}

	/**
	 * Waits for the acknowledgement from the server if group was
	 * created successfully or not
	 */
	private static void listenForGroupAck(String successMessage, String failMessage) {
		while(!hasCreatedGroup) {
			if(mess.hasNext()) {
				Message message = mess.next();
				if(message.getText().equalsIgnoreCase("SUCCESS")) {
					System.out.println(successMessage);
					hasCreatedGroup = true;
				}
				if(message.getText().equalsIgnoreCase("FAIL")) {
					System.out.println(failMessage);
					hasCreatedGroup = true;
				}
			}
		}
	}

	private static void listenForSearchedMessages() {
	    while (!gotSearchedMessages) {
            if(mess.hasNext()) {
                Message message = mess.next();
                if(message.getText().equalsIgnoreCase("FAIL")) {
                    System.out.println("NO MESSAGES TO RETRIEVE");
                    hasCreatedGroup = true;
                }
                else {
                    if(message.getText().equalsIgnoreCase("DONE SEARCHING")) {
                        hasCreatedGroup = true;
                    }
                    else {
                        System.out.println(message.getText());
                    }
                }
            }
        }
    }

	private static void listenForUserDetails(String failMessage) {
		while(!hasCreatedGroup) {
			if(mess.hasNext()) {
				Message message = mess.next();
				if(message.getText().equalsIgnoreCase("FAIL")) {
					System.out.println(failMessage);
					hasCreatedGroup = true;
				}
				else {
					System.out.println(message.getText());
					hasCreatedGroup = true;
				}
			}
		}
	}

	/**
	 * Checks the type of message and performs the operation or sets
	 * the error
	 * @param message		the message to be checked for ACK
	 */
	private static void performLoginOrRegistration(Message message) {
		if(isLogin(message)) {
			tryLogin(message);
		}
		else if(isRegistration(message)) {
			tryRegistration(message);
		}
	}

	/**
	 * Checks the message string and then decides if the message is related to
	 * registration or not
	 * @param message	message to read and parse
	 * @return			true if the received message is related to registration
	 */
	private static boolean isRegistration(Message message) {
		if(message.getText().equalsIgnoreCase("REGISTRATION_SUCCESSFUL") || message.getText().equalsIgnoreCase("REGISTRATION_FAILED")) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Checks the message string and then decides if the message is related to
	 * login or not
	 * @param message	message to read and parse
	 * @return			true if the received message is related to login
	 */
	private static boolean isLogin(Message message) {
		if(message.getText().equalsIgnoreCase("LOGIN_SUCCESSFUL") ||
                message.getText().equalsIgnoreCase("LOGIN_SUCCESSFUL SUBPOENA") ||
                message.getText().equalsIgnoreCase("LOGIN_FAILED")) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Checks the received message and sets the hasLogged flag to true if
	 * it is a positive ACK otherwise sets hasLogged flag to false
	 * @param message		the message to read
	 */
	private static void tryRegistration(Message message) {
		if(message.getSender().equalsIgnoreCase(uName) && message.getText().equalsIgnoreCase("REGISTRATION_SUCCESSFUL")) {
			System.out.println("Successfully registered and logged in!");
			hasLogged = true;
			hasReceivedError = false;
		}
		else {
			System.out.println("Registration failed");
			hasReceivedError = true;
		}
	}

	/**
	 * Checks the received message and sets the hasLogged flag to true if
	 * it is a positive ACK otherwise sets hasLogged flag to false
	 * @param message		the message to read
	 */
	private static void tryLogin(Message message) {
	    if (message.getSender().equalsIgnoreCase(uName) && message.getText().equalsIgnoreCase("LOGIN_SUCCESSFUL SUBPOENA")) {
            System.out.println("Logging in Subpoena user");
            isSubpoenaUser = true;
            hasLogged=true;
            hasReceivedError=false;
        }
		else if (message.getSender().equalsIgnoreCase(uName) && message.getText().equalsIgnoreCase("LOGIN_SUCCESSFUL")) {
			System.out.println("Successfully logged in!");
			hasLogged = true;
			hasReceivedError = false;
		}
		else {
			System.out.println("Login failed!");
			hasReceivedError = true;
		}
	}

	/**
	 * Takes the address and the port and connects to the server.
	 * @param address	the address of the server
	 * @param port		the port to communicate
	 * @return			the new IMConnection with the given address and
	 * 					the port
	 */
	private static IMConnection loginOrRegisterAction(String address, String port) {
		System.out.println("Enter username");
		Scanner sc = new Scanner(System.in);
		String username = sc.nextLine();
		System.out.println("Enter password");
		String password = sc.nextLine();
		uName = username;
		return new IMConnection(address, Integer.parseInt(port), username, encryptPassword(password));
	}

	/**
	 * Encrypts the given password to Hex
	 * @param password		the password to be encrypted
	 * @return 				the encrypted password
	 */
	private static String encryptPassword(String password) {
		String encoding = Hex.encodeHexString(password.getBytes());
		return encoding;
	}

	/**
	 * Talks with the server by reading the user inputs
	 */
	private static void talkToServer() {
		if (hasLogged) {
			// Repeat the following loop
			while (connect.connectionActive()) {
				// Check if the user has typed in a line of text to broadcast to the IM server.
				// If there is a line of text to be
				// broadcast:
				if (scan.hasNext()) {
					// Read in the text they typed
					String line = scan.nextLine();

					// If the line equals "/quit", close the connection to the IM server.
					if (line.equals("/quit")) {
						connect.disconnect();
						break;
					} else {
						// Else, send the text so that it is broadcast to all users logged in to the IM
						// server.
						connect.sendMessage(line);
					}
				}
				// Get any recent messages received from the IM server.
				if (mess.hasNext()) {
					Message message = mess.next();
					if (!message.getSender().equals(connect.getUserName())) {
						System.out.println(message.getSender() + ": " + message.getText());
					}
				}
			}
			System.out.println("Program complete.");
			System.exit(0);
		}
	}

	/**
	 * Talks with the server by reading the user inputs for individual user
	 */
	private static void sendIndividualMessage(String recName) {
		if (hasLogged) {
			// Repeat the following loop
			while (connect.connectionActive()) {
				// Check if the user has typed in a line of text to broadcast to the IM server.
				// If there is a line of text to be
				// broadcast:
				if (scan.hasNext()) {
					// Read in the text they typed
					String line = scan.nextLine();

					// If the line equals "/quit", close the connection to the IM server.
					if (line.equals("/quit")) {
						connect.disconnect();
						break;
					} else {
						// Else, send the text so that it is broadcast to all users logged in to the IM
						// server.
						connect.sendMessageToIndividual(line, recName);
					}
				}
				// Get any recent messages received from the IM server.
				if (mess.hasNext()) {

					Message message = mess.next();
					if (!message.getSender().equals(connect.getUserName())) {
						System.out.println(message.getSender() + ": " + message.getText());
					}
				}
			}
			System.out.println("Program complete.");
			System.exit(0);
		}
	}

	/**
	 * Talks with the server by reading the user inputs for individual user
	 */
	private static void sendIndividualMessageWithImage(String recName) {
		if (hasLogged) {
			// Repeat the following loop
			while (connect.connectionActive()) {
				// Check if the user has typed in a line of text to broadcast to the IM server.
				// If there is a line of text to be
				// broadcast:
				if (scan.hasNext()) {
					// Read in the text they typed
					String line = scan.nextLine();

					// If the line equals "/quit", close the connection to the IM server.
					if (line.equals("/quit")) {
						connect.disconnect();
						break;
					} else {
						// Else, send the text so that it is broadcast to all users logged in to the IM
						// server.
						String encodedString = line + " " + readFileFromDisk(line);
						connect.sendMessageWithImageToIndividual(encodedString, recName);
					}
				}
				// Get any recent messages received from the IM server.
				if (mess.hasNext()) {

					Message message = mess.next();
					if (!message.getSender().equals(connect.getUserName())) {
						storeImageOnClient(message.getText());
					}
				}
			}
			System.out.println("Program complete.");
			System.exit(0);
		}
	}

	private static void storeImageOnClient(String message) {
		String[] split = message.split(":");
		System.out.println(split[2]);
		String ext = FilenameUtils.getExtension(split[2].split("\\s+")[0]);
		String path = "data/file_" + UUID.randomUUID().toString() + "." + ext;
		try (FileOutputStream imgOutFile = new FileOutputStream(path)) {
			byte[] imgByteArray = org.apache.commons.codec.binary.Base64.decodeBase64(split[2].split("\\s+")[1]);
			imgOutFile.write(imgByteArray);
		} catch (IOException exc) {
			System.out.println("File could not be saved!");
		}
	}

	/**
	 * Talks with the server by reading the user inputs for individual user
	 */
	private static void sendGroupMessage(String grpName) {
		if (hasLogged) {
			// Repeat the following loop
			while (connect.connectionActive()) {
				// Check if the user has typed in a line of text to broadcast to the IM server.
				// If there is a line of text to be
				// broadcast:
				if (scan.hasNext()) {
					// Read in the text they typed
					String line = scan.nextLine();

					// If the line equals "/quit", close the connection to the IM server.
					if (line.equals("/quit")) {
						connect.disconnect();
						break;
					} else {
						// Else, send the text so that it is broadcast to all users logged in to the IM
						// server.
						connect.sendMessageToGroup(line, grpName);
					}
				}
				// Get any recent messages received from the IM server.
				if (mess.hasNext()) {
					Message message = mess.next();
					if (!message.getSender().equals(connect.getUserName())) {
						System.out.println(message.getSender() + ": " + message.getText());
					}
				}
			}
			System.out.println("Program complete.");
			System.exit(0);
		}
	}

	/**
	 * Method used to read file from provided filePath
	 * @param filePath - filepath from where the file should be read
	 * @return : String
	 */
	private static String readFileFromDisk(String filePath) {
		String encodedString= null;
		try {
			byte[] fileContent = FileUtils.readFileToByteArray(new File(filePath));
			encodedString = Base64.getEncoder().encodeToString(fileContent);
		} catch (IOException e) {
			encodedString = "Image file not properly uploaded. Ask to send again.";
			e.printStackTrace();
		}
		return encodedString;
	}

	/**
	 * Talks with the server by reading the user inputs for individual user
	 */
	private static void sendGroupMessageWithImage(String grpName) {
		if (hasLogged) {
			// Repeat the following loop
			while (connect.connectionActive()) {
				// Check if the user has typed in a line of text to broadcast to the IM server.
				// If there is a line of text to be
				// broadcast:
				if (scan.hasNext()) {
					// Read in the text they typed
					String line = scan.nextLine();

					// If the line equals "/quit", close the connection to the IM server.
					if (line.equals("/quit")) {
						connect.disconnect();
						break;
					} else {
						// Else, send the text so that it is broadcast to all users logged in to the IM
						// server.
						String encodedString = line + " " + readFileFromDisk(line);
						connect.sendMessageToGroupWithImage(encodedString, grpName);
					}
				}
				// Get any recent messages received from the IM server.
				if (mess.hasNext()) {

					Message message = mess.next();
					if (!message.getSender().equals(connect.getUserName())) {
						storeImageOnClient(message.getText());
					}
				}
			}
			System.out.println("Program complete.");
			System.exit(0);
		}
	}


	/**
	 * Displays the menu after successful login
	 */
	private static void showMenuAfterLogin() {
		if(hasLogged) {
			System.out.println("1. Update username or password");
			System.out.println("2. Delete user");
			System.out.println("3. Chat with all");
			System.out.println("4. Chat with individual");
			System.out.println("5. Chat with individual by sending an Image");
			System.out.println("6. View user details");
			System.out.println("7. Send message to Group");
			System.out.println("8. Send Image message to Group");
            System.out.println("9. Create a new group");
			System.out.println("10. Add users to group");
			System.out.println("11. Delete a group");
			System.out.println("12. Remove a user from group");
			System.out.println("13. Change name of a group");
			System.out.println("14. View users in the group");
			System.out.println("15. Search messages by name");
			System.out.println("16. Search messages by timeStamp");
            System.out.println("17. Set parental control for a user");

			Scanner sc = new Scanner(System.in);
			String choice = sc.nextLine();
			switch (Integer.valueOf(choice)) {
				case 1:
					updateAccount();
					break;
				case 2:
					requestDelete();
					System.out.println("Deleted and logged out!");
					break;
				case 3:
					System.out.println("Start typing your messages...");
					talkToServer();
					break;
				case 4:
					System.out.println("Enter receiver name : ");
					String recName = sc.nextLine();
					System.out.println("Start typing your messages...");
					sendIndividualMessage(recName);
					break;
				case 5:
					System.out.println("Enter receiver name : ");
					String recName1 = sc.nextLine();
					System.out.println("Start adding filePath to Image as message...");
					sendIndividualMessageWithImage(recName1);
					break;
				case 6:
					System.out.println("User details:");
					viewUserDetails();
					listenForUserDetails("USER NOT PRESENT");
					hasCreatedGroup = false;
					break;
				case 7:
					System.out.println("Enter group name : ");
					String grpName = sc.nextLine();
					System.out.println("Start typing your messages...");
					sendGroupMessage(grpName);
					break;
				case 8:
					System.out.println("Enter group name : ");
					String grpName1 = sc.nextLine();
					System.out.println("Start adding filePath to Image as message...");
					sendGroupMessageWithImage(grpName1);
					break;
                case 9:
                    System.out.println("Enter group name : ");
                    String newGrpName = sc.nextLine();
                    createGroup(newGrpName);
					listenForGroupAck("Group created successfully", "Group already exists");
					hasCreatedGroup = false;
                    break;
				case 10:
					System.out.println("Adding users to a group: ");
					addUsersToGroup(sc);
					listenForGroupAck("Group created successfully", "Group does not exist");
					hasCreatedGroup = false;
					break;
				case 11:
					System.out.println("Enter group name : ");
					String GrpNameDel = sc.nextLine();
					deleteGroup(GrpNameDel);
					listenForGroupAck("Group deleted successfully", "Group does not exist");
					hasCreatedGroup = false;
					break;
				case 12:
					System.out.println("Removing users from group: ");
					removeUsersFromGroup(sc);
					listenForGroupAck("Users removed successfully", "Group does not exist");
					hasCreatedGroup = false;
					break;
				case 13:
					System.out.println("Enter group name : ");
					String GrpNameOld = sc.nextLine();
					System.out.println("Enter new name : ");
					String GrpNameNew = sc.nextLine();
					renameGrp(GrpNameNew, GrpNameOld);
					listenForGroupAck("Group name updated successfully", "Group does not exist");
					hasCreatedGroup = false;
					break;
				case 14:
					System.out.println("Enter group name : ");
					String GrpName = sc.nextLine();
					viewUsersInGroup(GrpName);
                    listenForUserDetails("NO SUCH GROUP");
                    hasCreatedGroup = false;
					break;
				case 15:
					System.out.println("1. User name");
					System.out.println("2. Group name");
					String option = sc.nextLine();

					if(option.equalsIgnoreCase("1")) {
						System.out.println("Enter User name");
						String userName = sc.nextLine();
						searchMessageByUserName(userName);
						listenForSearchedMessages();
						listenForUserDetails("NO MESSAGES TO RETRIEVE");
						hasCreatedGroup = false;
					}
					else if(option.equalsIgnoreCase("2")) {
						System.out.println("Enter Group name");
						String grpNameSearch = sc.nextLine();
						searchMessageByGroupName(grpNameSearch);
                        listenForSearchedMessages();
                        listenForUserDetails("NO MESSAGES TO RETRIEVE");
                        hasCreatedGroup = false;
					}
					break;
				case 16:
					System.out.println("1. User name");
					System.out.println("2. Group name");
					String choice1 = sc.nextLine();
					if (choice1.equalsIgnoreCase("1")) {
						System.out.println("Enter User name");
						String userName = sc.nextLine();
						System.out.println("Enter timestamp start value (Format : MM/DD/YYYY HH:MM) : ");
						String startTime = sc.nextLine();
						System.out.println("Enter timestamp end value (Format : MM/DD/YYYY HH:MM) : ");
						String endTime = sc.nextLine();
						searchMessageByTimeStampForUser(userName, startTime, endTime);
                        listenForSearchedMessages();
                        listenForUserDetails("NO MESSAGES TO RETRIEVE");
                        hasCreatedGroup = false;
					}
					else if (choice1.equalsIgnoreCase("2")) {
						System.out.println("Enter Group name");
						String groupName = sc.nextLine();
						System.out.println("Enter timestamp start value (Format : MM/DD/YYYY HH:MM) : ");
						String startTime = sc.nextLine();
						System.out.println("Enter timestamp end value (Format : MM/DD/YYYY HH:MM) : ");
						String endTime = sc.nextLine();
						searchMessageByTimeStampForGroup(groupName, startTime, endTime);
                        listenForSearchedMessages();
                        listenForUserDetails("NO MESSAGES TO RETRIEVE");
                        hasCreatedGroup = false;
					}

					break;
                case 17:
                    System.out.println("Want to set ON / OFF ? : ");
                    String pcChoice = sc.nextLine(); // reading choice to set on or off the parental control feature
                    updateParentalControlForUser("", pcChoice);
					listenForParentalControlAck();
					toggledParental = false;
                    break;
				default:
					System.out.println("Invalid choice");
			}
		}
	}

	private static void listenForParentalControlAck() {
		while (!toggledParental) {
			if(mess.hasNext()) {
				Message message = mess.next();
				if(message.getText().equalsIgnoreCase("PARENTAL_ON")) {
					System.out.println("PARENTAL CONTROL TURNED ON");
					toggledParental = true;
				}
				else {
					System.out.println("PARENTAL CONTROL TURNED OFF");
					toggledParental = true;
				}
			}
		}
	}

	private static void showMenuForSubpoenaUser() {

	    if(hasLogged && isSubpoenaUser) {
            System.out.println("1. See live chat");
            System.out.println("2. See history chat");
			Scanner sc = new Scanner(System.in);
            String choice = sc.nextLine();
            if ("1".equalsIgnoreCase(choice)) {
                System.out.println("Seeing live chat.");
                seeLiveChat();
            }
            else {
				System.out.println("Enter start time (MM/dd/yyyy HH:mm)");
				String start = sc.nextLine();
				System.out.println("Enter End time (MM/dd/yyyy HH:mm)");
				String end = sc.nextLine();
                showHistoryForSubpoenaUser(start, end);
				listenForSearchedMessages();
				listenForUserDetails("NO MESSAGES TO RETRIEVE");
				hasCreatedGroup = false;
            }
        }
    }


    /**
     * Subpoena user listens for live chat for allowed users
     */
    private static void seeLiveChat() {
        if (hasLogged) {
            // Repeat the following loop
            boolean flag = false;
            while (connect.connectionActive()) {
                // Get any recent messages received from the IM server.
                if (mess.hasNext()) {

                    Message message = mess.next();
                    if (!message.getSender().equals(connect.getUserName())) {
                        System.out.println(message.getSender() + ": " + message.getText());
                    }
                }
            }
            System.out.println("Program complete.");
            System.exit(0);
        }
    }

	/**
	 * Sends a message to see the chat history for a subpoena user
	 */
	private static void showHistoryForSubpoenaUser(String start, String end) {
		sendHistoryChatForSubpoenaMessage(start, end);
	}

	/**
	 * Talks with the server by reading the user inputs for individual user
	 */
	private static void sendHistoryChatForSubpoenaMessage(String start, String end) {
		searchMessageByTimeStampForUser("", start, end);
	}

	/**
	 * Search messages by user name
	 *
	 * @param userName
	 */
	private static void searchMessageByUserName(String userName) {
		connect.sendSearchMessageByUName(userName);
	}

	/**
	 * Update parental control for a user
	 *
	 * @param userName
	 * @param choice
	 */
	private static void updateParentalControlForUser(String userName, String choice) {
		connect.sendUpdateParentalControlMsg(userName, choice);
	}


	/**
	 * Search message by timestamp for a user
	 * @param userName user
	 * @param startTime startime
	 * @param endTime end time
	 */
	private static void searchMessageByTimeStampForUser(String userName, String startTime, String endTime){
		SimpleDateFormat formatter1=new SimpleDateFormat("MM/dd/yyyy HH:mm");

		try {
			Date date1=formatter1.parse(startTime);
			Date date2 = formatter1.parse(endTime);
			connect.sendSearchMessageByTimeForUser(userName, date1, date2);
		} catch (ParseException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Search message by timestamp for a user
	 * @param grpName user
	 * @param startTime startime
	 * @param endTime end time
	 */
	private static void searchMessageByTimeStampForGroup(String grpName, String startTime, String endTime){
		SimpleDateFormat formatter1=new SimpleDateFormat("MM/dd/yyyy HH:mm");

		try {
			Date date1=formatter1.parse(startTime);
			Date date2 = formatter1.parse(endTime);
			connect.sendSearchMessageByTimeForGroup(grpName, date1, date2);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Search messages by group name
	 *
	 * @param groupName
	 */
	private static void searchMessageByGroupName(String groupName) {
		connect.sendSearchMessageByGName(groupName);
	}


	/**
	 * Requests the server to view details of the current user
	 */
	private static void viewUserDetails() {
		connect.sendViewUserMessage();
	}

	/**
	 * Requests the server to delete the current user
	 */
	private static void requestDelete() {
		connect.sendDeleteMessage();
	}


	/**
	 * Requests the server to rename a group
	 */
	private static void renameGrp(String GrpNameNew, String GrpNameOld) {
		connect.sendRenameGrpMessage(GrpNameNew, GrpNameOld);
	}

	/**
	 * Performs all the functions on update account
	 */
	private static void updateAccount() {
		System.out.println("Update account details:");
		System.out.println("1. Update username");
		System.out.println("2. Update password");
		Scanner sc = new Scanner(System.in);
		String choice = sc.nextLine();
		switch (Integer.valueOf(choice)) {
			case 1:
				 System.out.println("You have choosen to update username");
				 System.out.println("Enter new username");
				 String newUsername = sc.nextLine();
				 System.out.println("The new username is: "+newUsername);
				 updateUsername(newUsername);
				 System.out.println("You are logged out!");
				 break;
			case 2:
				System.out.println("You have choosen to update password");
				System.out.println("Enter new password");
				String newPassword = sc.nextLine();
				System.out.println("The new password is: "+newPassword);
				updatePassword(newPassword);
				System.out.println("You are logged out!");
				break;
		}
	}

    /**
     * Performs all the functions on group creation
     */
    private static void createGroup(String grpName) {
        connect.sendCreateGrpMessage(grpName);
    }

	/**
	 * Performs all the functions on viewing users in the group
	 */
	private static void viewUsersInGroup(String grpName) {
		connect.sendViewUsersMessage(grpName);
	}

	/**
	 * Performs all the functions on group deletion
	 */
	private static void deleteGroup(String grpName) {
		connect.sendDeleteGrpMessage(grpName);
	}

	/**
	 * Performs all the functions on adding users to a group
	 */
	private static void addUsersToGroup(Scanner scanner) {
		System.out.println("Enter group Name : ");
		String grpName = scanner.nextLine();
		boolean enterMore = true;
		StringBuilder users = new StringBuilder();
		while(enterMore) {
			System.out.println("Enter userName : ");
			String userName = scanner.nextLine();
			users.append(userName+" ");
			System.out.println("Do you want to enter more - Press Y or N");
			String option = scanner.nextLine();
			if ("N".equalsIgnoreCase(option)) break;
		}

		connect.sendAddUserToGrpMessage(grpName, users.toString());
	}

	/**
	 * Performs all the functions on removing users from a group
	 */
	private static void removeUsersFromGroup(Scanner scanner) {
		System.out.println("Enter group Name : ");
		String grpName = scanner.nextLine();
		boolean enterMore = true;
		StringBuilder users = new StringBuilder();
		while(enterMore) {
			System.out.println("Enter userName : ");
			String userName = scanner.nextLine();
			users.append(userName+" ");
			System.out.println("Do you want to enter more - Press Y or N");
			String option = scanner.nextLine();
			if ("N".equalsIgnoreCase(option)) break;
		}

		connect.sendRemoveUserFromGrpMessage(grpName, users.toString());
	}

	/**
	 * Sends the updated username to the server
	 * @param newUsername		the new username to be set
	 */
	private static void updateUsername(String newUsername) {
		connect.sendUpdateUsernameMessage(newUsername);
	}

	/**
	 * Sends the updated password to the server
	 * @param newPassword		the new password to be set
	 */
	private static void updatePassword(String newPassword) {
		connect.sendUpdatePasswordMessage(newPassword);
	}
}
