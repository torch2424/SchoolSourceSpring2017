import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;

//Json parsing
import org.json.simple.*;

//Cli Parsing
import com.beust.jcommander.*;

/*****************************//**
 * \brief It implements a distributed chat.
 * It creates a ring and delivers messages
 * using flooding
 **********************************/
public class Chat {



// My info
//Useing JCommander to parse cli input
@Parameter(names={"--alias", "-a"}, description="Your chat username", required = true)
public String alias;
@Parameter(names={"--port", "-p"}, description="Port to host your server on")
public int myPort=8080;

// Predecessor (Behind)
public String ipPredecessor;
public int portPredecessor;

// Successor (Forward)
@Parameter(names={"--host", "-h"}, description="The Host to connect to")
public String ipSuccessor = "localhost";
@Parameter(names={"--hostport", "-hp"}, description="The Port of the host to connect to")
public int portSuccessor=8080;

//How many messages to show on refresh
@Parameter(names={"--limit", "-l"}, description="Limit Chat history length to the passed limit. 0 disables this feature")
public static int chatHistoryLength=0;

//Debug logging
@Parameter(names={"--debug", "-d"}, description="To Enable Debug logging")
public static boolean debugMode=false;
public static void debugLog(String log) {
  if(debugMode == true) {
    System.out.println(log);
  }
}

//Chat command and messages
public static ArrayList<String> chatMessages;

//Chat Class Constructor
public Chat() {
  chatMessages = new ArrayList<String>();
}

//Main Function run at start
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

/*****************************//**
 * Starts the threads with the client and server:
 * \param Id unique identifier of the process
 * \param port where the server will listen
 **********************************/
public void startChat() {
  System.out.println("Starting Chat Application...");
  // Initialization of the peer
  Thread server = new Thread(new Server());
  Thread client = new Thread(new Client());
  server.start();
  client.start();
  try {
          //Default thread code, if the thread dies, join back into the main thread
          client.join();
          server.join();
  } catch (InterruptedException e)
  {
          // Handle Exception
          System.out.println("Could Not Join the Client or server back to main thread");
  }
}

//Static class for our Json
//Please see the getJsonJoin for documentation,
//on how messages are constructed
public static class ChatJson {

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

  public static String getJsonJoin(String myAlias, int myPort) {
    /*
    {
         "type" :  "JOIN",
         "parameters" :
                {
                     "myAlias" : string,
                     "myPort"  : number
                }
    }
    */
    // Create the Json Objects, and add their prameters
    JSONObject paramObject = new JSONObject();
    paramObject.put("myAlias", myAlias);
    paramObject.put("myPort", myPort);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("type", "JOIN");
    jsonObject.put("parameters", paramObject);

    //Convert the json object to a string, and return
    return ChatJson.JsonObjectToString(jsonObject);
  }

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

  public static String getJsonNewSuccessor(String ipNext, int portNext) {
    /*
    {
         "type" :  "NEWSUCCESSOR",
         "parameters" :
         {
             "ipSuccessor"    : string,
             "portSuccessor"  : number
         }
    }
    */
    // Create the Json Objects, and add their prameters
    JSONObject paramObject = new JSONObject();
    paramObject.put("ipSuccessor", ipNext);
    paramObject.put("portSuccessor", portNext);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("type", "NEWSUCCESSOR");
    jsonObject.put("parameters", paramObject);

    //Convert the json object to a string, and return
    return ChatJson.JsonObjectToString(jsonObject);
  }
}



/*****************************//**
 * \class Server class "chat.java"
 * \brief It implements the server
 **********************************/
private class Server implements Runnable
{
  public Server()
  {
  }
/*****************************//**
 * \brief It allows the system to interact with the participants.
 **********************************/
public void run() {
        try {
                Chat.debugLog("Server, myPort: " + myPort);
                ServerSocket servSock = new ServerSocket(myPort);
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
                  String message = (String) ois.readObject();

                  Chat.debugLog("Got Message! " + message);
                  Chat.chatMessages.add(message);

                  //only if the message requires a response
                  //oos.write(m);

                  //Close a client socket on every read/write
                  clntSock.close();

                  //Sleep for the next connection
                  Thread.sleep(100);
                  } catch(Exception e) {
                    System.out.println(e);
                  }
                }
        } catch (Exception e)
        {
                // Handle the exception
        }
}
}



