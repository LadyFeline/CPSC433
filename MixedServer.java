import java.io.BufferedReader;
import java.io.FileReader;

public class MixedServer {
	public static String config_file;
	public static int serverPort = 6789;
	public static int cacheSize = 1000;
	public static String serverName = "localhost";
	public static String WWW_ROOT = "/Users/ladyfeline/Desktop/";
	public static int numberOfThreads = 10;
	public static boolean _DEBUG = false;
	
	public static void main(String args[]) throws Exception {

		// see if we want a different root
		if (args.length >= 2)
			config_file = args[1];
		
        FileReader fileReader = new FileReader(config_file);

        // Always wrap FileReader in BufferedReader.
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line = null;
        while((line = bufferedReader.readLine()) != null) {
//        	System.out.println("again!");
        	line = line.trim();
			String[] list = line.split("\\s+");

			if (list.length != 2) {
				continue;
			}
			if (list[0].equals("DocumentRoot")) {
				WWW_ROOT = list[1];
				if (_DEBUG) System.out.println("WWW_ROOT: " + WWW_ROOT);
			} else if (list[0].equals("ServerName")) {
				serverName = list[1];
				if (_DEBUG) System.out.println("servername: "+serverName);
			} else if (list[0].equals("Listen")) {
				serverPort = Integer.parseInt(list[1]);
				if (_DEBUG) System.out.println("Port: "+serverPort);
			} else if (list[0].equals("CacheSize")) {
				cacheSize = Integer.parseInt(list[1]) * 1024;
				if (_DEBUG) System.out.println("cachesize: "+ cacheSize);
			} else if(list[0].equals("ThreadPoolSize")) {
				numberOfThreads = Integer.parseInt(list[1]);
				if (_DEBUG) System.out.println("numberOfThreads: "+ numberOfThreads);
			}
        }   
        
        // Close files.
        bufferedReader.close();  

        // 1, 4 done  // 2,3,5 stop early
        ThreadPoolWithSuspensionServer test = new ThreadPoolWithSuspensionServer(serverPort, WWW_ROOT, numberOfThreads);
		test.run();
		
	} // end of main
}
