import java.io.*;

/**
  * Class for performing transactions with our atomic commit interface
  * This contains enums for voting, and describing actions
  *
  * For brevity, will define these terms:
  * Getter - returns a private value from the transaction
  * Setter - sets a private value fof the transaction
  *
  * @author Aaron Turner
  */
public class Transaction implements Serializable {
  /**
   * Enum to define the type of write operation,
   * we shall perform in the transaction
   */
  public enum OPERATION {
    WRITE, DELETE
  }

  /**
   * Enum to define if we accept or reject the transaction
   */
  public enum VOTE {
    YES, NO
  }

  int transactionId;
  long guid;

  VOTE vote;
  OPERATION operation;
  FileStream fileStream;

  // Our constructor
  public Transaction(long passedGuid, FileStream passedFileStream, int passedId,
   OPERATION passedOperation) {
    guid = passedGuid;
    fileStream = passedFileStream;
    transactionId = passedId;
    operation = passedOperation;
  }

  /**
   * setters for vote, to YES enum
   */
  public void voteYes() {
    vote = VOTE.YES;
  }

  /**
   * setters for vote, to NO enum
   */
  public void voteNo() {
    vote = VOTE.NO;
  }

  /**
   * Getters for vote
   */
  public VOTE getVote() {
    return vote;
  }

  /**
   * Getter for operation
   */
  public OPERATION getOperation() {
    return operation;
  }

  /**
   * Getter for fileId
   */
  public long getFileId() {
    return guid;
  }

  /**
   * Getter for transactionId
   */
  public int getTransactionId() {
    return transactionId;
  }

  /**
   * Getter for operation
   */
  public FileStream getStream() {
    return fileStream;
  }
}
