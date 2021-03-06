import java.rmi.*;
import java.net.*;
import java.util.*;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.nio.file.*;

/**
 * The class Chord User defines the CLI for using the Chord.
 * It allows users to input commands, and performs actions on the Chord
 * Running in the background of the application
 * @author Aaron turner
 */
public class ChordUser
{
     int port;
     Chord chord;

     // Hashmap of the read times
     HashMap<Long, Long> readTimes = new HashMap<Long, Long>();

     /**
      * Constructor for our chord CLI
      *
      * @param p
      *          the port of the host of the other chord
      */
     public ChordUser(int p) {
         port = p;

         Timer timer1 = new Timer();
         // Hashmap of the read times
         HashMap readTimes = new HashMap();

         timer1.scheduleAtFixedRate(new TimerTask() {
            @Override
             public void run() {
                 try {
                     long guid = md5("" + port);
                     chord = new Chord(port, guid);
                     try{
                         Files.createDirectories(Paths.get(guid+"/repository"));
                     }
                     catch(IOException e)
                     {
                         e.printStackTrace();

                     }
                     //Moved usage to loop
                     //System.out.println("Usage: \n\tjoin <ip> <port>\n\twrite <file> (the file must be an integer stored in the working directory, i.e, ./"+guid+"/file");
                     //System.out.println("\tread <file>\n\tdelete <file>\n\tprint");

                     Scanner scan = new Scanner(System.in);
                     String delims = "[ ]+";
                     String command = "";
                     //Clear the screen before we show our output
                     //using ANSI Codes to get clear screen effect
                     System.out.println("\u001b[2J");
                     while (true)
                     {
                         //Print usage
                         System.out.println("Aaron Turner, CECS 327, Chord Filesystem\nUsage: \n\tjoin <ip> <port>\n\twrite <file> (the file must be an integer stored in the working directory, i.e, ./"+guid+"/file");
                         System.out.println("\tread <file>\n\tdelete <file>\n\tprint\n\tlist\n\tleave");
                         String text= scan.nextLine();
                         String[] tokens = text.split(delims);

                         //Clear the screen before we show our output
                         //using ANSI Codes to get clear screen effect
                         System.out.println("\u001b[2J");
                         System.out.println("Command: ");
                         System.out.println(text);
                         System.out.println("Response: ");

                         if (tokens[0].equals("join") && tokens.length == 3) {
                             try {
                                 int portToConnect = Integer.parseInt(tokens[2]);

                                 chord.joinRing(tokens[1], portToConnect);
                             } catch (IOException e) {
                                 e.printStackTrace();
                             }
                         } else if (tokens[0].equals("print")) {
                             chord.Print();
                         } else if (tokens[0].equals("list")) {
                           //Get all of our files within our repo
                           File folder = null;
                           try {
                               System.out.println("Files in ID folder:");
                               String path = "./" +  chord.getId() + "/"; // path to folder
                               folder = new File(path);
                               File[] listOfFiles = folder.listFiles();
                               if(listOfFiles.length < 1) {
                                 System.out.println("No files in folder...");
                               } else {
                                 for (int i = 0; i < listOfFiles.length; i++) {
                                   if (listOfFiles[i].isFile()) {
                                      System.out.println(listOfFiles[i].getName());
                                    } else if (listOfFiles[i].isDirectory()) {
                                      System.out.println(listOfFiles[i].getName() + "/");

                                      //Loop through and print the directory
                                      File[] listOfFilesSubFolder = listOfFiles[i].listFiles();
                                      for(int k = 0; k < listOfFilesSubFolder.length; k++) {
                                        System.out.println("\t" + listOfFilesSubFolder[k].getName());
                                      }
                                    }
                                 }
                               }

                               //A final line for spacing
                               System.out.println();
                           } catch (IOException e) {
                               e.printStackTrace();
                           }
                         } else if (tokens[0].equals("leave")) {

                            //Cancel our UI timer
                            timer1.cancel();
                            timer1.purge();

                            //quit the UI, goodbyes will be caught by our shutdown
                            System.exit(1);
                            return;
                         } else if  ((tokens[0].equals("write") ||
                          tokens[0].equals("delete")) &&
                          tokens.length == 2) {
                             //Create a local
                             //    "./"+  guid +"/"+fileName
                             // where filename = tokens[1];
                             try {
                                 String path;
                                 String fileName = tokens[1];

                                 // Total peers is 3, because we hav 3 copies of each file
                                 int totalPeers = 3;
                                 // Number of peers who can commit
                                 int numPeersCanCommit = 0;

                                 // The shared Id of the transaction
                                 int transactionId = (int)(Math.random() * 10000 + 1);
                                 // Our list of transactions we are sharing
                                 HashMap<Long, Transaction> transactionMap = new HashMap<Long, Transaction>();


                                 // Ask the peers if we can commit
                                 for(int i = 1; i <= totalPeers; ++i) {
                                   // TODO: mod by 65535 to get it to go to other ports
                                   long guidObject = md5(fileName + i);
                                   // If you are using windows you have to use
                                   // 				path = ".\\"+  guid +"\\"+fileName; // path to file
                                   path = "./" + guid + "/" + fileName; // path to file
                                   FileStream file = new FileStream(path);
                                   ChordMessageInterface peer = chord.locateSuccessor(guidObject);
                                   // Ask all of the peers if we can commit, and pass it a new transaction object
                                   Transaction peerTransaction;
                                   if(tokens[0].equals("write")) {
                                     peerTransaction = new Transaction(guidObject, file, transactionId, Transaction.OPERATION.WRITE);
                                     transactionMap.put(guidObject, peerTransaction);
                                   } else {
                                     peerTransaction = new Transaction(guidObject, file, transactionId, Transaction.OPERATION.DELETE);
                                     transactionMap.put(guidObject, peerTransaction);
                                   }
                                   Transaction peerCanCommitTransaction = peer.canCommit(peerTransaction, (Long) readTimes.get(guidObject));
                                   transactionMap.put(guidObject, peerCanCommitTransaction);

                                   if(peer.getDecision(peerCanCommitTransaction) == Transaction.VOTE.YES)
                                    numPeersCanCommit++;
                                 }

                                 // Check if we are committing
                                 if(numPeersCanCommit >= totalPeers) {
                                   // Commit the transactions
                                   for(int i = 1; i <= totalPeers; ++i) {
                                    // TODO: mod by 65535 to get it to go to other ports
                                    long guidObject = md5(fileName + i);
                                    ChordMessageInterface peer = chord.locateSuccessor(guidObject);
                                    peer.doCommit(transactionMap.get(guidObject));
                                    // Update our read time
                                    readTimes.put(guidObject, peer.getTime());
                                   }

                                   // File Commited
                                   // Get our example transaction
                                   Transaction firstTransaction = (Transaction) transactionMap.values().toArray()[0];
                                   System.out.println(fileName +
                                    " has been committed! With the operation: " +
                                    firstTransaction.getOperation());
                                 } else {
                                   // Abort the transactions
                                   System.out.println("All Peers did not agree to the commit!\nPlease update your file accordingly, aborting...");
                                   for(int i = 1; i <= totalPeers; ++i) {
                                    // TODO: mod by 65535 to get it to go to other ports
                                    long guidObject = md5(fileName + i);
                                    ChordMessageInterface peer = chord.locateSuccessor(guidObject);
                                    peer.doAbort(transactionMap.get(guidObject));
                                   }
                                 }
                             } catch (IOException e) {
                                 e.printStackTrace();
                             }
                         } else if  (tokens[0].equals("read") && tokens.length == 2) {
                             // Obtain the chord that is responsable for the file:
                             //  peer = chord.locateSuccessor(guidObject);
                             // where guidObject = md5(fileName);
                             // Now you can obtain the conted of the file in the chord using
                             // Call stream = peer.get(guidObject)
                             // Store the content of stream in the file that you create
                             try {
                                 String fileName = tokens[1];
                                 // Plus 1 to read the first copy
                                 long guidObject = md5(fileName + 1);
                                 ChordMessageInterface peer = chord.locateSuccessor(guidObject);
                                 InputStream stream = peer.get(guidObject);

                                 //Check if we got null
                                 if (stream == null) {
                                   System.out.println("File not found: " + fileName);
                                   continue;
                                 }

                                 // Write the stream to a file
                                 //http://www.baeldung.com/convert-input-stream-to-a-file
                                 byte[] buffer = new byte[stream.available()];
                                 stream.read(buffer);

                                 // Save the file in our current directory
                                 String clientPath = "./" +  chord.getId() + "/" + fileName;
                                 File readFile = new File(clientPath);
                                 // Delete the file, and re-create it if it already exists
                                 if(readFile.exists()) {
                                   readFile.delete();
                                   readFile.createNewFile();
                                 }
                                 OutputStream outStream = new FileOutputStream(readFile);
                                 outStream.write(buffer);
                                 System.out.println("Read file at: " + fileName);
                                 // Also read the contents of the file
                                 System.out.println("File Contents of " + fileName + ":");
                                 Scanner input = new Scanner(readFile);
                                 while (input.hasNextLine())
                                 {
                                   System.out.println(input.nextLine());
                                 }
                                 input.close();

                                 // Finally, add to our read times
                                 for(int i = 1; i < 4; i++) {
                                   readTimes.put(md5(fileName + i), peer.getTime());
                                 }
                             } catch (IOException e) {
                                 e.printStackTrace();
                             }
                        } else {
                          System.out.println("Command not recognized");
                        }
                     }
                 }
                 catch(RemoteException e)
                 {
                        System.out.println(e);
                 }
             }
         }, 1000, 1000);
    }

