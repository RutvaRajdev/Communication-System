package edu.northeastern.ccs.im.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.northeastern.ccs.im.Message;
import edu.northeastern.ccs.im.model.MessageType;
import edu.northeastern.ccs.im.model.SingleMessage;
import edu.northeastern.ccs.im.model.Subpoena;
import edu.northeastern.ccs.im.service.*;
import edu.northeastern.ccs.im.service.impl.*;
import net.gpedro.integrations.slack.SlackApi;
import net.gpedro.integrations.slack.SlackMessage;

/**
 * A network server that communicates with IM clients that connect to it. This
 * version of the server spawns a new thread to handle each client that connects
 * to it. At this point, messages are broadcast to all of the other clients. 
 * It does not send a response when the user has gone off-line.
 * 
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0
 * International License. To view a copy of this license, visit
 * http://creativecommons.org/licenses/by-sa/4.0/. It is based on work
 * originally written by Matthew Hertz and has been adapted for use in a class
 * assignment at Northeastern University.
 * 
 * @version 1.3
 */
public abstract class Prattle {

	/** Amount of time we should wait for a signal to arrive. */
	private static final int DELAY_IN_MS = 50;

	/** Number of threads available in our thread pool. */
	private static final int THREAD_POOL_SIZE = 20;

	/** Delay between times the thread pool runs the client check. */
	private static final int CLIENT_CHECK_DELAY = 200;

	/** Collection of threads that are currently being used. */
	private static ConcurrentLinkedQueue<ClientRunnable> active;

	private static final String LOGGING = "LOGGING";
	private static final String LOGGING_LEVEL = "LOGGING_LEVEL";

	private static final String RECALL = "RECALL";

	private static boolean done;

	private static Set<String> abusiveWords;

	private static final SlackApi slack;

	/**
	 * UserService to communicate with User database
	 * Follows Singleton pattern
	 */
	private static final UserService userService;

	private static final GroupService groupService;

	private static final MessageService messageService;

	private static MessageQueueService messageQueueService;

	private static SubpeonaServices subpoenaService;

	private static final byte[] jsonData;

	/** The logger of this class */
	private static Logger logger = Logger.getLogger(Prattle.class.getName());

	/** All of the static initialization occurs in this "method" */
	static {
		// Create the new queue of active threads.
		jsonData = readConfigFile();
		slack = new SlackApi("https://hooks.slack.com/services/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");
		active = new ConcurrentLinkedQueue<>();
		done = false;
		userService = new UserServiceImpl();
		groupService = new GroupServiceImpl();
		messageService = new MessageServiceImpl();
		messageQueueService = new MessageQueueServiceImpl();
		subpoenaService = new SubpeonaServicesImpl();
		abusiveWords = getAbusiveWordsFromFile();
	}

