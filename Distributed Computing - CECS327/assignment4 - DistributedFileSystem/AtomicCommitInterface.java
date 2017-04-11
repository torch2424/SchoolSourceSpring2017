import java.rmi.*;
import java.io.*;

/**
 * The Interface Chord is the server-side implementation fo the Chord.
 * Communicates with other servers.
 * Please see documentation on the Chord Class to see function documentation
 * of this interface
 * @author Aaron turner
 */
public interface AtomicCommitInterface extends Remote {
  public boolean canCommit(Transaction transaction) throws RemoteException;
  public void doCommit(Transaction transaction) throws IOException, RemoteException;
  public void doAbort(Transaction transaction) throws RemoteException;
  public boolean haveCommited(Transaction transaction, int guid) throws RemoteException;
  public boolean getDecisions(Transaction transaction) throws RemoteException;
}