    /**
     * function used to hash a string into a usable guid (md5 hash)
     *
     * @param objectName
     *          the string we are hashing through MD5
     */
    private long md5(String objectName)
    {
        try
        {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(objectName.getBytes());
            BigInteger bigInt = new BigInteger(1,m.digest());
            return Math.abs(bigInt.longValue());
        }
        catch(NoSuchAlgorithmException e)
        {
                e.printStackTrace();

        }
        return 0;
    }

    /**
     * Function called before killing the application
     */
    public void quitChord() {
      chord.quitChord();
      System.out.println("Thank you for using the Chord. Good bye!");
    }

    /**
     * Function called at the beginning of every Java application
     *
     * @param args
     *            the command line arguments that were sent when the application launched
     */
    static public void main(String args[])
    {
        if (args.length < 1 ) {
            throw new IllegalArgumentException("Parameter: <port>");
        }

        try {
            //Create our chord user
            final ChordUser chordUser = new ChordUser(Integer.parseInt(args[0]));

            //Catch CTRL+C Events
            Runtime.getRuntime().addShutdownHook(new Thread() {
              public void run() {
                //Quit the chord user
                chordUser.quitChord();
              }
           });
        }
        catch (Exception e) {
           e.printStackTrace();
           System.exit(1);
        }
     }
}