	/**
	 * Returns database name from config file if the config file is read properly.
	 * Otherwise returns "msd103"
	 */
	public static String getDatabaseName() {
		String dbName = "msd103";
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(jsonData);
			dbName = rootNode.path("DATABASE").asText();
		} catch (IOException exc) {
			slack.call(new SlackMessage("COULD NOT READ FILE - config.json"));
		}
		return dbName;
	}

	/**
	 * Reads config file and returns its content.
	 */
	private static byte[] readConfigFile() {
		try {
			return Files.readAllBytes(Paths.get("src/main/resources/config.json"));
		} catch (IOException exc){
			slack.call(new SlackMessage("COULD NOT READ DATABASE NAME"));
			return "".getBytes();
		}
	}

	/**
	 * Returns database connection string from config file if the config file is read properly.
	 * Otherwise returns "mongodb://team103:team103@ds163176.mlab.com:63176/msd103"
	 */
	public static String getConnectionString() {
		String connString = "mongodb://team103:team103@ds163176.mlab.com:63176/msd103";
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(jsonData);
			connString = rootNode.path("DATABASE_CONN_STRING").asText();
		} catch (IOException exc) {
			slack.call(new SlackMessage("COULD NOT READ DATABASE CONNECTION STRING"));
		}
		return connString;
	}

	/**
     * Returns the list of abusive words
     * @return      the set of abusive words
     */
	public static Set<String> getAbusiveWords() {
	    return abusiveWords;
    }

    /**
     * Reads the abusive words from file
     * @return      the set of abusive words
     */
    protected static Set<String> getAbusiveWordsFromFile(){
        Set<String> abusiveWords = new HashSet<>();
        String csvFile = "src/main/resources/abusiveWords.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line = "";
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String word = line;
                abusiveWords.add(word);
            }
        }  catch (Exception e) {
			slack.call(new SlackMessage("COULD NOT FIND FILE "+csvFile));
			if ("ON".equals(System.getenv(LOGGING))) {
				logger.log(Level.parse(System.getenv(LOGGING_LEVEL)),
						e.toString());
			}
        }
        return abusiveWords;
	}

	/**
	 * Returns the slack api
	 * @return	the slack api instance
	 */
	public static SlackApi getSlack() {
    	return slack;
	}

	/**
	 * @return UserService bean which will be used to communicate with User database
	 */
	public static UserService getUserService() {
		return userService;
	}

	/**
	 * @return GroupService bean which will be used to communicate with Group database
	 */
	public static GroupService getGroupService() {
		return groupService;
	}

	/**
	 *
	 * @return MessageService which will be used to communicate with Message database
	 */
	public static MessageService getMessageService() {
		return messageService;
	}

    /**
     *
     * @return SuboenaService which will be used to communicate with subpoena
     * user database
     */
	public static SubpeonaServices getSubpoenaService() {
	    return subpoenaService;
    }

	/**
	 * Broadcast a given message to all the other IM clients currently on the
	 * system. This message _will_ be sent to the client who originally sent it.
	 * 
	 * @param message Message that the client sent.
	 */
	public static void broadcastMessage(Message message) {
		// Loop through all of our active threads
		for (ClientRunnable tt : active) {
			// Do not send the message to any clients that are not ready to receive it.

			if (tt.isInitialized()) {
				tt.enqueueMessage(message);
			}
		}
	}

	/**
	 * Takes the given message and the name of the receiver and sends
	 * it to the given receiver
	 * @param message		the message to be sent
	 * @param rec			the name of the receiver
	 */
	public static void sendOneToOne(Message message, String rec) {
		boolean isMessageSent = false;
		// Loop through all of our active threads
		for (ClientRunnable tt : active) {
			// Do not send the message to any clients that are not ready to receive it.
			if (tt.isInitialized() && tt.getName().equalsIgnoreCase(rec)) {
				isMessageSent = true;
				tt.enqueueMessage(message);
			}
		}
		if(!isMessageSent) {
			if(message.getText().equalsIgnoreCase(RECALL)) {
				replaceLastMessageWithRecall(message, rec);
			}
			messageQueueService.addToQueue(message.getName(), rec, new SingleMessage(message.getText(), MessageType.TEXT));

		}
	}

    /**
     * Sends a copy of the message to subpoena user if he is requesting it
     * @param message           the message to be sent to the subpoena user
     * @param groupName         the name of the group
     */
	protected static void sendGroupMessageToSubpoena(Message message, String groupName) {
		Date now = new Date();
	    List<Subpoena> allAuthorizedSubpoenaUsers = subpoenaService.findByGroupMonitored(groupName);
        for (ClientRunnable tt : active) {
            iterateEachActiveSubpoena(tt, message, allAuthorizedSubpoenaUsers, now);
        }
    }

	/**
	 * Iterates over each subpoena and finds the right one to send the message
	 * @param tt							the current active user of Prattle
	 * @param message						the incoming message
	 * @param allAuthorizedSubpoenaUsers	the list of all subpoenas authorized
	 * @param now							current time
	 */
    private static void iterateEachActiveSubpoena(ClientRunnable tt, Message message, List<Subpoena> allAuthorizedSubpoenaUsers, Date now) {
		for(Subpoena subpoena : allAuthorizedSubpoenaUsers) {
			if(tt.isInitialized() && tt.getName().equalsIgnoreCase(subpoena.getUsername()) && now.compareTo(subpoena.getFromTime()) >= 0 && now.compareTo(subpoena.getToTime()) <= 0) {
				tt.enqueueMessage(message);
			}
			else if(tt.isInitialized() && tt.getName().equalsIgnoreCase(subpoena.getUsername())) {
				tt.enqueueMessage(Message.makePersonalMessage("PRATTLE", "NOT AUTHORIZED TO SEE IN THIS TIME FRAME"));
			}
		}
	}

    /**
     * Sends a copy of the message to subpoena user if he is requesting it
     * @param message           the message to be sent to the subpoena user
     * @param rec               the user name of the receiver
     */
    protected static void sendUserMessageToSubpoena(Message message, String rec) {
    	Date now = new Date();
        List<Subpoena> allAuthorizedSubpoenaUsers = subpoenaService.findByUsersMonitored(message.getName(), rec);
        if(allAuthorizedSubpoenaUsers.isEmpty()) {
            allAuthorizedSubpoenaUsers = subpoenaService.findByUsersMonitored(rec, message.getName());
        }
        for (ClientRunnable tt : active) {
            iterateEachActiveSubpoena(tt, message, allAuthorizedSubpoenaUsers, now);
        }
    }

	/**
	 * Removes the last non-recall message
	 * @param message		the recall message from the client
	 * @param receiver		the name of the receiver
	 */
	public static void replaceLastMessageWithRecall(Message message, String receiver) {
		List<SingleMessage> allMessages = messageQueueService.getAndRemoveQueue(message.getName(), receiver);
		for(int i = allMessages.size() - 1; i >= 0; i--) {
			if(!allMessages.get(i).getMessage().equalsIgnoreCase(RECALL)) {
				allMessages.remove(i);
				break;
			}
		}
		for(SingleMessage singleMessage : allMessages) {
			messageQueueService.addToQueue(message.getName(), receiver, singleMessage);
		}
	}

	/**
	 * Removes the last non-recall message
	 * @param receiver		the name of the receiver
	 */
	public static void replaceLastMessageWithRecallForGroup(String receiver, String groupName) {
		List<SingleMessage> allMessages = messageQueueService.getAndRemoveQueue(groupName, receiver);
		for(int i = allMessages.size() - 1; i >= 0; i--) {
			if(!allMessages.get(i).getMessage().equalsIgnoreCase(RECALL)) {
				allMessages.remove(i);
				break;
			}
		}
		for(SingleMessage singleMessage : allMessages) {
			messageQueueService.addToQueue(groupName, receiver, singleMessage);
		}
	}

	/**
	 * Takes the given message and the name of the receiver and sends
	 * it to the given receiver
	 * @param message		the message to be sent
	 * @param rec			the name of the receiver
	 */
	public static void sendToGroup(Message message, String rec, String groupName) {
		boolean isMessageSent = false;
		// Loop through all of our active threads
		for (ClientRunnable tt : active) {
			// Do not send the message to any clients that are not ready to receive it.
			if (tt.isInitialized() && tt.getName().equalsIgnoreCase(rec)) {
				isMessageSent = true;
				tt.enqueueMessage(message);
			}
		}
		if(!isMessageSent) {
			if(message.getText().equalsIgnoreCase(RECALL)) {
				replaceLastMessageWithRecallForGroup(rec, groupName);
			}
			SingleMessage singleMessage = new SingleMessage(message.getText(), MessageType.TEXT);
			singleMessage.setSender(message.getName());
			messageQueueService.addToQueue(groupName, rec, singleMessage);

		}
	}

	/**
	 * Checks if there is any backlog for the user and if it is there
	 * then sends each message one at a time
	 * @param sender		the name of the sender
	 * @param receiver		the name of the receiver
	 */
	public static void isUserOnline(String sender, String receiver) {
		Queue<SingleMessage> allMessages = new ConcurrentLinkedQueue<>(messageQueueService.getAndRemoveQueue(receiver, sender));
		while(!allMessages.isEmpty()) {
			sendOneToOne(Message.makePersonalMessage(receiver, allMessages.remove().getMessage()), sender);
		}
	}

	/**
	 * Checks if there is any backlog for the user and if it is there
	 * then sends each message one at a time
	 * @param sender		the name of the sender
	 * @param receiver		the name of the receiver
	 */
	public static void isUserOnlineToReceiveGroup(String sender, String receiver) {
		Queue<SingleMessage> allMessages = new ConcurrentLinkedQueue<>(messageQueueService.getAndRemoveQueue(receiver, sender));
		while(!allMessages.isEmpty()) {
			SingleMessage singleMessage = allMessages.remove();
			sendOneToOne(Message.makeGroupMessage(singleMessage.getSender(), singleMessage.getMessage()), sender);
		}
	}

	/**
	 * Start up the threaded talk server. This class accepts incoming connections on
	 * a specific port specified on the command-line. Whenever it receives a new
	 * connection, it will spawn a thread to perform all of the I/O with that
	 * client. This class relies on the server not receiving too many requests -- it
	 * does not include any code to limit the number of extant threads.
	 * 
	 * @param args String arguments to the server from the command line. At present
	 *             the only legal (and required) argument is the port on which this
	 *             server should list.
	 * @throws IOException Exception thrown if the server cannot connect to the port
	 *                     to which it is supposed to listen.
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
		// Connect to the socket on the appropriate port to which this server connects.
		try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) {
			serverSocket.configureBlocking(false);
			serverSocket.socket().bind(new InetSocketAddress(ServerConstants.PORT));
			// Create the Selector with which our channel is registered.
			Selector selector = SelectorProvider.provider().openSelector();
			// Register to receive any incoming connection messages.
			serverSocket.register(selector, SelectionKey.OP_ACCEPT);
			// Create our pool of threads on which we will execute.
			ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
			// Listen on this port until ...
			while (!done) {
				// Check if we have a valid incoming request, but limit the time we may wait.
				while (selector.select(DELAY_IN_MS) != 0) {
					// Get the list of keys that have arrived since our last check
					Set<SelectionKey> acceptKeys = selector.selectedKeys();
					// Now iterate through all of the keys
					Iterator<SelectionKey> it = acceptKeys.iterator();
					while (it.hasNext()) {
						// Get the next key; it had better be from a new incoming connection
						SelectionKey key = it.next();
						it.remove();
						// Assert certain things I really hope is true
						assert key.isAcceptable();
						assert key.channel() == serverSocket;
						// Create a new thread to handle the client for which we just received a
						// request.
						spawnNewThread(serverSocket, threadPool);
					}
				}
			}

		} catch (Exception e) {
			if ("ON".equals(System.getenv(LOGGING))) {
				logger.log(Level.parse(System.getenv(LOGGING_LEVEL)),
						e.getMessage());
			}
		}
	}

	/**
	 * Spawns a new thread every time new user connects the channel
	 * @param serverSocket		the server socket instance
	 * @param threadPool		the thread pool instance
	 */
	private static void spawnNewThread(ServerSocketChannel serverSocket, ScheduledExecutorService threadPool) {
		try {
			// Accept the connection and create a new thread to handle this client.
			SocketChannel socket = serverSocket.accept();
			// Make sure we have a connection to work with.
			if (socket != null) {
				ClientRunnable tt = new ClientRunnable(socket);
				// Add the thread to the queue of active threads
				active.add(tt);
				// Have the client executed by our pool of threads.
				@SuppressWarnings("rawtypes")
				ScheduledFuture clientFuture = threadPool.scheduleAtFixedRate(tt, CLIENT_CHECK_DELAY,
						CLIENT_CHECK_DELAY, TimeUnit.MILLISECONDS);
				tt.setFuture(clientFuture);
			}
		} catch (Exception ae) {
			if ("ON".equals(System.getenv(LOGGING))) {
				logger.log(Level.parse(System.getenv(LOGGING_LEVEL)),
						"Caught Assertion: " + ae.toString());
			}
		}
	}

	/**
	 * Remove the given IM client from the list of active threads.
	 * 
	 * @param dead Thread which had been handling all the I/O for a client who has
	 *             since quit.
	 */
	public static void removeClient(ClientRunnable dead) {
		// Test and see if the thread was in our list of active clients so that we
		// can remove it.
		if (!active.remove(dead) && "ON".equals(System.getenv(LOGGING))) {
			logger.log(Level.parse(System.getenv(LOGGING_LEVEL)),
					"Could not find a thread that I tried to remove!\n");
		}
	}

	/**
	 * Adds the given thread to the active list.
	 * SHOULD BE ONLY USED FOR TESTING
	 * @param thread		the thread to be added to the active list
	 */
	public static void addToActiveThread(ClientRunnable thread) {
		active.add(thread);
	}
}
