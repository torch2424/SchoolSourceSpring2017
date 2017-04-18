import java.io.*;

/**
  * Class for performing transactions with our atomic commit interface
  * This contains enums for voting, and describing actions
  * @author Aaron Turner
  */
public class Transaction implements Serializable {
  public enum Operation {
    WRITE, DELETE
  }

  public enum VOTE {
    YES, NO
  }

  int TransactionId;
  int guid;

  VOTE vote;
  FileStream fileStream;

  // Our constructor
  public Transaction(passedGuid, passedFileStream, passedId) {
    guid = passedGuid;
    fileStream = passedFileStream;
    TransactionId = passedId
  }

  public void voteYes() {
    vote = VOTE.YES;
  }

  public void voteNo() {
    vote = VOTE.NO;
  }
}
