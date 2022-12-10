package client;
import java.rmi.Remote;

public interface SlaveCommInterface extends Remote{

    void passProblem(byte[] problemSize, int index);
    void announceSuccess();

}
