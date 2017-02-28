import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;

//Json parsing
import org.json.simple.*;
import org.json.simple.parser.*;

//Cli Parsing
import com.beust.jcommander.*;

 /**
 * It implements a distributed chat.
 * It creates a ring and delivers messages
 * using flooding
 *
 * @author Aaron Turner
 */
public class Chat {

//Colors for our Terminal
public static final String ANSI_CLS = "\u001b[2J";
public static final String ANSI_HOME = "\u001b[H";
public static final String ANSI_WHITE = "\u001B[37m";
public static final String ANSI_RED = "\u001B[31m";
public static final String ANSI_GREEN = "\u001B[32m";
public static final String ANSI_YELLOW = "\u001B[33m";
public static final String ANSI_BLUE = "\u001B[34m";
public static final String ANSI_PURPLE = "\u001B[35m";
public static final String ANSI_CYAN = "\u001B[36m";

// My info
//Useing JCommander to parse cli input
@Parameter(names={"--alias", "-a"}, description="Your chat username", required = true)
public static String myAlias;
@Parameter(names={"--port", "-p"}, description="Port to host your server on")
public static int myPort=8080;
public static String myIp = "";

// Predecessor (Behind)
public static String ipPredecessor = "";
public static int portPredecessor = -1;

// Successor (Forward)
@Parameter(names={"--host", "-h"}, description="The Host to connect to")
public static String ipSuccessor = "localhost";
@Parameter(names={"--hostport", "-hp"}, description="The Port of the host to connect to")
public static int portSuccessor = 8080;

//How many messages to show on refresh
@Parameter(names={"--limit", "-l"}, description="Limit Chat history length to the passed limit. 0 disables this feature")
public static int chatHistoryLength=0;

//Debug logging
@Parameter(names={"--debug", "-d"}, description="To Enable Debug logging")
public static boolean debugMode = false;

/**
* Wrapper function around System.out.println, to show specific logs
* in debugMode
*
* @param log - the string to log to the UI
*/
public static void debugLog(String log) {
  if(debugMode == true) {
    System.out.println(log);
  }
}

/**
* Wrapper function to show errors dependent on the type of error we received
*
* @param e - The Exception that was thrown
*/
public static void handleException(Exception e) {
  String exceptionString = e.toString().toLowerCase();
  if(!exceptionString.contains("eof")) {
    Chat.debugLog(e.toString());
  }
}

//Localhost only
@Parameter(names={"--localhost-only", "-lo"}, description="To force connections to only go through localhost")
public static boolean localhostOnly= false;

//Our static booleans for dictating joining
public static boolean waitingForAccept;

//Chat command and messages
public static ArrayList<String> chatMessages;

/**
* Constructor for the Chat Class
*/
public Chat() {
  chatMessages = new ArrayList<String>();
}

/**
* main function ran at program start. This passes arges to jCommander
* to parse and assign our passed parameters
*
* @param args - Array of passed arguments
*/
public static void main(String[] args) {
        //Set our debug mode
        Chat chat = new Chat();
        JCommander jCommander = new JCommander(chat);
        jCommander.setProgramName("Peer-To-Peer Chat");
        try {
          jCommander.parse(args);
          chat.startChat();
        } catch(Exception e) {
          jCommander.usage();
        }
}

 /**
 * Spins up threads for the client and server, and begins waiting for an accept
 */
public void startChat() {
  //Get our IP
  if(Chat.localhostOnly) {
    Chat.myIp = "localhost";
  } else {
    try {
      //Get the host, and split by / to get ip
      String fullHost = InetAddress.getLocalHost().toString();
      String[] splitHost = fullHost.split("/");
      Chat.myIp = splitHost[1];
    } catch (Exception e) {
      System.out.println("Could not obtain an IP address");
      System.exit(0);
    }
  }

  //Initialize joining
  Chat.waitingForAccept = true;

  // Initialization of the peer
  Thread server = new Thread(new Server());
  Thread client = new Thread(new Client());
  server.start();
  client.start();
  try {
          //Default thread code, if the thread dies, join back into the main thread
          server.join();
          client.join();
  } catch (InterruptedException e)
  {
          // Handle Exception
          System.out.println("Could Not Join the Client or server back to main thread");
  }
}

/**
* Static class for our Json
* Please see the getJsonJoin for documentation,
* on how messages are constructed
*/
public static class ChatJson {

