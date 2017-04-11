/**
  * Class for performing transactions with our atomic commit interface
  * This contains enums for voting, and describing actions
  * @author Aaron Turner
  */
public class Transaction implements Serializeable {
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
}
