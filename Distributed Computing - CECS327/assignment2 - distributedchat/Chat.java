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

/*
   Json Messages:

   {
        "type" :  "JOIN",
        "parameters" :
               {
                    "myAlias" : string,
                    "myPort"  : number
               }
   }

   {
        "type" :  "ACCEPT",
        "parameters" :
               {
                   "ipPred"    : string,
                   "portPred"  : number
               }
    }

    {
         "type" :  "LEAVE",
         "parameters" :
         {
             "ipPred"    : string,
             "portPred"  : number
         }
    }

   {
         "type" :  "Put",
        "parameters" :
         {
             "aliasSender"    : string,
             "aliasReceiver"  : string,
             "message"        : string
        }
   }

   {
        "type" :  "NEWSUCCESSOR",
        "parameters" :
        {
            "ipSuccessor"    : string,
            "portSuccessor"  : number
        }
   }
 */

// My info
public String alias;
public int myPort;
// Successor
public String ipSuccessor;
public int portSuccessor;
// Predecessor
public String ipPredecessor;
public int portPredecessor;

//Debug logging
public static boolean debugMode;
public static void debugLog(String log) {
  if(debugMode) {
    System.out.println(log);
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
              Socket serverSocket = new Socket("localhost", 2424);
              ObjectOutputStream oos = new ObjectOutputStream(serverSocket.getOutputStream());
              ObjectInputStream ois = new ObjectInputStream(serverSocket.getInputStream());

              // Create the mssages m using JsonWriter and send it as stream
              JSONObject paramObject = new JSONObject();
              paramObject.put("myAlias", alias);
              paramObject.put("myPort", myPort);
              JSONObject joinObject = new JSONObject();
              joinObject.put("type", "JOIN");
              joinObject.put("parameters", paramObject);

              //this sends the message
              StringWriter jsonMessage = new StringWriter();
              joinObject.writeJSONString(jsonMessage);
              String stringMessage = jsonMessage.toString();
              oos.writeObject(stringMessage);
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
