import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import java.lang.ProcessBuilder.Redirect;

class filePiece {
	byte[] content;
	long lastModifyTime;

	public filePiece(byte[] content, long lastModifyTime) {
		this.content = content;
		this.lastModifyTime = lastModifyTime;
	}
}

abstract class Handler {

	static boolean _DEBUG = true;
	static int reqCount = 0;
	int overload = 1000;

	String WWW_ROOT;
	Socket connSocket;
	ServerSocket welcome;
	BufferedReader inFromClient;
	DataOutputStream outToClient;
	Vector<Socket> pool;
	HashMap<String, filePiece> Cache;

	String urlName;
	String fileName;
	File fileInfo;
	boolean runnable = false;
	boolean mobile = false;
	public Date modifiedDate;
	int connNum;

	public Handler(Socket connectionSocket, String WWW_ROOT, HashMap<String, filePiece> Cache) throws Exception {
		reqCount++;
		this.WWW_ROOT = WWW_ROOT;
		this.connSocket = connectionSocket;
		this.Cache = Cache;
	}

	public Handler(ServerSocket listenSocket, String WWW_ROOT, HashMap<String, filePiece> Cache) throws Exception {
		reqCount++;
		this.WWW_ROOT = WWW_ROOT;
		this.welcome = listenSocket;
		this.Cache = Cache;
	}

	public Handler(Vector<Socket> connSockPool, ServerSocket listenSocket, String WWW_ROOT,
			HashMap<String, filePiece> Cache) throws Exception {
		this.pool = connSockPool;
		this.WWW_ROOT = WWW_ROOT;
		this.welcome = listenSocket;
		this.Cache = Cache;
	}

	public void processRequest() {
		try {
			mapURL2File();

			// found the file and knows its info
			if (fileInfo != null) {
				if (runnable) {
					System.out.println("Start cgi program..");
					ProcessBuilder pb = new ProcessBuilder("perl", fileName);
					Map<String, String> env = pb.environment();
					env.put("INDEX", Integer.toString(reqCount));
					pb.redirectOutput(Redirect.PIPE);
					Process p = pb.start();
					outputCGIResult(p.getInputStream());
				} else {
					outputResponseHeader();
					outputResponseBody();
				}
			} else {
				DEBUG("NULL FILE");// dod not handle error
			}

			connSocket.close();
		} catch (Exception e) {
			outputError(400, "Server error");
		}
	} // end of processARequest

	// HTTP/1.0 <StatusCode> <message>
	// Date: <date>
	// Server: <your server name>
	// Content-Type: text/html
	// Content-Length: <LengthOfFile>
	// CRLF
	// <file content>
	protected void mapURL2File() throws Exception {

		inFromClient = new BufferedReader(new InputStreamReader(connSocket.getInputStream()));
		outToClient = new DataOutputStream(connSocket.getOutputStream());

		String requestMessageLine = inFromClient.readLine();
		DEBUG("Request " + reqCount + ": " + requestMessageLine);

		if (requestMessageLine.startsWith("GET")) {
			String[] request = requestMessageLine.split("\\s");

			if (request.length < 2 || !request[0].equals("GET")) {
				outputError(500, "Bad request");
				return;
			}

			// parse URL to retrieve file name
			urlName = request[1];
			if (urlName.startsWith("/") == true)
				urlName = urlName.substring(1);
		} else if (requestMessageLine.startsWith("User-Agent")) {
			String agent = requestMessageLine
					.substring(requestMessageLine.indexOf(':') + 1, requestMessageLine.length()).toLowerCase();
			if (agent.indexOf("iphone") != -1 || agent.indexOf("android") != -1) {
				mobile = true;
			}
		} else if (requestMessageLine.startsWith("If-Modified-Since")) {
			String time = requestMessageLine.substring(requestMessageLine.indexOf(':') + 1,
					requestMessageLine.length());
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			this.modifiedDate = dateFormat.parse(time);
		}

		if (urlName.equals("load")) {
			// this is a heart beat request from load balancer
			if (this.connNum > overload) {
				outputError(503, "Overloading");
			} else {
				outputError(200, "Success");
			}
			return;
		}
		// process the request

		if (urlName.endsWith(".cgi")) {
			// runnable file
			this.runnable = true;
		}

		if (urlName.endsWith("/") == true) {
			if (mobile == true) {
				urlName = "index_m.html";
			} else {
				urlName = "index.html";
			}
		}

		// map to file name
		fileName = WWW_ROOT + urlName;
		DEBUG("Map to File name: " + fileName);
		fileInfo = new File(fileName);

		if (!fileInfo.exists()) {
			if (mobile == true && urlName.equals("index_m.html")) {
				fileName = WWW_ROOT + "index.html";
				fileInfo = new File(fileName);
				if (!fileInfo.exists()) {
					outputError(404, "Not Found");
				}
			} else {
				outputError(404, "Not Found");
			}
			fileInfo = null;
			return;
		}
		
		if (modifiedDate != null) {
			if (fileInfo.lastModified() < modifiedDate.getTime()) {
				outputError(304, "Not Modified");
				fileInfo = null;
				return;
			}
		}

	} // end mapURL2file

