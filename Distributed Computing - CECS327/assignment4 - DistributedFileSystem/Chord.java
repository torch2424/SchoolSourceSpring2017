// Import our libs
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.net.*;
import java.util.*;
import java.io.*;

/**
 * The Class Chord is the server-side implementation fo the Chord.
 * Communicates with other servers
 * @author Aaron turner
 */
public class Chord extends java.rmi.server.UnicastRemoteObject implements ChordMessageInterface, AtomicCommitInterface
{
    public static final int M = 2;

    Registry registry;    // rmi registry for lookup the remote objects.
    //Our successor and predecessor to communicate in a Chord
    ChordMessageInterface successor;
    ChordMessageInterface predecessor;
    ChordMessageInterface[] finger;
    int nextFinger;
    long guid;   		// GUID (i)

    // Hashmap of the read times
    HashMap writeTimes = new HashMap();

    /**
     * Find another chord at the host
     *
     * @param ip
     *          the Ip adress of the host of the other chord
     * @param port
     *          the port of the host of the other chord
     * @return the  found chord
     */
    public ChordMessageInterface rmiChord(String ip, int port)
    {
        ChordMessageInterface chord = null;
        try{
            Registry registry = LocateRegistry.getRegistry(ip, port);
            chord = (ChordMessageInterface)(registry.lookup("Chord"));
        } catch (RemoteException | NotBoundException e){
            e.printStackTrace();
        }
        return chord;
    }

    /**
     * Find if a key is within, or equal to, two other keys
     *
     * @param key
     *          the key of the chord or object we would like to compare
     * @param key1
     *          the first key bound
     * @param key2
     *          the second key bound
     * @return if the key is within the two other keys (key1, key2)
     */
    public Boolean isKeyInSemiCloseInterval(long key, long key1, long key2)
    {
       if (key1 < key2)
           return (key > key1 && key <= key2);
      else
          return (key > key1 || key <= key2);
    }

    /**
     * Find if a key is within two other keys,
     * but no equal to any of the bounding keys
     *
     * @param key
     *          the key of the chord or object we would like to compare
     * @param key1
     *          the first key bound
     * @param key2
     *          the second key bound
     * @return if the key is within the two other keys (key1, key2)
     */
    public Boolean isKeyInOpenInterval(long key, long key1, long key2)
    {
      if (key1 < key2)
          return (key > key1 && key < key2);
      else
          return (key > key1 || key < key2);
    }

    /**
     * send a PUT of an object to another chord
     *
     * @param guidObject
     *          the object we would like to send
     * @param stream
     *          the input stream to stream the file to the other chord
     */
    public void put(long guidObject, InputStream stream) throws RemoteException {
      try {
          String fileName = "./"+guid+"/repository/" + guidObject;
          FileOutputStream output = new FileOutputStream(fileName);
          while (stream.available() > 0)
              output.write(stream.read());
          output.close();
      }
      catch (IOException e) {
          System.out.println(e);
      }
    }

    /**
     * GET an object from ourselves, or another chord
     *
     * @param guidObject
     *          the object we would like to get
     * @return Inputstream to save the object to our device
     */
    public InputStream get(long guidObject) throws RemoteException {
      InputStream file = null;
      try {
         //get the file ./port/repository/guid
         // I think the professor meant port == guid, by the above comment
         String path = "./"+  guid + "/repository/" + guidObject; // path to file
         file = new FileStream(path);
      }
      catch (IOException e) {
          System.out.println(e);
      }
      return file;
    }

    /**
     * DELETE an object from ourselves, or another chord
     *
     * @param guidObject
     *          the object we would like to DELETE
     */
    public void delete(long guidObject) throws RemoteException {
          File file = null;
          try {
             // delete the file ./port/repository/guid/
             // I think the professor meant port == guid, by the above comment
             String path = "./"+  guid + "/repository/" + guidObject; // path to file
             file = new File(path);
             file.delete();
          }
          catch (Exception e) {
              System.out.println(e);
          }
    }

