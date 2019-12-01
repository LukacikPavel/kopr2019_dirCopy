package sk.upjs.ics.kopr2019_dirCopy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

public class ClientController {

	@FXML
	private ResourceBundle resources;

	@FXML
	private URL location;

	@FXML
	private ProgressBar progressBarLength;

	@FXML
	private ProgressBar progressBarFiles;

	@FXML
	private Button buttonStart;

	@FXML
	private Button buttonPause;

	@FXML
	private Text textLength;

	@FXML
	private Text textCount;

	@FXML
	private TextField textFieldConnections;

	private ClientService service;
	private HelperServiceFiles helperServiceFiles;
	private HelperServiceLength helperServiceLength;
	private CountDownLatch counter;

	@FXML
	void buttonPauseClicked(ActionEvent event) {
		service.cancel();
		try {
			counter.await();
			service.getMap().put("set.Length", service.getActualLength().get());
			service.getMap().put("set.Files", (long) service.getActualFileCount().get());
			saveState(service.getMap());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@FXML
	void buttonStartClicked(ActionEvent event) {
		int numberOfConnections = Integer.parseInt(textFieldConnections.getText());
		counter = new CountDownLatch(numberOfConnections);

		helperServiceFiles = new HelperServiceFiles();
		helperServiceLength = new HelperServiceLength();

		service = new ClientService(helperServiceLength, helperServiceFiles, counter, readSavedState());

		progressBarLength.progressProperty().bind(helperServiceLength.progressProperty());
		progressBarFiles.progressProperty().bind(helperServiceFiles.progressProperty());

		textLength.textProperty().bind(helperServiceLength.messageProperty());
		textCount.textProperty().bind(helperServiceFiles.messageProperty());
		service.setNumerOfSockets(numberOfConnections);

		service.start();
	}

	public static void saveState(ConcurrentHashMap<String, Long> map) {
		try {
			FileOutputStream fileOut = new FileOutputStream("map.ser");
			ObjectOutputStream oos = new ObjectOutputStream(fileOut);
			oos.writeObject(map);
			oos.close();
			fileOut.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@SuppressWarnings("unchecked")
	private ConcurrentHashMap<String, Long> readSavedState() {
		ConcurrentHashMap<String, Long> map = null;
		File file = new File("map.ser");
		if (file.exists()) {
			try {
				FileInputStream fileIn = new FileInputStream("map.ser");
				ObjectInputStream ois = new ObjectInputStream(fileIn);
				map = (ConcurrentHashMap<String, Long>) ois.readObject();
				ois.close();
				fileIn.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		file.delete();
		return map;
	}

	@FXML
	void initialize() {
	}
}
