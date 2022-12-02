package client;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public interface MasterCommInterface {

    void passSolution(String solution);

    void subscribe(String ip) throws MalformedURLException, NotBoundException, RemoteException;

}
