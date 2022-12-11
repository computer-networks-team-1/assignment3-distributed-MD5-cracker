package client;
import java.rmi.Remote;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public interface MasterCommInterface extends Remote{
    void subscribe(SlaveCommInterface sci) throws MalformedURLException, NotBoundException, RemoteException;
    void passSolution(String solution) throws Exception;
}