    /**
     * Return the guid of our chord
     *
     * @return our Chord's guid
     */
    public long getId() throws RemoteException {
        return guid;
    }

    /**
     * Return if our Chord is running
     *
     * @return true
     */
    public boolean isAlive() throws RemoteException {
	    return true;
    }

    /**
     * Return our chord's predecessor (The chord behind us in the ring)
     *
     * @return our Chord's predecessor
     */
    public ChordMessageInterface getPredecessor() throws RemoteException {
	    return predecessor;
    }

    /**
     * find a chord's successor (The chord in front of us in the ring)
     *
     * @param key
     *          the key of the chord whose successor we want
     *
     * @return our Chord's successor
     */
    public ChordMessageInterface locateSuccessor(long key) throws RemoteException {
	    if (key == guid) {
        throw new IllegalArgumentException("Key must be distinct that  " + guid);
      }
	    if (successor.getId() != guid)
	    {
	      if (isKeyInSemiCloseInterval(key, guid, successor.getId()))
	        return successor;
	      ChordMessageInterface j = closestPrecedingNode(key);

          if (j == null)
	        return null;
	      return j.locateSuccessor(key);
        }
        return successor;
    }

    /**
     * Return our chord's immediate successor (The chord in front of us in the ring)
     *
     *
     * @param key
     *          the key of the chord whose successor we want
     *
     * @return our Chord's successor
     */
    public ChordMessageInterface closestPrecedingNode(long key) throws RemoteException {
        return successor;
    }

    /**
     * Join a chord ring of the specified host
     *
     *
     * @param ip
     *          the ip of a chord within the ring
     * @param port
     *          the  of a chord within the ring
     */
    public void joinRing(String ip, int port)  throws RemoteException {
        try{
            System.out.println("Get Registry to joining ring");
            Registry registry = LocateRegistry.getRegistry(ip, port);
            ChordMessageInterface chord = (ChordMessageInterface)(registry.lookup("Chord"));
            predecessor = null;
            successor = chord.locateSuccessor(this.getId());

            //Notify your successor of your files
            notify(successor, false);

            System.out.println("Joining ring");
        }
        catch(RemoteException | NotBoundException e){
            successor = this;
        }
    }

    /**
     * find our chord's next successor (The chord in front of us in the ring)
     */
    public void findingNextSuccessor()
    {
        int i;
        successor = this;
        for (i = 0;  i< M; i++)
        {
            try
            {
                if (finger[i].isAlive())
                {
                    successor = finger[i];
                }
            }
            catch(RemoteException | NullPointerException e)
            {
                finger[i] = null;
            }
        }
    }

    /**
     * balance the files within the chord,
     * and equally distribute them through their ids
     */
    public void stabilize() {
      try {
          if (successor != null)
          {
              ChordMessageInterface x = successor.getPredecessor();

              if (x != null && x.getId() != this.getId() && isKeyInOpenInterval(x.getId(), this.getId(), successor.getId()))
              {
                  successor = x;
              }
              if (successor.getId() != getId())
              {
                  successor.notify(this, false);
              }
          }
      } catch(RemoteException | NullPointerException e1) {
          findingNextSuccessor();

      }
    }

