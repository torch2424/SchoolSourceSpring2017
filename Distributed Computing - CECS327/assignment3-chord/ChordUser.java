import java.rmi.*;
import java.net.*;
import java.util.*;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.nio.file.*;

public class ChordUser
{
     int port;
     Chord chord;

     public ChordUser(int p) {
         port = p;

         Timer timer1 = new Timer();

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
                         System.out.println("\tread <file>\n\tdelete <file>\n\tprint\n\tleave");
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
                         } else if (tokens[0].equals("leave")) {

                            //Cancel our UI timer
                            timer1.cancel();
                            timer1.purge();

                            //quit the UI, goodbyes will be caught by our shutdown
                            System.exit(1);
                            return;
                         } else if  (tokens[0].equals("write") && tokens.length == 2) {
                             //Create a local
                             //    "./"+  guid +"/"+fileName
                             // where filename = tokens[1];
                             try {
                                 String path;
                                 String fileName = tokens[1];
                                 long guidObject = md5(fileName);
                                 // If you are using windows you have to use
                                 // 				path = ".\\"+  guid +"\\"+fileName; // path to file
                                 path = "./"+  guid +"/"+fileName; // path to file
                                 FileStream file = new FileStream(path);
                                 ChordMessageInterface peer = chord.locateSuccessor(guidObject);
                                 peer.put(guidObject, file); // put file into ring
                                 System.out.println("Wrote file at: " + path);
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
                                 long guidObject = md5(fileName);
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

                                 String tmpPath = "/var/tmp/" + fileName;
                                 File readFile = new File(tmpPath);
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
                             } catch (IOException e) {
                                 e.printStackTrace();
                             }
                        } else if  (tokens[0].equals("delete") && tokens.length == 2) {
                          try {
                            // Obtain the chord that is responsable for the file:
                            //  peer = chord.locateSuccessor(guidObject);
                            // where guidObject = md5(fileName)
                            // Call peer.delete(guidObject)
                            String fileName = tokens[1];
                            long guidObject = md5(fileName);
                            ChordMessageInterface peer = chord.locateSuccessor(guidObject);
                            peer.delete(guidObject);
                            System.out.println("Delete File: " + fileName);
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

    public void quitChord() {
      chord.quitChord();
      System.out.println("Thank you for using the Chord. Good bye!");
    }

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
