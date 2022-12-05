package client;

import java.rmi.Remote;

public interface ServerCommInterface extends Remote {
	
	public void register(String teamName, ClientCommInterface cc) throws Exception;


}