/*****************************//*
 * \brief It implements the client
 **********************************/
private class Client implements Runnable
{

  //Colors for our Terminal
  private static final String ANSI_CLS = "\u001b[2J";
  private static final String ANSI_HOME = "\u001b[H";
  private static final String ANSI_WHITE = "\u001B[37m";
  private static final String ANSI_RED = "\u001B[31m";
  private static final String ANSI_GREEN = "\u001B[32m";
  private static final String ANSI_YELLOW = "\u001B[33m";
  private static final String ANSI_BLUE = "\u001B[34m";
  private static final String ANSI_PURPLE = "\u001B[35m";
  private static final String ANSI_CYAN = "\u001B[36m";

public Client()
{
}
/*****************************//**
 * \brief It allows the user to interact with the system.
 **********************************/
public void run()
{

    try {
    // The first thing to do is to join
    // ask the ip and port when joining and set ipSuccessor = ip, portSuccessor = port
    //Socket socket = new Socket(ipSuccessor, portSuccessor);
    Scanner consoleInput = new Scanner(System.in);
    boolean clientRunning = true;
        while (clientRunning)
        {
            //Build Our UI
            System.out.print(ANSI_CLS + ANSI_HOME);
            System.out.flush();

            //Print Messages, descending order
            System.out.println("-------------------");
            if(chatHistoryLength != 0) {
              System.out.println(ANSI_CYAN + "Messages (Limited to " + chatHistoryLength + ")" + ANSI_WHITE);
            } else {
              System.out.println(ANSI_CYAN + "Messages" + ANSI_WHITE);
            }
            System.out.println("-------------------");
            int messagesToShow = Chat.chatMessages.size() - 1;
            if(chatHistoryLength != 0 && chatHistoryLength <= Chat.chatMessages.size() - 1) {
              messagesToShow = chatHistoryLength;
            }
            for(int i = 0; i <= messagesToShow; i++) {
              System.out.println(Chat.chatMessages.get(Chat.chatMessages.size() - 1 - i));
            }

            //Print Current input
            System.out.println("");
            System.out.println("-------------------");
            System.out.println(ANSI_CYAN + "Chat Cheat Sheet:" + ANSI_WHITE);
            System.out.println("-------------------");
            System.out.println(ANSI_GREEN + "Send Message" + ANSI_WHITE + " - @[alias] [Message]");
            System.out.println(ANSI_GREEN + "Refesh Messages" + ANSI_WHITE + " - type ENTER/RETURN");
            System.out.println(ANSI_GREEN + "Quit the application" + ANSI_WHITE + " - quit");
            System.out.println("");
            System.out.print(ANSI_BLUE + "Command: " + ANSI_WHITE);

              //Get our current console input and add to our chat command
              String chatCommand = consoleInput.nextLine();

              // Reastablish connection with server continuosly
              //Change local host to a passed ip (argv) to change the requested server
              Socket serverSocket = new Socket(ipSuccessor, portSuccessor);

              //Get our JSON streams
              ObjectOutputStream oos = new ObjectOutputStream(serverSocket.getOutputStream());
              ObjectInputStream ois = new ObjectInputStream(serverSocket.getInputStream());

              //Send a Join to the server
              String jsonJoin = ChatJson.getJsonJoin(alias, myPort);
              oos.writeObject(jsonJoin);

              //Close the Server Socket
              serverSocket.close();
        }
      } catch(Exception e) {
        System.out.println(e);
      }
}
}
}