    /**
     * Send a file to another chord,
     * where the file's id is between the two chord's id.
     * If the chord is leaving, send all files
     *
     *
     * @param j
     *          the other chord we would like to send to
     * @param isForLeaving
     *          If the reason for sending the files us because we are disconnecting
     */
    public void notify(ChordMessageInterface j, boolean isForLeaving) throws RemoteException {
        //Transfer keys in the range [j (other user guid),i (our own guid)) to j;
         if (predecessor == null || (predecessor != null
                    && isKeyInOpenInterval(j.getId(), predecessor.getId(), guid)))
             predecessor = j;

        //Get all of our files within our repo
        File folder = null;
        try {
           // delete the file ./port/repository/guid/
           // I think the professor meant port == guid, by the above comment
           String path = "./"+  guid + "/repository"; // path to file
           folder = new File(path);
           File[] listOfFiles = folder.listFiles();
           for (int i = 0; i < listOfFiles.length; i++) {
             // Check if the object id is between our id and the j id
             long fileId = Long.valueOf(listOfFiles[i].getName());
             // If the key is in the interval,
             // or if the sender is leaving, and we need to send all of our files while we can
             if(isKeyInOpenInterval(fileId, guid, j.getId()) ||
              isForLeaving) {
                System.out.println("Sending file!: " + fileId);
               //Send (Put) the file to the other user
               FileStream file = new FileStream(path + "/" + listOfFiles[i].getName());
               j.put(fileId, file);
               // Now delete our file
               listOfFiles[i].delete();
             }
           }
        }
        catch (Exception e) {
            System.out.println(e);
        }

    }

    /**
     * Fix the fingers of the chords pointing at one another in the ring
     */
    public void fixFingers() {

        long id = guid;
        try {
            long nextId;
            if (nextFinger == 0 || finger[nextFinger -1] == null) {
              nextId = (this.getId() + (1 << nextFinger));
            } else {
              nextId = finger[nextFinger -1].getId();
            }
            finger[nextFinger] = locateSuccessor(nextId);

            if (finger[nextFinger].getId() == guid)
                finger[nextFinger] = null;
            else
                nextFinger = (nextFinger + 1) % M;
        }
        catch(RemoteException | NullPointerException e) {
            finger[nextFinger] = null;
            e.printStackTrace();
        }
    }

    /**
     * Ensure that we still have an alive predecessor
     */
    public void checkPredecessor() {
      try {
          if (predecessor != null && !predecessor.isAlive())
              predecessor = null;
      }
      catch(RemoteException e)
      {
          predecessor = null;
//           e.printStackTrace();
      }
    }

    /**
     * Constructor for our Chord server class
     * @param port
     *          the port we should be hosting on
     * @param guid
     *          the id of our chord server
     */
    public Chord(int port, long guid) throws RemoteException {
        int j;
	    finger = new ChordMessageInterface[M];
        for (j=0;j<M; j++){
	       finger[j] = null;
     	}
        this.guid = guid;

        predecessor = null;
        successor = this;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
	    @Override
	    public void run() {
            stabilize();
            fixFingers();
            checkPredecessor();
            }
        }, 500, 500);
        try{
            // create the registry and bind the name and object.
            System.out.println(guid + " is starting RMI at port="+port);
            registry = LocateRegistry.createRegistry( port );
            registry.rebind("Chord", this);
        }
        catch(RemoteException e){
	       throw e;
        }
    }

    /**
     * Print command run by user, show all information about our chord
     */
    void Print()
    {
        int i;
        try {
            System.out.println("Your ID (Current User): "+ guid);
            if (successor != null)
                System.out.println("successor "+ successor.getId());
            if (predecessor != null)
                System.out.println("predecessor "+ predecessor.getId());
            for (i=0; i<M; i++)
            {
                try {
                    if (finger != null)
                        System.out.println("Finger "+ i + " " + finger[i].getId());
                } catch(NullPointerException e)
                {
                    finger[i] = null;
                }
            }
        }
        catch(RemoteException e){
	       System.out.println("Cannot retrive id");
        }
    }

    /**
     * Function to ask the chord if we can commit a file
     */
    public boolean canCommit(Transaction transaction) {
      // A client has asked us if we can commit

    }

    /**
     * Function called before closing the chord server. Will send
     * All files to it's successor
     */
    public void quitChord() {
      //Transfer files to other chords if needed
      //Transfer files to the predecessor
      try {
        if (successor != null && successor.getId() != guid) {
          System.out.println("Notifying successor");
          notify(successor, true);
        }
      } catch(RemoteException e) {
       System.out.println("Could not send reamining files to predecessor");
      }
    }
}
