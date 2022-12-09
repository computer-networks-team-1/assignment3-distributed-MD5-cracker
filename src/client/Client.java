package client;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Client implements MasterCommInterface, SlaveCommInterface {

	private List<SlaveCommInterface> slaves = new ArrayList<>();
	private final int N_SLAVES = 1;

	private byte[] problem = null;
	private Integer index = 0;

	private Remote interfaceServer;

	private HashMap<byte[], Integer> map = new HashMap<>();

	private String teamName;

	public static void main(String[] args) throws Exception {

		if (args.length < 4) {
			System.out.println("Proper Usage is: java Client serverAddress yourOwnIPAddress teamname type");
			System.exit(0);
		}

		String type = args[3];
		if (
				!(type.equalsIgnoreCase("slave") ||
						type.equalsIgnoreCase("master"))
		) {
			System.out.println("type has to be either 'master' or 'slave'");
			System.exit(0);
		}

		LocateRegistry.createRegistry(1099);
		Client clientInstance = new Client();

		// This is crucial, otherwise the RPis will not be reachable from the server/master.
		System.setProperty("java.rmi.server.hostname", args[1]);


		if (type.equalsIgnoreCase("master")) {
			clientInstance.interfaceServer = Naming.lookup("rmi://" + args[0] + "/server");

			Naming.rebind("rmi://" + args[1] + "/master", clientInstance);

			String teamName = args[2];

			System.out.println("Client starting, listens on IP " + args[1] + " for server callback.");

			while (clientInstance.slaves.size() != clientInstance.N_SLAVES) {
				Thread.sleep(10);
			}

			// Create a communication handler and register it with the server
			// The communication handler is the object that will receive the tasks from the server
			ClientCommHandler cch = new ClientCommHandler();
			System.out.println("Client registers with the server");
			((ServerCommInterface)clientInstance.interfaceServer).register(teamName, cch);

			// Now forever solve tasks given by the server
			while (true) {
				// Wait until getting a problem from the server
				while (cch.currProblem == null) {
					Thread.sleep(1);
				}
				clientInstance.problem = cch.currProblem;
				//Distribute problem to slaves
				int segment = cch.currProblemSize / (clientInstance.slaves.size() + 1);

				for(int i = 0; i< clientInstance.slaves.size(); i++ ) {
					try {
						clientInstance.slaves.get(i).passProblem(clientInstance.problem, (segment * i));
					} catch (Exception e) {
						System.out.println("Could not give the problem to some client");
					}
				}

				clientInstance.doHashMaster();
				cch.currProblem = null;
			}

		} else {
			clientInstance.interfaceServer = Naming.lookup("rmi://" + args[0] + "/master");

			Naming.rebind("rmi://" + args[1] + "/slave", clientInstance);

			System.out.println("Slave registers with the master");
			((MasterCommInterface) clientInstance.interfaceServer).subscribe(args[1]);

			// Wait until getting a problem from the master
			while(true) {
				while (clientInstance.problem == null) {
					Thread.sleep(1);
				}

				clientInstance.doHashSlave();
				clientInstance.problem = null;
			}

		}

	}

	public void doHashMaster () throws Exception {

		MessageDigest md = MessageDigest.getInstance("MD5");

		if (map.get(problem) != null)
			((ServerCommInterface)interfaceServer).submitSolution(teamName, String.valueOf(map.get(problem)));
		else {
			while (true) {
				// Calculate their hash
				byte[] currentHash = md.digest(index.toString().getBytes());
				// If the calculated hash equals the one given by the server, submit the integer as solution
				map.put(currentHash, index);
				if (Arrays.equals(currentHash, problem)) {
					System.out.println("client submits solution");
					((ServerCommInterface)interfaceServer).submitSolution(teamName, index.toString());
					index++;
					break;
				}
				index++;
			}

		}

	}

	public void doHashSlave () throws Exception {

		MessageDigest md = MessageDigest.getInstance("MD5");

		if (map.get(problem) != null)
			((MasterCommInterface)interfaceServer).passSolution(String.valueOf(map.get(problem)));
		else {
			while (true) {
				// Calculate their hash
				byte[] currentHash = md.digest(index.toString().getBytes());
				// If the calculated hash equals the one given by the server, submit the integer as solution
				map.put(currentHash, index);
				if (Arrays.equals(currentHash, problem)) {
					System.out.println("client submits solution");
					((MasterCommInterface)interfaceServer).passSolution(String.valueOf(map.get(problem)));
					index++;
					break;
				}
				index++;
			}

		}

	}

	//MasterCommInterface methods
	@Override
	public void passSolution(String solution) throws Exception {
		((ServerCommInterface)interfaceServer).submitSolution(teamName, String.valueOf(map.get(problem)));
	}

	@Override
	public void subscribe(String ip) throws MalformedURLException, NotBoundException, RemoteException {
		SlaveCommInterface sci = (SlaveCommInterface) Naming.lookup("rmi://" + ip + "/slave");
		slaves.add(sci);
	}

	//SlaveCommInterface methods
	@Override
	public void passProblem(byte[] problem, int index) {
		if (problem == null)
			System.out.println("Problem is empty!");
		else
			System.out.println("Client received new problem");
		this.problem = problem;
		this.index = index;
	}

}
