package client;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SlaveCommInterface extends Remote{

    void passProblem(byte[] problem) throws RemoteException;
    void setProblemSpace(int index, int segmentSize) throws RemoteException;
}
