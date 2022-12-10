package client;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Client implements MasterCommInterface, SlaveCommInterface {

	static boolean solutionFound = false;

	static List<SlaveCommInterface> slaves = new ArrayList<>();
	static final int N_SLAVES = 1;

	static byte[] problem = null;
	static Integer index = 0;

	static ServerCommInterface interfaceServer;

	static HashMap<byte[], Integer> map = new HashMap<>();

	static String teamName;

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

		// This is crucial, otherwise the RPis will not be reachable from the server/master.
		System.setProperty("java.rmi.server.hostname", args[1]);

		interfaceServer = (ServerCommInterface) Naming.lookup("rmi://" + args[0] + "/server");

		String teamName = null;
		if (type.equalsIgnoreCase("master")) {
			teamName = args[2];

			System.out.println("Client starting, listens on IP " + args[1] + " for server callback.");

			while (slaves.size() != N_SLAVES) {
				Thread.sleep(10);
			}

			// Create a communication handler and register it with the server
			// The communication handler is the object that will receive the tasks from the server
			ClientCommHandler cch = new ClientCommHandler();
			System.out.println("Client registers with the server");
			((ServerCommInterface) interfaceServer).register(teamName, cch);

			// Now forever solve tasks given by the server
			while (true) {
				// Wait until getting a problem from the server
				while (cch.currProblem == null) {
					Thread.sleep(1);
				}

				int segment = cch.currProblemSize / (slaves.size() + 1);

				for(int i = 0; i< slaves.size(); i++ ) {
					try {
						slaves.get(i).passProblem(cch.currProblem, (segment * i));
					} catch (Exception e) {
						System.out.println("Could not give the problem to some client");
					}
				}

				solutionFound = false;

				doHashMaster();

				cch.currProblem = null;
			}

		} else {

			while(true) {

				solutionFound = false;

				((MasterCommInterface) interfaceServer).subscribe(args[1]);

				doHashSlave();
			}

		}




//		}
	}

	public static void doHashMaster () throws Exception {

		MessageDigest md = MessageDigest.getInstance("MD5");

		if (map.get(problem) != null)
			interfaceServer.submitSolution(teamName, String.valueOf(map.get(problem)));
		else {
			Integer i = index;
			while (!solutionFound) {
				// Calculate their hash
				byte[] currentHash = md.digest(i.toString().getBytes());
				// If the calculated hash equals the one given by the server, submit the integer as solution
				map.put(currentHash, i);
				if (Arrays.equals(currentHash, problem)) {
					System.out.println("client submits solution");
					interfaceServer.submitSolution(teamName, i.toString());
					break;
				}

				i++;
			}

		}

		//check if a client has a solution and check i fhthe problem has changed

	}

	public static void doHashSlave () throws Exception {

		MessageDigest md = MessageDigest.getInstance("MD5");

		HashMap<byte[], Integer> map = new HashMap<>();

		if (map.get(problem) != null)
			((MasterCommInterface)interfaceServer).passSolution(String.valueOf(map.get(problem)));
		else {
			Integer i = index;
			while (!solutionFound) {
				// Calculate their hash
				byte[] currentHash = md.digest(i.toString().getBytes());
				// If the calculated hash equals the one given by the server, submit the integer as solution
				map.put(currentHash, i);
				if (Arrays.equals(currentHash, problem)) {
					System.out.println("client submits solution");
					((MasterCommInterface)interfaceServer).passSolution(String.valueOf(map.get(problem)));
					break;
				}

				i++;
			}

		}

	}

	@Override
	public void passSolution(String solution) throws Exception {
		interfaceServer.submitSolution(teamName, String.valueOf(map.get(problem)));
		solutionFound=true;
	}

	@Override
	public void subscribe(String ip) throws MalformedURLException, NotBoundException, RemoteException {
		SlaveCommInterface sci = (SlaveCommInterface) Naming.lookup("rmi://" + ip + "/server");
		slaves.add(sci);
	}

	@Override
	public void passProblem(byte[] problem, int index) {
		Client.problem = problem;
		Client.index = index;
	}
}
