package client;
import java.rmi.Remote;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public interface MasterCommInterface extends Remote {

    public void submitSolution(String name, String sol) throws Exception;

    void subscribe(String ip) throws MalformedURLException, NotBoundException, RemoteException;

}
