import java.rmi.*;
import java.io.*;

/**
 * The Interface Chord is the server-side implementation fo the Chord.
 * Communicates with other servers.
 * Please see documentation on the Chord Class to see function documentation
 * of this interface
 * @author Aaron turner
 */
public interface ChordMessageInterface extends Remote
{
    public ChordMessageInterface getPredecessor()  throws RemoteException;
    ChordMessageInterface locateSuccessor(long key) throws RemoteException;
    ChordMessageInterface closestPrecedingNode(long key) throws RemoteException;
    public void joinRing(String Ip, int port)  throws RemoteException;
    public void notify(ChordMessageInterface j, boolean isForLeaving) throws RemoteException;
    public boolean isAlive() throws RemoteException;
    public long getId() throws RemoteException;


    public void put(long guidObject, InputStream file) throws IOException, RemoteException;
    public InputStream get(long guidObject) throws IOException, RemoteException;
    public void delete(long guidObject) throws IOException, RemoteException;
}
