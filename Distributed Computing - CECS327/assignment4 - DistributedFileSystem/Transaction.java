import java.io.*;

/**
  * Class for performing transactions with our atomic commit interface
  * This contains enums for voting, and describing actions
  * @author Aaron Turner
  */
public class Transaction implements Serializable {
  public enum OPERATION {
    WRITE, DELETE
  }

  public enum VOTE {
    YES, NO
  }

  int transactionId;
  long guid;

  VOTE vote;
  Operation operation;
  FileStream fileStream;

  // Our constructor
  public Transaction(long passedGuid, FileStream passedFileStream, int passedId,
   Operation passedOperation) {
    guid = passedGuid;
    fileStream = passedFileStream;
    transactionId = passedId;
    operation = passedOperation;
  }

  public void voteYes() {
    vote = VOTE.YES;
  }

  public void voteNo() {
    vote = VOTE.NO;
  }

  public VOTE getVote() {
    return vote;
  }

  public OPERATION getOperation() {
    return operation;
  }

  public long getFileId() {
    return guid;
  }

  public int getTransactionId() {
    return transactionId;
  }

  public FileStream getStream() {
    return fileStream;
  }
}
