package sk.upjs.ics.kopr2019_dirCopy;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ReceiveFile implements Callable<Boolean> {

	private int chunkSize = 1024;
	private Socket socket;
	private ConcurrentHashMap<String, Long> map;
	private long offset;
	private String filename;
	private CountDownLatch counter;
	private AtomicLong actualLength;
	private AtomicInteger actualFileCount;

	public ReceiveFile(Socket socket, ConcurrentHashMap<String, Long> map, CountDownLatch counter,
			AtomicLong actualLength, AtomicInteger actualFileCount) {
		this.socket = socket;
		this.map = map;
		this.counter = counter;
		this.actualLength = actualLength;
		this.actualFileCount = actualFileCount;
	}

	@Override
	public Boolean call() throws java.lang.Exception {
		RandomAccessFile raf;
		try {
			File file = null;
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			while (true) {
				filename = dis.readUTF();

				if (filename.equals(Searcher.POISON_PILL.getName())) {
//					System.out.println(filename);
					break;
				}

				long length = dis.readLong();
				file = new File(ClientService.PATH + filename);

				if (!map.containsKey(filename)) {
					offset = 0;
				} else {
					offset = map.get(filename);
				}
//				System.out.println(filename + " " + offset);

				File parentFile = file.getParentFile();
				parentFile.mkdirs();
				raf = new RandomAccessFile(file, "rw");
				raf.setLength(length);

				byte[] data = new byte[chunkSize];

				raf.seek(offset);

//				int pocetPaketov = (int) Math.ceil((double) (length - offset) / chunkSize);
				int bytesRead = 0;
				while (offset < length) {
//					for (int i = 0; i < pocetPaketov; i++) {
					if (Thread.currentThread().isInterrupted()) {
						break;
					}
					if (length - offset < data.length) {
						bytesRead = dis.read(data, 0, (int) (length - offset));
					} else {
						bytesRead = dis.read(data, 0, data.length);
					}
					raf.seek(offset);
//					bytesRead = dis.read(data);
					raf.write(data, 0, bytesRead);
					offset += bytesRead;
					actualLength.addAndGet(bytesRead);
//					System.out.println(offset);
//					}
				}
				raf.close();
				if (offset < length) {
					saveStateOfFile(filename, offset);
					break;
				} else {
					saveStateOfFile(filename, Long.MAX_VALUE);
				}
				actualFileCount.incrementAndGet();
			}
			counter.countDown();
			socket.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SocketException e) {
			saveStateOfFile(filename, offset);
			counter.countDown();
			throw new Exception();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;

	}

	private void saveStateOfFile(String filename, long offset) {
		map.put(filename, offset);
	}

}
