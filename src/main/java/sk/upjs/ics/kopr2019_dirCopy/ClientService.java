package sk.upjs.ics.kopr2019_dirCopy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public class ClientService extends Service<Long> {

	public static final String PATH = "D:\\Downloads\\lenTak\\";
	public int numerOfSockets;
	private CountDownLatch counter;
	private ExecutorService executor;
	private ObjectOutputStream oos;
	private ObjectInputStream dis;
	private int fileCount;
	private long totalLength;
	private AtomicInteger actualFileCount;
	private AtomicLong actualLength;
	private HelperServiceFiles helperServiceFiles;
	private HelperServiceLength helperServiceLength;
	private ConcurrentHashMap<String, Long> map;

	public ClientService(HelperServiceLength helperServiceLength, HelperServiceFiles helperServiceFiles,
			CountDownLatch counter, ConcurrentHashMap<String, Long> map) {
		this.helperServiceLength = helperServiceLength;
		this.helperServiceFiles = helperServiceFiles;
		this.counter = counter;
		this.map = map;

		setOnCancelled(new EventHandler<WorkerStateEvent>() {

			@Override
			public void handle(WorkerStateEvent event) {
				executor.shutdownNow();
			}
		});
	}

	public AtomicInteger getActualFileCount() {
		return actualFileCount;
	}

	public AtomicLong getActualLength() {
		return actualLength;
	}

	public ConcurrentHashMap<String, Long> getMap() {
		return map;
	}

	public void setNumerOfSockets(int numerOfSockets) {
		this.numerOfSockets = numerOfSockets;
	}

	@Override
	protected Task<Long> createTask() {
		return new Task<Long>() {

			@Override
			protected Long call() throws Exception {
				try (Socket clientSocket = new Socket("localhost", Server.PORT_INIT)) {
					oos = new ObjectOutputStream(clientSocket.getOutputStream());
					dis = new ObjectInputStream(clientSocket.getInputStream());
					executor = Executors.newFixedThreadPool(numerOfSockets);
					actualFileCount = new AtomicInteger();
					actualLength = new AtomicLong();

					try {
						if (map == null) {
							oos.writeUTF("START");
							oos.writeInt(numerOfSockets);
							map = new ConcurrentHashMap<String, Long>();
						} else {
							oos.writeUTF("CONTINUE");
							oos.writeInt(numerOfSockets);
							oos.writeObject(map);

							actualLength.set(map.get("set.Length"));

							actualFileCount.set(map.get("set.Files").intValue());

						}
						oos.flush();

						fileCount = dis.readInt();
						totalLength = dis.readLong();

						List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
						for (int i = 0; i < numerOfSockets; i++) {
							Socket socket = new Socket("localhost", Server.PORT_INIT);
							ReceiveFile task = new ReceiveFile(socket, map, counter, actualLength, actualFileCount);
							Future<Boolean> future = executor.submit(task);
							futures.add(future);
						}

						helperServiceFiles.setActualFileCount(actualFileCount);
						helperServiceFiles.setFileCount(fileCount);
						helperServiceFiles.start();

						helperServiceLength.setActualLength(actualLength);
						helperServiceLength.setTotalLength(totalLength);
						helperServiceLength.start();

						System.out.println(futures.get(0).get());
						System.out.println("Stahovanie je dokoncene");
						executor.shutdown();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
//						e.printStackTrace();
					} catch (ExecutionException e) {
						counter.await();
						map.put("set.Length", actualLength.get());
						map.put("set.Files", (long) actualFileCount.get());
						System.out.println(map);
						ClientController.saveState(map);
						System.out.println("EE");
						cancel();
					}

				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return actualLength.get();
			}
		};
	}

}
