import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import org.json.simple.*;

/*****************************//**
 * \brief It implements a distributed chat.
 * It creates a ring and delivers messages
 * using flooding
 **********************************/
public class Chat {



// My info
public String alias;
public int myPort;

// Successor (Forward)
public String ipSuccessor;
public int portSuccessor;

// Predecessor (Behind)
public String ipPredecessor;
public int portPredecessor;

//Debug logging
public static boolean debugMode;
public static void debugLog(String log) {
  if(debugMode) {
    System.out.println(log);
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

                  System.out.println("Got Message! " + message);

                  //only if the message requires a response
                  //oos.write(m);

                  //Close a client socket on every read/write
                  clntSock.close();

                  //Sleep for the next connection
                  Thread.sleep(1000);
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
    boolean clientRunning = true;
        while (clientRunning)
        {
            try {
              // Reastablish connection with server continuosly
              //Change local host to a passed ip (argv) to change the requested server
              Socket serverSocket = new Socket("localhost", myPort);
              ObjectOutputStream oos = new ObjectOutputStream(serverSocket.getOutputStream());
              ObjectInputStream ois = new ObjectInputStream(serverSocket.getInputStream());
              String jsonJoin = ChatJson.getJsonJoin(alias, myPort);
              oos.writeObject(jsonJoin);
              Thread.sleep(1000);
            } catch(Exception e) {
              System.out.println(e);
            }
        }
      } catch(Exception e) {
        System.out.println(e);
      }
}
}

/*****************************//**
 * Starts the threads with the client and server:
 * \param Id unique identifier of the process
 * \param port where the server will listen
 **********************************/
public Chat(String alias, int myPort) {
        System.out.println("Starting Chat Application...");
        this.alias = alias;
        this.myPort = myPort;
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

public static void main(String[] args) {

        //Set our debug mode
        debugMode = true;

        if (args.length < 2 ) {
                throw new IllegalArgumentException("Parameter: <alias> <myPort>");
        }
        Chat chat = new Chat(args[0], Integer.parseInt(args[1]));
}
}
