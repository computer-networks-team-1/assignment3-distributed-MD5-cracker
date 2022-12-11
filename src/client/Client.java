package client;

import server.ServerCommInterface;

import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client extends UnicastRemoteObject implements MasterCommInterface, SlaveCommInterface {

	private List<SlaveCommInterface> slaves = new ArrayList<>();
	private final int N_SLAVES = 0; //TODO should this be a commandline argument?

	private byte[] problem = null;
	private Integer index = 0;
	private Integer segmentSize = 0;

	private Remote interfaceServer; //Problem server or Master-client

	private String teamName;

	private final MessageDigest md = MessageDigest.getInstance("MD5");

	protected Client() throws RemoteException, NoSuchAlgorithmException {
	}

	public static void main(String[] args) throws Exception {

		checkCommandLineArgs(args);

		String type = args[3];

		LocateRegistry.createRegistry(1099);
		Client clientInstance = new Client();

		// This is crucial, otherwise the RPis will not be reachable from the server/master.
		System.setProperty("java.rmi.server.hostname", args[1]);


		if (type.equalsIgnoreCase("master")) {
			clientInstance.interfaceServer = Naming.lookup("rmi://" + args[0] + "/server");

			Naming.rebind("rmi://" + args[1] + "/master", clientInstance);

			clientInstance.teamName = args[2];

			System.out.println("Master client starting, listens on IP " + args[1] + " for server callback.");

			while (clientInstance.slaves.size() != clientInstance.N_SLAVES) {
				Thread.sleep(10);
			}
			System.out.println("All slaves registered with the master");

			// Create a communication handler and register it with the server
			// The communication handler is the object that will receive the tasks from the server
			ClientCommHandler cch = new ClientCommHandler();

			((ServerCommInterface)clientInstance.interfaceServer).register(clientInstance.teamName, cch);
			System.out.println("Master client registered with the server");

			// Now forever solve tasks given by the server
			while (true) {

				clientInstance.problem = cch.currProblem;

				//Distribute problem to slaves
				clientInstance.segmentSize = cch.currProblemSize / (clientInstance.slaves.size() + 1);

				for(int i = 0; i< clientInstance.slaves.size(); i++ ) {
					try {
						SlaveCommInterface currSlave = clientInstance.slaves.get(i);
						currSlave.setProblemSpace(clientInstance.segmentSize * i, clientInstance.segmentSize); //TODO overhead calculations. Could be optimized
						currSlave.passProblem(cch.currProblem);
					} catch (Exception e) {
						System.out.println("Could not pass the problem to some client");
					}
				}

				clientInstance.doHashMaster(cch);
			}

		} else { //type == slave
			clientInstance.interfaceServer = Naming.lookup("rmi://" + args[0] + "/master");

			((MasterCommInterface)clientInstance.interfaceServer).subscribe(clientInstance);
			System.out.println("Slave client registered with the master client");

			//Await first problem. This should only be necessary for first problem
			while (clientInstance.problem == null) {
				Thread.sleep(1);
			}

			// Just go and solve problems continuously
			clientInstance.doHashSlave();
		}

	}

	private static void checkCommandLineArgs(String[] args) {
		if (args.length < 4) {
			System.out.println("Proper Usage is: java Client serverAddress yourOwnIPAddress teamname type");
			System.exit(0);
		}

		if (
				!(args[3].equalsIgnoreCase("slave") ||
						args[3].equalsIgnoreCase("master"))
		) {
			System.out.println("type has to be either 'master' or 'slave'");
			System.exit(0);
		}
	}

	public void doHashMaster (ClientCommHandler cch) throws Exception {

		int i = index;
		while (this.problem == cch.currProblem) {
			byte[] currentHash = md.digest(index.toString().getBytes());
			if (Arrays.equals(currentHash, problem)) {
				((ServerCommInterface)interfaceServer).submitSolution(teamName, index.toString());
				System.out.println("Master client submitted solution");
			}
			//Suppose that search space is made up by equally sized segments
			//We go through search space in a loop
			if ((i + 1) <= (index + segmentSize)) {
				i++;
			} else {
				i = index;
			}
		}

	}

	public void doHashSlave () throws Exception {

		int i = index;
		while (true) {
			byte[] currentHash = md.digest(Integer.valueOf(i).toString().getBytes());
			if (Arrays.equals(currentHash, problem)) {
				((MasterCommInterface)interfaceServer).passSolution(String.valueOf(index));
				System.out.println("Slave client submitted solution to Master client");
			}
			//Suppose that search space is made up by equally sized segments
			//We go through search space in a loop
			if ((i + 1) <= (index + segmentSize)) {
				i++;
			} else {
				i = index;
			}
		}

	}

	//MasterCommInterface methods
	@Override
	public void passSolution(String sol) throws Exception {
		((ServerCommInterface) interfaceServer).submitSolution(teamName, sol);
	}

	@Override
	public void subscribe(SlaveCommInterface sci) {
		slaves.add(sci);
	}

	//SlaveCommInterface methods
	@Override
	public void passProblem(byte[] problem) {
		this.problem = problem;
	}

	@Override
	public void setProblemSpace(int index, int segmentSize) {
		this.index = index;
		this.segmentSize = segmentSize;
	}
}
