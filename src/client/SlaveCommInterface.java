package client;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SlaveCommInterface extends Remote{

    void passProblem(byte[] problemSize, int index, int index_end) throws RemoteException;
    void announceSuccess() throws RemoteException;

}
