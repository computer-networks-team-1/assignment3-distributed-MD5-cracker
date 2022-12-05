package client;

import java.rmi.Remote;

public interface ServerCommInterface extends ServerInterface {
	
	public void register(String teamName, ClientCommInterface cc) throws Exception;


}
