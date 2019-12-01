package sk.upjs.ics.kopr2019_dirCopy;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Server {

	public static final int PORT_INIT = 6543;
//	public static final int PORT_DOWNLOAD = 6544;
//	public static final String DIR_PATH = "D:\\Downloads\\Se7en ( 1995 )";
	public static final String DIR_PATH = "C:\\Users\\admin\\Desktop\\directory";
//	public static final String DIR_PATH = "D:\\Downloads\\Minecraft 1.13.1 by TeamExtreme";
//	public static final String DIR_PATH = "D:\\Downloads\\obrazok";

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		File dir = new File(DIR_PATH);
		try (ServerSocket server = new ServerSocket(PORT_INIT)) {
			while (true) {
				Socket communicationSocket = server.accept();
				ObjectInputStream ois = new ObjectInputStream(communicationSocket.getInputStream());
				ObjectOutputStream dos = new ObjectOutputStream(communicationSocket.getOutputStream());
				ConcurrentHashMap<String, Long> map = null;

				String action = ois.readUTF();
				System.out.println(action);
				int numberOfSockets = ois.readInt();

				if (action.equals("CONTINUE")) {
					map = (ConcurrentHashMap<String, Long>) ois.readObject();
					System.out.println(map);
					System.out.println(map.size());
				}

				ExecutorService executor = Executors.newFixedThreadPool(numberOfSockets);
				BlockingQueue<File> queue = new LinkedBlockingQueue<File>();
				Searcher searcher = new Searcher(dir, queue, numberOfSockets);
				long[] results = searcher.call();

				dos.writeInt((int) (results[0]));
				dos.writeLong(results[1]);
				dos.flush();

				for (int i = 0; i < numberOfSockets; i++) {
					Socket socket = server.accept();
					SendFile task = new SendFile(queue, socket, map);
					executor.execute(task);
				}

				communicationSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
