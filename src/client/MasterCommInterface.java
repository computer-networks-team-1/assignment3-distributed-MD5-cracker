package client;
import java.rmi.Remote;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public interface MasterCommInterface extends ServerInterface {
    void subscribe(String ip) throws MalformedURLException, NotBoundException, RemoteException;

}
