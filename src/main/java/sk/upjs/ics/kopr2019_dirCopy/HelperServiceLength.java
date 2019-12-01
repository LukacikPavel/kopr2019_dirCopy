package sk.upjs.ics.kopr2019_dirCopy;

import java.util.concurrent.atomic.AtomicLong;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class HelperServiceLength extends Service<Boolean> {

	private long totalLength;
	private AtomicLong actualLength;

	public void setActualLength(AtomicLong actualLength) {
		this.actualLength = actualLength;
	}

	public void setTotalLength(long totalLength) {
		this.totalLength = totalLength;
	}

	@Override
	protected Task<Boolean> createTask() {
		return new Task<Boolean>() {

			@Override
			protected Boolean call() throws Exception {
				while (true) {
					updateProgress(actualLength.get(), totalLength);
					updateMessage(actualLength.get() + "/" + totalLength);
				}
			}
		};
	}

}
