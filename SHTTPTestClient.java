import java.io.*;
import java.net.*;
import java.util.*;

public class SHTTPTestClient implements Runnable {
	private static String ServerName = "123";
	private static String Server = "localhost";
	private static int ServerPort = 6789;
	private static int ThreadNumber = 10;
	private static String FileName = "/Users/ladyfeline/Documents/ComputerNetworks/HW3/SHTTP/src/1.txt";
	private static long TimeOfTest = 10;
	private static boolean _DEBUG = true;
	private long StartTime;
	private long seq;
	private long waitTime = 0;
	private int FilesGot = 0;
	private int BytesGot = 0;

	public SHTTPTestClient(int seq) {
		this.seq = seq;
		this.StartTime = System.currentTimeMillis();
	}

	public static void main(String args[]) throws IOException, InterruptedException {
		
		long start = System.currentTimeMillis();
		
		for (int i = 0; i < args.length; i = i + 2)
			update(args[i], args[i + 1]);

		Thread[] threads = new Thread[ThreadNumber];
		SHTTPTestClient[] clients = new SHTTPTestClient[ThreadNumber];

		for (int i = 0; i < ThreadNumber; i++) {
			clients[i] = new SHTTPTestClient(i);
			threads[i] = new Thread(clients[i]);
			threads[i].start();
		}

		for (int i = 0; i < ThreadNumber; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException ex) {

			}
		}
		
		long end = System.currentTimeMillis();
		
		long SumWaitTime = 0;
		int SumFilesGot = 0;
		int SumBytesGot = 0;
		double AvgFiles = 1, AvgBytes, AvgWaitTime;

		for (int i = 0; i < ThreadNumber; i++) {
			SumWaitTime += clients[i].waitTime;
			SumFilesGot += clients[i].FilesGot;
			SumBytesGot += clients[i].BytesGot;
		}
		
		if (_DEBUG) System.out.println("Total waiting time: " + SumWaitTime);
		if (_DEBUG) System.out.println("Total files downloaded: " + SumFilesGot);
		if (_DEBUG) System.out.println("Total bytes got: " + SumBytesGot);
		if (_DEBUG) System.out.println("Time duration: " + (end-start));

		AvgFiles = SumFilesGot / ((float)(end-start)/1000);
		if (_DEBUG) System.out.println("Average number of downloaded files: " + AvgFiles + " files/s");
		AvgBytes = SumBytesGot / ((float)(end-start)/1000);
		if (_DEBUG) System.out.println("Average bytes of downloaded files: " + AvgBytes +" bytes/s");
		AvgWaitTime = SumWaitTime / AvgFiles;
		if (_DEBUG) System.out.println("Average waiting time of each file: " + AvgWaitTime + " ms");

//		System.out.println("files/second: " + AvgFiles);
//		System.out.println("bytes/second: " + AvgBytes);
//		System.out.println("waitTime/file: " + AvgWaitTime);
	}

	// Construct our packet
	private String ConstructMessage(String file) {
		// GET <URL> HTTP/1.0
		// Host: <ServerName>
		// CRLF
		String mes = "GET " + file + " HTTP/1.0\r\n" + "Host: " + ServerName + "\r\n\r\n";
		if (_DEBUG) System.out.println(mes);
		return mes;
	}

	public void run() {
		try {
			List<String> filesList = new ArrayList<String>();
			FileInputStream fis = new FileInputStream(FileName);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			while ((line = br.readLine()) != null) {
				filesList.add(line);
			}
			br.close();

			while (true) {
				if (System.currentTimeMillis() - StartTime >= TimeOfTest * 1000) 
					break;
				for (String filename : filesList) {
					InetAddress serverIPAddress = InetAddress.getByName(Server);
					Socket clientSocket = new Socket(serverIPAddress, ServerPort);
					long start = System.currentTimeMillis();
					
					// Send a message
					String Message = ConstructMessage(filename);
					DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
					outToServer.writeBytes(Message);
					
					// Receive from server
					BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					
					// block until getting first data 
					StringBuffer sb = new StringBuffer();
					sb.append((char) inFromServer.read());
					
					long end = System.currentTimeMillis();
					// read the following data
					char[] buf = new char[1024];
					while(inFromServer.read(buf) != -1) {
						sb.append(buf);
					}
					clientSocket.close();
					this.waitTime += (end - start);
					this.FilesGot++;
					this.BytesGot += sb.length();
				}
			}
			

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// update parameters read from command lines
	
	public static void update(String command, String content) {
		switch (command) {
		case "-server":
			try {
				Server = content;
				System.out.println("-server");
				System.out.println(Server);
			} catch (NumberFormatException ex) {

			}
			break;
		case "-servname":
			try {
				ServerName = content;
				System.out.println("-servname");
				System.out.println(ServerName);
			} catch (NumberFormatException ex) {

			}
			break;
		case "-port":
			try {
				ServerPort = Integer.parseInt(content);
				System.out.println("-port");
				System.out.println(ServerPort);
			} catch (NumberFormatException ex) {

			}
			break;
		case "-parallel":
			try {
				ThreadNumber = Integer.parseInt(content);
				System.out.println("-parallel");
				System.out.println(ThreadNumber);
			} catch (NumberFormatException ex) {

			}
			break;
		case "-files":
			try {
				FileName = content;
				System.out.println("-files");
				System.out.println(FileName);
			} catch (NumberFormatException ex) {

			}
			break;
		case "-T":
			try {
				TimeOfTest = Long.parseLong(content);
				System.out.println("-T");
				System.out.println(TimeOfTest);
			} catch (NumberFormatException ex) {

			}
			break;
		default:
			break;
		}
	}
}