  /**
  * Function to Convert a jsonObject data type, to a string to be handled/sent
  *
  * @param jsonObject - json object we are converting
  */
  private static String JsonObjectToString(JSONObject jsonObject) {
    String jsonString = "";
    try {
      //Convert the json object to a string, using StringWriter
      StringWriter jsonStringWriter = new StringWriter();
      jsonObject.writeJSONString(jsonStringWriter);
      jsonString = jsonStringWriter.toString();
    } catch(Exception e) {
      System.out.println(e);
      System.exit(1);
    }

    //Return the string
    return jsonString;
  }

  /**
  * Function to return a filled schema of the Join JSON request
  *
  * @param myPort - the port we are hosting our server on
  * @return Json Object as a string to be sent in the ObjectOutputStream
  */
  public static String getJsonJoin(int myPort) {
    /*
    {
         "type" :  "JOIN",
         "parameters" :
                {
                     "myIp": string,
                     "myPort"  : number
                }
    }
    */
    // Create the Json Objects, and add their prameters
    JSONObject paramObject = new JSONObject();
    paramObject.put("myIp", Chat.myIp);
    paramObject.put("myPort", myPort);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("type", "JOIN");
    jsonObject.put("parameters", paramObject);

    //Convert the json object to a string, and return
    return ChatJson.JsonObjectToString(jsonObject);
  }

  /**
  * Function to return a filled schema of the Accept JSON request
  *
  * @param ipPrevious - the ip address of the predecessor
  * @param portPrevious - the port of the predecessor
  * @return Json Object as a string to be sent in the ObjectOutputStream
  */
  public static String getJsonAccept(String ipPrevious, int portPrevious) {
    /*
    {
         "type" :  "ACCEPT",
         "parameters" :
                {
                    "ipPred"    : string,
                    "portPred"  : number
                }
     }
    */
    // Create the Json Objects, and add their prameters
    JSONObject paramObject = new JSONObject();
    paramObject.put("ipPred", ipPrevious);
    paramObject.put("portPred", portPrevious);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("type", "ACCEPT");
    jsonObject.put("parameters", paramObject);

    //Convert the json object to a string, and return
    return ChatJson.JsonObjectToString(jsonObject);
  }

  /**
  * Function to return a filled schema of the LEAVE JSON request
  *
  * @param ipPrevious - the ip address of the predecessor
  * @param portPrevious - the port of the predecessor
  * @return Json Object as a string to be sent in the ObjectOutputStream
  */
  public static String getJsonLeave(String ipPrevious, int portPrevious) {
    /*
    {
         "type" :  "LEAVE",
         "parameters" :
         {
             "ipPred"    : string,
             "portPred"  : number
         }
    }
    */
    // Create the Json Objects, and add their prameters
    JSONObject paramObject = new JSONObject();
    paramObject.put("ipPred", ipPrevious);
    paramObject.put("portPred", portPrevious);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("type", "LEAVE");
    jsonObject.put("parameters", paramObject);

    //Convert the json object to a string, and return
    return ChatJson.JsonObjectToString(jsonObject);
  }

  /**
  * Function to return a filled schema of the PUT JSON request.
  * Used for sending messages to another user
  *
  * @param aliasSender - the alias of the sender of the message
  * @param aliasReceiver - the alias of the receiver for the message
  * @param message - the message the receiver will view
  * @return Json Object as a string to be sent in the ObjectOutputStream
  */
  public static String getJsonPut(String aliasSender, String aliasReceiver, String message) {
    /*
    {
          "type" :  "Put",
         "parameters" :
          {
              "aliasSender"    : string,
              "aliasReceiver"  : string,
              "message"        : string
         }
    }
    */
    // Create the Json Objects, and add their prameters
    JSONObject paramObject = new JSONObject();
    paramObject.put("aliasSender", aliasSender);
    paramObject.put("aliasReceiver", aliasReceiver);
    paramObject.put("message", message);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("type", "PUT");
    jsonObject.put("parameters", paramObject);

    //Convert the json object to a string, and return
    return ChatJson.JsonObjectToString(jsonObject);
  }

