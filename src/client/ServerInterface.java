package client;

import java.rmi.Remote;

public interface ServerInterface extends Remote {


    public void submitSolution(String name, String sol) throws Exception;

}
