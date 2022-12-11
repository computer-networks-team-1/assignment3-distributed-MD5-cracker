package client;

import server.ServerCommInterface;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client extends UnicastRemoteObject implements MasterCommInterface, SlaveCommInterface {

	private List<SlaveCommInterface> slaves = new ArrayList<>();
	private int N_SLAVES = 0;

	private byte[] problem = null;
	private Integer index_start = 0;
	private Integer index_end = 0;

	private Remote interfaceServer;

//	private HashMap<byte[], Integer> map = new HashMap<>();

	private String teamName;

	private boolean solutionFound = false;

	protected Client() throws RemoteException {
	}

	public static void main(String[] args) throws Exception {

		if (args.length < 4) {
			System.out.println("Proper Usage is: java Client serverAddress yourOwnIPAddress teamname type <n_slaves>");
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

			clientInstance.teamName = args[2];

			String nSlaves = args[4];
			clientInstance.N_SLAVES = Integer.parseInt(nSlaves);

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
				clientInstance.solutionFound = false;

				if (clientInstance.problem == cch.currProblem) { //clientInstance solved the problem
					cch.currProblem = null;
				}

				for(int i = 0; i< clientInstance.slaves.size(); i++ ) {
					try {
						clientInstance.slaves.get(i).announceSuccess();
					} catch (Exception e) {
						System.out.println("Could not announce success to some client");
					}
				}

				// Wait until getting a problem from the server
				while (cch.currProblem == null) {
					Thread.sleep(1);
				}
				clientInstance.problem = cch.currProblem;

				//Distribute problem to slaves
				int segment = cch.currProblemSize / (clientInstance.slaves.size() + 1);

				for(int i = 0; i< clientInstance.slaves.size(); i++ ) {
					try {

						clientInstance.slaves.get(i).passProblem(clientInstance.problem, (segment * i), (segment * (i+1)-1));
					} catch (Exception e) {
						System.out.println("Could not give the problem to some client");
					}
				}
				clientInstance.index_start = segment * clientInstance.slaves.size();
				clientInstance.index_end = cch.currProblemSize-1;

				clientInstance.doHashMaster(cch);
			}

		} else {
			clientInstance.interfaceServer = Naming.lookup("rmi://" + args[0] + "/master");

			System.out.println("Slave registers with the master");
			((MasterCommInterface) clientInstance.interfaceServer).subscribe(clientInstance);

			// Wait until getting a problem from the master
			while(true) {
				clientInstance.solutionFound = false;
				while (clientInstance.problem == null) {
					Thread.sleep(1);
				}

				clientInstance.doHashSlave();
			}

		}

	}

	public void doHashMaster (ClientCommHandler cch) throws Exception {

		MessageDigest md = MessageDigest.getInstance("MD5");

		Integer index = index_start;

//		if (map.get(problem) != null)
//			((ServerCommInterface)interfaceServer).submitSolution(teamName, String.valueOf(map.get(problem)));
//		else {
			while (!solutionFound && this.problem == cch.currProblem) {
				// Calculate their hash
				byte[] currentHash = md.digest(index_start.toString().getBytes());
				// If the calculated hash equals the one given by the server, submit the integer as solution
//				map.put(currentHash, index);
				if (Arrays.equals(currentHash, problem)) {
					System.out.println("Master client submits solution");
					((ServerCommInterface)interfaceServer).submitSolution(teamName, index.toString());
					solutionFound = true;
				}
				index++;

				if(index == index_end+1)
					index = index_start;
			}

//		}

	}

	public void doHashSlave () throws Exception {

		MessageDigest md = MessageDigest.getInstance("MD5");

		Integer index = index_start;

//		if (map.get(problem) != null)
//			((MasterCommInterface)interfaceServer).passSolution(String.valueOf(map.get(problem)));
//		else {
			while (!solutionFound) {
				// Calculate their hash
				byte[] currentHash = md.digest(index_start.toString().getBytes());
				// If the calculated hash equals the one given by the server, submit the integer as solution
//				map.put(currentHash, index);
				if (Arrays.equals(currentHash, problem)) {
					System.out.println("slave client submits solution");
					((MasterCommInterface)interfaceServer).passSolution((index.toString()));
					solutionFound = true;
					problem = null;
				}
				index++;
				if(index == index_end+1)
					index = index_start;
			}

//		}

	}

	//MasterCommInterface methods
	@Override
	public void passSolution(String solution) throws Exception {
		((ServerCommInterface)interfaceServer).submitSolution(teamName, solution);
		solutionFound = true;
	}

	@Override
	public void subscribe(SlaveCommInterface sci) throws MalformedURLException, NotBoundException, RemoteException {
		slaves.add(sci);
	}

	//SlaveCommInterface methods
	@Override
	public void passProblem(byte[] problem, int index, int index_end) throws RemoteException{
		if (problem == null)
			System.out.println("Problem is empty!");
		else
			System.out.println("Client received new problem");
		this.problem = problem;
//		if (index == 0)
		this.index_start = index;
		this.index_end = index_end;
	}

	@Override
	public void announceSuccess() throws RemoteException {
		solutionFound = true;
	}

}