  /**
  * Function to return a filled schema of the NEWSUCCESSOR JSON request
  *
  * @param ipNext - the ip address of the successor
  * @param portNext - the port of the successor
  * @return Json Object as a string to be sent in the ObjectOutputStream
  */
  public static String getJsonNewSuccessor(String ipNext, int portNext) {
    /*
    {
         "type" :  "NEWSUCCESSOR",
         "parameters" :
         {
             "ipSucc"    : string,
             "portSucc"  : number
         }
    }
    */
    // Create the Json Objects, and add their prameters
    JSONObject paramObject = new JSONObject();
    paramObject.put("ipSucc", ipNext);
    paramObject.put("portSucc", portNext);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("type", "NEWSUCCESSOR");
    jsonObject.put("parameters", paramObject);

    //Convert the json object to a string, and return
    return ChatJson.JsonObjectToString(jsonObject);
  }
}

/**
* Class for implementing the Client. This will build the UI, and
* Accept commands, and cycle through the chat messages
*/
private class Client implements Runnable
{

/**
* Constructor for the client
*/
public Client()
{
}

/**
* Function ran at the inititalization of the client thread
*
* Intially will make the Join request, and wait for the ACCEPT.
* This will infinitely loop to accept user input, and display the UI.
*/
public void run()
{

    try {
    // The first thing to do is to join
    System.out.println("-------------------");
    System.out.println(ANSI_CYAN + "Joining " + ipSuccessor + ":" +  portSuccessor + ANSI_WHITE);
    System.out.println("-------------------");

    //Create our socket
    Socket serverSocket = new Socket(ipSuccessor, portSuccessor);

    //Get our JSON streams
    ObjectOutputStream oos = new ObjectOutputStream(serverSocket.getOutputStream());
    ObjectInputStream ois = new ObjectInputStream(serverSocket.getInputStream());

    //Send a Join to the server
    //Get the current ip of the device (client)
    String jsonJoin = ChatJson.getJsonJoin(Chat.myPort);
    oos.writeObject(jsonJoin);

    //Close the Server Socket
    serverSocket.close();

    //Now Wait for the server to flip the accepted boolean
    while(Chat.waitingForAccept) {
      Thread.sleep(200);
    }

  } catch(Exception e) {
    Chat.handleException(e);
  }

    //Prepare for unrecognized commands
    boolean shouldShowBadCommand = false;
    String badCommand = "";

    //Finally start the UI, since we got accept
    Scanner consoleInput = new Scanner(System.in);
    boolean clientRunning = true;
        while (clientRunning) {
          try {
            //Build Our UI
            System.out.print(Chat.ANSI_CLS + Chat.ANSI_HOME);
            System.out.flush();

            //Print Messages, descending order
            Chat.debugLog("Refreshed! Random number proof: " + (Math.random() * 50 + 1));
            System.out.println("-------------------");
            if(chatHistoryLength != 0) {
              System.out.println(Chat.ANSI_CYAN +
                "Messages (Limited to " + chatHistoryLength + ")" +
                ANSI_WHITE);
            } else {
              System.out.println(Chat.ANSI_CYAN + "Messages" + Chat.ANSI_WHITE);
            }
            System.out.println("-------------------");
            int messagesToShow = 0;
            if(chatHistoryLength != 0 && chatHistoryLength <= Chat.chatMessages.size() - 1) {
              messagesToShow = chatHistoryLength;
            }
            for(int i = messagesToShow; i <= Chat.chatMessages.size() - 1; i++) {
              System.out.println(Chat.chatMessages.get(i));
            }

            //Print Current input
            System.out.println("");
            System.out.println("-------------------");
            System.out.println(Chat.ANSI_CYAN + "Chat Cheat Sheet:" + Chat.ANSI_WHITE);
            System.out.println("-------------------");
            System.out.println(Chat.ANSI_GREEN + "My Host" +
              Chat.ANSI_WHITE + " - " + Chat.myIp + ":" + Chat.myPort);
            if(Chat.debugMode) {
              System.out.println(Chat.ANSI_RED + "[DEBUG] My Successor" +
                Chat.ANSI_WHITE + " - " + Chat.ipSuccessor + ":" + Chat.portSuccessor);
                System.out.println(Chat.ANSI_RED + "[DEBUG] My Predecessor" +
                  Chat.ANSI_WHITE + " - " + Chat.ipPredecessor + ":" + Chat.portPredecessor);
            }
            System.out.println(Chat.ANSI_GREEN + "My Alias" +
              Chat.ANSI_WHITE + " - @" + Chat.myAlias);
            System.out.println(Chat.ANSI_GREEN + "Send Message" +
              Chat.ANSI_WHITE + " - @[alias] [Message]");
            System.out.println(Chat.ANSI_GREEN + "Refesh Messages" +
              Chat.ANSI_WHITE + " - type ENTER/RETURN");
            System.out.println(Chat.ANSI_GREEN + "Quit the application" +
              Chat.ANSI_WHITE + " - quit");
            System.out.println("");
            if(shouldShowBadCommand) {
              System.out.println(Chat.ANSI_RED + "Unrecognized Command: " +
                Chat.ANSI_WHITE + badCommand);
            }
            System.out.println("");
            System.out.print(Chat.ANSI_BLUE + "Command: " + Chat.ANSI_WHITE);

              //Get our current console input and add to our chat command
              String chatCommand = consoleInput.nextLine();

              //Reset our bad command
              shouldShowBadCommand = false;

              if(chatCommand.length() <= 1) {
                continue;
              }

              // Reastablish connection with server continuosly
              //Change local host to a passed ip (argv) to change the requested server
              Socket serverSocket = new Socket(ipSuccessor, portSuccessor);

              //Get our JSON streams
              ObjectOutputStream oos = new ObjectOutputStream(serverSocket.getOutputStream());
              ObjectInputStream ois = new ObjectInputStream(serverSocket.getInputStream());

              if(chatCommand.contains("@") && chatCommand.contains(" ")) {
                //We are sending a message to a user
                chatCommand = chatCommand.substring(1);
                String[] commandSplit = chatCommand.split(" ", 2);

                //Add to our message log
                Chat.chatMessages.add("You: " + commandSplit[1]);

                //Create our socket
                Socket newSocket = new Socket(Chat.ipSuccessor, Chat.portSuccessor);
                //Get our JSON streams
                ObjectOutputStream newOos = new ObjectOutputStream(newSocket.getOutputStream());

                String json = ChatJson.getJsonPut(Chat.myAlias, commandSplit[0], commandSplit[1]);
                newOos.writeObject(json);
                Chat.debugLog("Sent Leave: " + json);
                newSocket.close();

              } else if(chatCommand.toLowerCase().equals("quit")) {

                //Send a leave to our successor
                //Create our socket
                Socket newSocket = new Socket(Chat.ipSuccessor, Chat.portSuccessor);
                //Get our JSON streams
                ObjectOutputStream newOos = new ObjectOutputStream(newSocket.getOutputStream());

                String json = ChatJson.getJsonLeave(Chat.ipPredecessor, Chat.portPredecessor);
                newOos.writeObject(json);
                Chat.debugLog("Sent Leave: " + json);
                newSocket.close();

                //Tell the client to leave
                clientRunning = false;

                //Tell the user goodbye
                System.out.println("Goodbye! Thank you for using this chat application!");
                System.exit(0);
              } else {
                //Show a bad command
                shouldShowBadCommand = true;
                badCommand = chatCommand;
              }

              //Close the Server Socket
              serverSocket.close();
          } catch(Exception e) {
            Chat.handleException(e);
          }
        }
}
}

/**
* Class for implementing the Server. This will listen for, and accept
* Valid json requests sent to the server
*/
private class Server implements Runnable
{
  /**
  * Constructor for the server
  */
  public Server()
  {
  }

