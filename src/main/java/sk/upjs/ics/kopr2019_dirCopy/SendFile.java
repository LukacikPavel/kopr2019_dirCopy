package sk.upjs.ics.kopr2019_dirCopy;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class SendFile implements Runnable {

	private BlockingQueue<File> queue;
	private Socket socket;
	private long offset;
	private ConcurrentHashMap<String, Long> map;

	public SendFile(BlockingQueue<File> queue, Socket socket, ConcurrentHashMap<String, Long> map) {
		this.queue = queue;
		this.socket = socket;
		this.map = map;
	}

	@Override
	public void run() {
		try {
			File file = queue.take();
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			while (file != Searcher.POISON_PILL) {
				String filename = file.getPath().substring(Server.DIR_PATH.lastIndexOf('\\') + 1);

				if (map == null || !map.containsKey(filename)) {
					offset = 0;
				} else {
					offset = map.get(filename);
					if (offset == Long.MAX_VALUE) {
						file = queue.take();
						continue;
					}
				}
				System.out.println(filename + " " + offset);

				dos.writeUTF(filename);
				dos.writeLong(file.length());
				dos.flush();

				byte[] data = Files.readAllBytes(file.toPath());
				dos.write(data, (int) offset, (int) (file.length() - offset));
				dos.flush();
				file = queue.take();
			}
			dos.writeUTF(Searcher.POISON_PILL.getName());
			System.out.println("odoslany poison pill");
			socket.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SocketException e) {
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
