package sk.upjs.ics.kopr2019_dirCopy;

import java.util.concurrent.atomic.AtomicInteger;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class HelperServiceFiles extends Service<Boolean> {

	private int fileCount;
	private AtomicInteger actualFileCount;

	public void setFileCount(int fileCount) {
		this.fileCount = fileCount;
	}

	public void setActualFileCount(AtomicInteger actualFileCount) {
		this.actualFileCount = actualFileCount;
	}

	@Override
	protected Task<Boolean> createTask() {
		return new Task<Boolean>() {

			@Override
			protected Boolean call() throws Exception {
				while (true) {
					updateProgress(actualFileCount.get(), fileCount);
					updateMessage(actualFileCount.get() + "/" + fileCount);
				}
			}
		};
	}

}