  /**
  * Function ran at the inititalization of the server thread
  *
  * Will Simply handle the valid requests that it listens for.
  */
public void run() {

        //Start the server socket
        ServerSocket servSock = null;
        try {
          servSock = new ServerSocket(Chat.myPort);
        } catch(Exception e) {
          Chat.handleException(e);
          System.exit(0);
        }

        //Debug the server
        Chat.debugLog("Server, myPort: " + Chat.myPort);
        boolean serverRunning = true;
        while (serverRunning)
        {
          try {
          // Get client connections
          Socket clntSock = servSock.accept();
          //Create a new thread to handle the connection
          ObjectInputStream ois = new ObjectInputStream(clntSock.getInputStream());
          ObjectOutputStream oos = new ObjectOutputStream(clntSock.getOutputStream());
          //reads the message using JsonParser and handle the messages
          String jsonRequest = (String) ois.readObject();

          Chat.debugLog("Got Message! " + jsonRequest);

          //Declare our response
          String jsonResponse = "";

          //Boolean for if we are sending requests to other servers,
          // And waiting for another response
          boolean waitingForResponse = false;

          //Declare our Json Parser
          JSONParser parser = new JSONParser();
          try {
                //Parse the json Object, based on our expected schema
               JSONObject requestObject = (JSONObject) parser.parse(jsonRequest);
               String requestType = requestObject.get("type").toString();
               JSONObject requestParams = (JSONObject) parser.parse(requestObject.get("parameters").toString());

               if(requestType.equals("JOIN")) {
                 //We Have a client wanting to join our chat
                 //We need to grab myIp, and myPort
                 String newIp = requestParams.get("myIp").toString();
                 int newPort = Integer.valueOf(requestParams.get("myPort").toString());

                 //If we have a predecessor, tell them they have a new Successor
                 if(Chat.ipPredecessor.length() > 0 &&
                    Chat.portPredecessor > -1) {
                      //Create our socket
                      Socket serverSocket = new Socket(ipPredecessor, portPredecessor);
                      //Get our JSON streams
                      ObjectOutputStream predOos = new ObjectOutputStream(serverSocket.getOutputStream());

                      //Send a NEW SUCCESSOR to the predecessor
                      String json = ChatJson.getJsonNewSuccessor(newIp, newPort);
                      predOos.writeObject(json);
                      Chat.debugLog("Sent new successor: " + json);

                      //Close the socket
                      serverSocket.close();
                  }

                  //Tell the new client we accept them
                  //Create our socket
                  Socket socket = new Socket(newIp, newPort);
                  //Get our JSON streams
                  ObjectOutputStream newOos = new ObjectOutputStream(socket.getOutputStream());

                  String json = ChatJson.getJsonAccept(Chat.ipPredecessor, Chat.portPredecessor);
                  newOos.writeObject(json);
                  Chat.debugLog("Accepted client: " + json);

                  //Close the socket
                  socket.close();

                  //Set the new client as our predecessor
                  Chat.ipPredecessor = newIp;
                  Chat.portPredecessor = newPort;
                  Chat.waitingForAccept = false;
               } else if (requestType.equals("NEWSUCCESSOR")) {
                 //Set our successor to the one passed by the json
                 String succIp = requestParams.get("ipSucc").toString();
                 int succPort = Integer.valueOf(requestParams.get("portSucc").toString());
                 Chat.ipSuccessor = succIp;
                 Chat.portSuccessor = succPort;
               } else if (requestType.equals("ACCEPT")) {
                 //Set our predecessor to the one passed by the json
                 String predIp = requestParams.get("ipPred").toString();
                 int predPort = Integer.valueOf(requestParams.get("portPred").toString());
                 if(Chat.ipPredecessor.length() <= 0 &&
                    Chat.portPredecessor <= -1) {
                   Chat.ipPredecessor = predIp;
                   Chat.portPredecessor = predPort;
                   Chat.waitingForAccept = false;
                 }
               } else if (requestType.equals("PUT")) {
                 //Get our parameters
                 String aliasSender = requestParams.get("aliasSender").toString();
                 String aliasReceiver = requestParams.get("aliasReceiver").toString();
                 String chatMessage = requestParams.get("message").toString();

                 //Check if we sent this message, if the message is for us,
                 // Or to pass it around the circle
                 if(aliasSender.equals(Chat.myAlias)) {
                   Chat.chatMessages.add("User not found: @" + aliasReceiver);
                 } else if(aliasReceiver.equals(Chat.myAlias)) {
                   Chat.chatMessages.add("@" + aliasSender + " says: " + chatMessage);
                 } else {
                   //Pass to the successor
                   Socket serverSocket = new Socket(Chat.ipSuccessor, Chat.portSuccessor);
                   //Get our JSON streams
                   ObjectOutputStream newOos = new ObjectOutputStream(serverSocket.getOutputStream());
                   //Send a message pass
                   String json = ChatJson.getJsonPut(aliasSender, aliasReceiver, chatMessage);
                   newOos.writeObject(json);
                   Chat.debugLog("Passed a message: " + json);

                   //Close the socket
                   serverSocket.close();
                 }
               } else if (requestType.equals("LEAVE")) {
                 //Set our predecessor to the one passed by the json
                 String predIp = requestParams.get("ipPred").toString();
                 int predPort = Integer.valueOf(requestParams.get("portPred").toString());

                 //Send a new succssore the the passed pred
                 Socket serverSocket = new Socket(predIp, predPort);
                 //Get our JSON streams
                 ObjectOutputStream predOos = new ObjectOutputStream(serverSocket.getOutputStream());

                 //Send a NEW SUCCESSOR to the predecessor
                 String json = ChatJson.getJsonNewSuccessor(Chat.myIp, Chat.myPort);
                 predOos.writeObject(json);
                 Chat.debugLog("Sent new successor: " + json);

                 //Close the socket
                 serverSocket.close();

                 //Finally set our predecessor to them
                 Chat.ipPredecessor = predIp;
                 Chat.portPredecessor = predPort;
              } else {
                //Ignore the message
              }
            } catch(ParseException pe){
              //Inform the client of a request error
              Chat.chatMessages.add(Chat.ANSI_RED +
                "Invalid Server Request: " + jsonRequest +
                Chat.ANSI_WHITE);
            }

          //Close a client socket on every read/write
          clntSock.close();

          //Sleep for the next connection
          Thread.sleep(100);
          } catch(Exception e) {
            Chat.handleException(e);
          }
        }
      }
}
}
