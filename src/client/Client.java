package client;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Client implements MasterCommInterface, SlaveCommInterface {

	List<SlaveCommInterface> slaves = new ArrayList<>();

	public static void main(String[] args) throws Exception {

		String type = args[3];

		if (type.equals("master"))
		{
			String teamName = args[2];

			System.out.println("Client starting, listens on IP " + args[1] + " for server callback.");

			// This is crucial, otherwise the RPis will not be reachable from the server.
			System.setProperty("java.rmi.server.hostname", args[1]);

			// Initially we have no problem :)
			byte[] problem = null;

			// Lookup the server
			ServerCommInterface sci = (ServerCommInterface)Naming.lookup("rmi://" + args[0] + "/server");

			// Create a communication handler and register it with the server
			// The communication handler is the object that will receive the tasks from the server
			ClientCommHandler cch = new ClientCommHandler();
			System.out.println("Client registers with the server");
			sci.register(teamName, cch);
		} else {
			MasterCommInterface sci = (MasterCommInterface) Naming.lookup("rmi://" + args[0] + "/server");
			sci.subscribe(args[1]);
		}

		if(args.length < 3)
		{
			System.out.println("Proper Usage is: java Client serverAddress yourOwnIPAddress teamname");
			System.exit(0);
		}

		
		MessageDigest md = MessageDigest.getInstance("MD5");

		HashMap<byte[], Integer> map = new HashMap<>();

		//which value should the client start to compute from
		int startingPoint = 100;
		
		// Now forever solve tasks given by the server
		while (true) {
			// Wait until getting a problem from the server
			while (cch.currProblem==null) {
//				Thread.sleep(1);
			}

			problem = cch.currProblem;
			// Then bruteforce try all integers till problemsize

			if(map.get(problem) != null)
				sci.submitSolution(teamName, String.valueOf(map.get(problem)));
			else {
				for (Integer i=0; i<=cch.currProblemSize; i++) {
					// Calculate their hash
					byte[] currentHash = md.digest(i.toString().getBytes());
					// If the calculated hash equals the one given by the server, submit the integer as solution
					map.put(currentHash, i);
					if (Arrays.equals(currentHash, problem)) {
						System.out.println("client submits solution");
						sci.submitSolution(teamName, i.toString());
						cch.currProblem=null;
						break;
					}
				}

			}


		}
	}

	@Override
	public void passSolution(String solution) {

	}

	@Override
	public void subscribe(String ip) throws MalformedURLException, NotBoundException, RemoteException {
		SlaveCommInterface sci = (SlaveCommInterface) Naming.lookup("rmi://" + ip + "/server");
		slaves.add(sci);
	}

	@Override
	public void passProblem(int problemSize, int index) {

	}
}
