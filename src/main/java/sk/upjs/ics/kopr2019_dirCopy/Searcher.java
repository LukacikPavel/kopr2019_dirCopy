package sk.upjs.ics.kopr2019_dirCopy;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

public class Searcher implements Callable<long[]> {

	public static final File POISON_PILL = new File("poison.pill");
	private File rootDir;
	private BlockingQueue<File> queue;
	private int numberOfSockets;
	private int fileCount = 0;
	private long totalLength = 0;

	public Searcher(File rootDir, BlockingQueue<File> queue, int numberOfSockets) {
		this.rootDir = rootDir;
		this.queue = queue;
		this.numberOfSockets = numberOfSockets;
	}

	@Override
	public long[] call() throws Exception {
		search(rootDir.listFiles());
		for (int i = 0; i < numberOfSockets; i++) {
			queue.offer(POISON_PILL);
			System.out.println(POISON_PILL.getName());
		}
		System.out.println(fileCount + " " + totalLength);
		long[] results = { fileCount, totalLength };
		return results;
	}

	private void search(File[] dir) {
		for (int i = 0; i < dir.length; i++) {
			if (dir[i].isDirectory()) {
				search(dir[i].listFiles());
			} else {
				queue.offer(dir[i]);
				fileCount++;
				totalLength += dir[i].length();
			}
		}
	}

}