	protected void outputResponseHeader() throws Exception {
		outToClient.writeBytes("HTTP/1.0 200 Document Follows\r\n");
		outToClient.writeBytes("Set-Cookie: MyCool433Seq12345\r\n");

		if (urlName.endsWith(".jpg"))
			outToClient.writeBytes("Content-Type: image/jpeg\r\n");
		else if (urlName.endsWith(".gif"))
			outToClient.writeBytes("Content-Type: image/gif\r\n");
		else if (urlName.endsWith(".html") || urlName.endsWith(".htm"))
			outToClient.writeBytes("Content-Type: text/html\r\n");
		else
			outToClient.writeBytes("Content-Type: text/plain\r\n");
	}

	protected void outputResponseBody() throws Exception {
		System.out.println("Cache size: " + Cache.keySet().size());
		// DEBUG("Cache size " + Cache.keySet().size());

		int numOfBytes = (int) fileInfo.length();
		outToClient.writeBytes("Content-Length: " + numOfBytes + "\r\n");
		outToClient.writeBytes("\r\n");

		if (!Cache.containsKey(fileName) || Cache.get(fileName).lastModifyTime < fileInfo.lastModified()) {
			FileInputStream fileStream = new FileInputStream(fileName);
			byte[] fileInBytes = new byte[numOfBytes];
			long timeStamp = fileInfo.lastModified();
			fileStream.read(fileInBytes);
			Cache.put(fileName, new filePiece(fileInBytes, timeStamp));
			System.out.println("file read complete");
			outToClient.write(fileInBytes, 0, numOfBytes);
		} else {
			System.out.println("file retrieved from cache");
			byte[] fileInBytes = Cache.get(fileName).content;
			outToClient.write(fileInBytes, 0, numOfBytes);
		}
	}

	protected void outputCGIResult(InputStream inFromProgram) throws Exception {
		byte[] buf = new byte[10000];
		int bufLength = inFromProgram.read(buf);
		inFromProgram.close();
		outToClient.writeBytes("HTTP/1.0 200 Document Follows\r\n");
		outToClient.writeBytes("Content-Type: text/html\r\n");
		outToClient.writeBytes("Content-Length: " + buf.length + "\r\n");
		outToClient.writeBytes("\r\n");
		outToClient.write(buf, 0, bufLength);
	}

	void outputError(int errCode, String errMsg) {
		try {
			outToClient.writeBytes("HTTP/1.0 " + errCode + " " + errMsg + "\r\n");
		} catch (Exception e) {
		}
	}

	static void DEBUG(String s) {
		if (_DEBUG)
			System.out.println(s);
	}
}

class SequentialHandler extends Handler {

	public SequentialHandler(Socket connectionSocket, String WWW_ROOT, HashMap<String, filePiece> Cache)
			throws Exception {
		super(connectionSocket, WWW_ROOT, Cache);
		// TODO Auto-generated constructor stub
	}

}

class MultiThreadHandler extends Handler implements Runnable {

	public MultiThreadHandler(Socket connectionSocket, String WWW_ROOT, HashMap<String, filePiece> Cache)
			throws Exception {
		super(connectionSocket, WWW_ROOT, Cache);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		processRequest();
	}
}

class ThreadPoolWithCompetingSocketHandler extends Handler implements Runnable {

	public ThreadPoolWithCompetingSocketHandler(ServerSocket listenSocket, String WWW_ROOT,
			HashMap<String, filePiece> Cache) throws Exception {
		super(listenSocket, WWW_ROOT, Cache);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			while (true) {
				synchronized (welcome) {
					connSocket = welcome.accept();
				}
				processRequest();
				connSocket.close();
			} // end of while

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

class ThreadPoolWithBusyWaitingHandler extends Handler implements Runnable {

	public ThreadPoolWithBusyWaitingHandler(Vector<Socket> connSockPool, ServerSocket listenSocket, String WWW_ROOT,
			HashMap<String, filePiece> Cache) throws Exception {
		super(connSockPool, listenSocket, WWW_ROOT, Cache);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (true) {
			// get a new request connection
			connSocket = null;
			while (connSocket == null) {
				synchronized (pool) {
					if (!pool.isEmpty()) {
						// remove the first request
						connSocket = (Socket) pool.remove(0);
						System.out.println("Thread " + this + " process request " + connSocket);
					} // end if
				} // end of sync
			} // end while
			processRequest();
		} // end while(true)
	}

}

class ThreadPoolWithSuspensionHandler extends Handler implements Runnable {
	public ThreadPoolWithSuspensionHandler(Vector<Socket> connSockPool, ServerSocket listenSocket, String WWW_ROOT,
			HashMap<String, filePiece> Cache) throws Exception {
		super(connSockPool, listenSocket, WWW_ROOT, Cache);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		while (true) {
			// get a new request connection
			connSocket = null;

			while (connSocket == null) {
				synchronized (pool) {
					if (!pool.isEmpty()) {
						// remove the first request
						reqCount++;
						connSocket = (Socket) pool.remove(0);
					} else
						try {
							pool.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				} // end of sync
			} // end while
			processRequest();
		} // end while(true)
	}
}
