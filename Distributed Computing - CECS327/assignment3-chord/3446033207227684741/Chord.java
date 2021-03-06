import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.net.*;
import java.util.*;
import java.io.*;


public class Chord extends java.rmi.server.UnicastRemoteObject implements ChordMessageInterface
{
    public static final int M = 2;

    Registry registry;    // rmi registry for lookup the remote objects.
    ChordMessageInterface successor;
    ChordMessageInterface predecessor;
    ChordMessageInterface[] finger;
    int nextFinger;
    long guid;   		// GUID (i)


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

    public Boolean isKeyInSemiCloseInterval(long key, long key1, long key2)
    {
       if (key1 < key2)
           return (key > key1 && key <= key2);
      else
          return (key > key1 || key <= key2);
    }

    public Boolean isKeyInOpenInterval(long key, long key1, long key2)
    {
      if (key1 < key2)
          return (key > key1 && key < key2);
      else
          return (key > key1 || key < key2);
    }


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

    public long getId() throws RemoteException {
        return guid;
    }
    public boolean isAlive() throws RemoteException {
	    return true;
    }

    public ChordMessageInterface getPredecessor() throws RemoteException {
	    return predecessor;
    }

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

    public ChordMessageInterface closestPrecedingNode(long key) throws RemoteException {
        //TODO
        return successor;
    }

    public void joinRing(String ip, int port)  throws RemoteException {
        try{
            System.out.println("Get Registry to joining ring");
            Registry registry = LocateRegistry.getRegistry(ip, port);
            ChordMessageInterface chord = (ChordMessageInterface)(registry.lookup("Chord"));
            predecessor = null;
            successor = chord.locateSuccessor(this.getId());
            System.out.println("Joining ring");
        }
        catch(RemoteException | NotBoundException e){
            successor = this;
        }
    }

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
                  successor.notify(this);
              }
          }
      } catch(RemoteException | NullPointerException e1) {
          findingNextSuccessor();

      }
    }

    public void notify(ChordMessageInterface j) throws RemoteException {
        //Transfer keys in the range [j (other user guid),i (our own guid)) to j;
         if (predecessor == null || (predecessor != null
                    && isKeyInOpenInterval(j.getId(), predecessor.getId(), guid)))
             predecessor = j;

        //Get our highest and lowest is hash from ours, and j
        long highId = 0;
        long lowId = 0;
        if (guid > j.getId()) {
          highId = guid;
          lowId = j.getId();
        } else {
          lowId = guid;
          highId = j.getId();
        }

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
             if(fileId > lowId && fileId < highId) {
               //Send (Put) the file to the other user
               FileStream file = new FileStream(path + "/" + listOfFiles[i].getName());
               j.put(fileId, file);
             }
           }
        }
        catch (Exception e) {
            System.out.println(e);
        }

    }

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

    public void quitChord() {
      //Transfer files to other chords if needed
      //Transfer files to the predecessor
      try {
        notify(predecessor);
      } catch(RemoteException e) {
       System.out.println("Could not send reamining files to predecessor");
      }
    }
}
