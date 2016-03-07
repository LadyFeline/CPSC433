import java.io.*;
import java.net.*;
import java.util.*;

abstract class MyServer {
	public static int serverPort = 6789;
	public static String WWW_ROOT = "/Users/ladyfeline/Desktop/";
	public static int numberOfThreads = 10;
	public static int connNum = 0;
	static HashMap<String, filePiece> Cache;
	
	public MyServer(int port, String root, int NumOfThreads){
		this.serverPort = port;
		this.WWW_ROOT = root;
		this.numberOfThreads = NumOfThreads;
		this.Cache = new HashMap<String, filePiece>();
	} 

	protected static void run() throws IOException {}

} // end of class WebServer

class SequentialServer extends MyServer {
	public SequentialServer(int port, String root, int NumOfThreads) {
		super(port, root, NumOfThreads);
		// TODO Auto-generated constructor stub
	}

	protected static void run() throws IOException {
		// create server socket
		ServerSocket listenSocket = new ServerSocket(serverPort);
		System.out.println("server listening at: " + listenSocket);
		System.out.println("server www root: " + WWW_ROOT);
		HashMap<String, filePiece> Cache = new HashMap<String, filePiece>();
		while (true) {

			try {
				// take a ready connection from the accepted queue
				Socket connectionSocket = listenSocket.accept();
				connNum++;
				System.out.println("\nReceive request from " + connectionSocket);

				// process a request
				SequentialHandler wrh = new SequentialHandler(connectionSocket, WWW_ROOT, Cache);
				wrh.processRequest();
			} catch (Exception e) {

			}
		} // end of while (true)
	}
}

class MultiThreadServer extends MyServer {
	public MultiThreadServer(int port, String root, int NumOfThreads) {
		super(port, root, NumOfThreads);
		// TODO Auto-generated constructor stub
	}

	protected static void run() throws IOException {
		// create server socket
		ServerSocket listenSocket = new ServerSocket(serverPort);
		System.out.println("server listening at: " + listenSocket);
		System.out.println("server www root: " + WWW_ROOT);

		while (true) {

			try {
				// take a ready connection from the accepted queue
				Socket connectionSocket = listenSocket.accept();
				System.out.println("\nReceive request from " + connectionSocket);

				// process a request
				MultiThreadHandler wrh = new MultiThreadHandler(connectionSocket, WWW_ROOT, Cache);
				Thread t = new Thread(wrh);
				t.start();

			} catch (Exception e) {

			}
		} // end of while (true)
	}
}

class ThreadPoolWithCompetingSocketServer extends MyServer{
	public ThreadPoolWithCompetingSocketServer(int port, String root, int NumOfThreads) {
		super(port, root, NumOfThreads);
		// TODO Auto-generated constructor stub
	}

	protected static void run() throws IOException {
		ServerSocket listenSocket = new ServerSocket(serverPort);
		System.out.println("server listening at: " + listenSocket);
		System.out.println("server www root: " + WWW_ROOT);
		Thread[] threads = new Thread[numberOfThreads];
		ThreadPoolWithCompetingSocketHandler[] handlers = new ThreadPoolWithCompetingSocketHandler[numberOfThreads];
		for (int i = 0; i < numberOfThreads; i++){
			try {
				handlers[i] = new ThreadPoolWithCompetingSocketHandler(listenSocket, WWW_ROOT, Cache);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			threads[i] = new Thread(handlers[i]);
			threads[i].start();
		}
	}
}

class ThreadPoolWithBusyWaitingServer extends MyServer{
	public ThreadPoolWithBusyWaitingServer(int port, String root, int NumOfThreads) {
		super(port, root, NumOfThreads);
		// TODO Auto-generated constructor stub
	}

	protected static void run() throws IOException {
		try {
			// create server socket
			ServerSocket listenSocket = new ServerSocket(serverPort);
			System.out.println("server listening at: " + listenSocket);
			System.out.println("server www root: " + WWW_ROOT);

			Vector<Socket> connSockPool = new Vector<Socket>();

			// create thread pool
			ThreadPoolWithBusyWaitingHandler[] handlers = new ThreadPoolWithBusyWaitingHandler[numberOfThreads];
			Thread[] threads = new Thread[numberOfThreads];

			// start all threads
			for (int i = 0; i < threads.length; i++) {
				handlers[i] = new ThreadPoolWithBusyWaitingHandler(connSockPool, listenSocket, WWW_ROOT, Cache);
				threads[i] = new Thread(handlers[i]);
				threads[i].start();
			}
			
			// accept new requests and update pool
			while (true) {
				try {
					// accept connection from connection queue
					Socket connSock = listenSocket.accept();
					
					// how to assign to an idle thread?
					synchronized (connSockPool) {
						connSockPool.add(connSock);
					} // end of sync
				} catch (Exception e) {
					System.out.println("server run failed.");
				} // end of catch
			} // end of loop
		} catch (Exception e) {
			System.out.println("Server construction failed.");
		} // end of catch
	}
}

class ThreadPoolWithSuspensionServer extends MyServer{
	public ThreadPoolWithSuspensionServer(int port, String root, int NumOfThreads) {
		super(port, root, NumOfThreads);
		// TODO Auto-generated constructor stub
	}

	protected static void run() throws IOException {
		try {
			// create server socket
			ServerSocket listenSocket = new ServerSocket(serverPort);
			System.out.println("server listening at: " + listenSocket);
			System.out.println("server www root: " + WWW_ROOT);

			Vector<Socket> connSockPool = new Vector<Socket>();

			// create thread pool
			ThreadPoolWithSuspensionHandler[] handlers = new ThreadPoolWithSuspensionHandler[numberOfThreads];
			Thread[] threads = new Thread[numberOfThreads];

			// start all threads
			for (int i = 0; i < threads.length; i++) {
				handlers[i] = new ThreadPoolWithSuspensionHandler(connSockPool, listenSocket, WWW_ROOT, Cache);
				threads[i] = new Thread(handlers[i]);
				threads[i].start();
			}
			
			while (true) {
				try {
					// accept connection from connection queue
					Socket connSock = listenSocket.accept();

					// how to assign to an idle thread?
					synchronized (connSockPool) {
						connSockPool.add(connSock);
						connSockPool.notifyAll();
					} // end of sync
				} catch (Exception e) {
					System.out.println("server run failed.");
				} // end of catch
			} // end of loop
		} catch (Exception e) {
			System.out.println("Server construction failed.");
		} // end of catch

	}
}