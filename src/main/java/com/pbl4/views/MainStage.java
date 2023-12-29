package com.pbl4.views;

import com.pbl4.models.CpuInfo;
import com.pbl4.models.HostProcess;
import com.pbl4.utils.CoresManager;
import com.pbl4.utils.ProcessesUtil;
import com.sun.management.OperatingSystemMXBean;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.Duration;

/**
 *
 * @author tugino & huanmd
 */
public class MainStage implements Runnable, EventHandler<WindowEvent>, ChangeListener<String> {

	private static final String TAG = "MainStage";

	private static final int LOOP_DELAY = 1000;
	private static final int DELAY_PROCESSES = 2000;
	private static final int MAX_DATA_POINTS = 100;

	private int delayCount = 0;

	private TabPane center;
	private Scene stageScene;

	private BorderPane layoutPane;
	private MenuBar menuBar;

	private double prefWidth;
	private double prefHeight;

	private boolean isFullscreen = false;

	public ObservableList<HostProcess> userProcessesList = FXCollections.observableArrayList();
	public ObservableList<HostProcess> allProcessesList = FXCollections.observableArrayList();

	private HashMap<String, HostProcess> monitoredProcesses = new HashMap<>();

	private CoresManager coresManager;
	private LineChart<Number, Number> cpuChart;
	private LineChart<Number, Number> memoryChart;
	private NumberAxis xCPUAxis;
	private NumberAxis xMemoryAxis;
	private XYChart.Series memorySeries;

	private Stage myStage;
	private CheckBox checkBoxAllProcesses;
	private TextField inputFilter;
	private TableView<HostProcess> viewProcesses;
	private ContextMenu contextMenu;

	private Tab detailTab;

	private String filterString;

	private int currentGraphPosition = 0;

	private ScheduledExecutorService updateProcessesService;

	/*
	 * Handle event rchoose an option on contextMenu and tab "Process"
	 */

	private final EventHandler<ActionEvent> actionEventHandler = (ActionEvent actionEvent) -> {
		String clickedId;
		if (actionEvent.getSource() instanceof CheckBox) {
			updateProcesses();
			return;
		} else if (actionEvent.getSource() instanceof MenuItem) {
			clickedId = ((MenuItem) actionEvent.getSource()).getId();
		} else {
			clickedId = ((Button) actionEvent.getSource()).getId();
		}

		HostProcess hostProcess = viewProcesses.getSelectionModel().getSelectedItem();

		switch (clickedId) {
			case "exit":
				updateProcessesService.shutdown();
				if (myStage != null) {
					myStage.close();
				}
				break;
			case "killp":
				ProcessesUtil.killProcess(viewProcesses.getSelectionModel().getSelectedItem());
				updateProcesses();
				break;
			case "pausep":
				ProcessesUtil.pauseProcess(viewProcesses.getSelectionModel().getSelectedItem());
				updateProcesses();
				break;
			case "continuep":
				ProcessesUtil.continueProcess(viewProcesses.getSelectionModel().getSelectedItem());
				updateProcesses();
				break;
			case "monitorep":
				if (!hostProcess.isMonitored()) {
					hostProcess.setMonitored(true);
					hostProcess.setTimeStartFollow(System.currentTimeMillis());
					monitoredProcesses.put(hostProcess.getKey(), hostProcess);
					viewProcesses.refresh();
					refreshDetailTab();
				}
				break;
			case "smonitorep":
				if (hostProcess.isMonitored()) {
					List<Long> memoryLogs = hostProcess.getMemoryLogs();
					monitoredProcesses.remove(hostProcess.getKey());
					hostProcess.setMonitored(false);

					saveLogs(hostProcess.getPID() + "-memory", memoryLogs);

					viewProcesses.refresh();
				}
				break;
			case "fs":
				isFullscreen = !isFullscreen;
				myStage.setFullScreen(isFullscreen);
				updateDimensions();
				break;
		}
	};

	/*
	 * Handle event right-click on HostProcess in Tab "Process"
	 */

	private final EventHandler<MouseEvent> mouseEventHandler = (MouseEvent mouseEvent) -> {
		if (mouseEvent.isSecondaryButtonDown()) {
			contextMenu.show(center, mouseEvent.getScreenX(), mouseEvent.getScreenY());
		} else if (mouseEvent.isPrimaryButtonDown()) {
			// TODO: show command in a view
			System.out.println("Command: " + viewProcesses.getSelectionModel().getSelectedItem().getCommand());
		}
	};

	public MainStage(Stage stage) {
		this.myStage = stage;
	}

	public void init() {
		Group root = new Group();
		stageScene = new Scene(root);
		myStage.setScene(stageScene);

		myStage.setResizable(true);
		myStage.setWidth(650);
		myStage.setHeight(520);

		myStage.widthProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneWidth,
					Number newSceneWidth) {
				updateDimensions();
			}
		});

		myStage.heightProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneHeight,
					Number newSceneHeight) {
				updateDimensions();
			}
		});

		layoutPane = new BorderPane();

		root.getChildren().add(layoutPane);

		initTableContextMenu();
		initCenter();
		initTop();

		updateDimensions();

		myStage.setOnCloseRequest(this);

		updateProcessesService = Executors.newSingleThreadScheduledExecutor();// Executors.newScheduledThreadPool(1);
		updateProcessesService.scheduleWithFixedDelay(this, 0, LOOP_DELAY, TimeUnit.MILLISECONDS);
	}

	private void initTop() {
		TilePane topPane = new TilePane(Orientation.HORIZONTAL);

		menuBar = new MenuBar();

		// --- Menu File
		Menu menuFile = new Menu("File");
		MenuItem add = new MenuItem("Exit");
		add.setId("exit");
		add.setOnAction(actionEventHandler);
		menuFile.getItems().addAll(add);

		// --- Menu Edit
		Menu menuEdit = new Menu("Edit");

		// --- Menu View
		Menu menuView = new Menu("View");
		add = new MenuItem("Fullscreen");
		add.setId("fs");
		add.setOnAction(actionEventHandler);
		menuView.getItems().addAll(add);

		menuBar.getMenus().addAll(menuFile, menuEdit, menuView);

		topPane.getChildren().add(menuBar);

		layoutPane.setTop(topPane);
	}

	private void initCenter() {
		center = new TabPane();

		HBox.setHgrow(center, Priority.ALWAYS);

		addTab("Processes", initTabProcesses());
		addTab("Resources", initTabResources());
		{
			detailTab = new Tab("Detail", initTabDetails());
			detailTab.setClosable(false);
			center.getTabs().add(detailTab);
		}

		HBox centerContainer = new HBox(center);
		centerContainer.setPadding(new Insets(0, 10, 10, 10));
		centerContainer
				.setBackground(new Background(new BackgroundFill(Color.gray(0.865), CornerRadii.EMPTY, Insets.EMPTY)));

		layoutPane.setCenter(centerContainer);
	}

	private void refreshDetailTab() {
		VBox detailContainer = (VBox) initTabDetails();
		detailTab.setContent(detailContainer);
	}

	private void addTab(String tabTitle, Node tabContent) {
		Tab tab = new Tab();
		tab.setText(tabTitle);
		tab.setContent(tabContent);
		tab.setClosable(false);

		center.getTabs().add(tab);
	}

	public void show() {
		init();
		myStage.show();
	}

	private void updateDimensions() {
		if (isFullscreen) {
			Rectangle2D screenBounds = Screen.getPrimary().getBounds();

			prefWidth = screenBounds.getWidth();
			prefHeight = screenBounds.getHeight();
		} else {
			prefWidth = myStage.getWidth();
			prefHeight = myStage.getHeight() - menuBar.getHeight();
		}

		menuBar.setPrefWidth(prefWidth);
		layoutPane.setPrefSize(prefWidth, prefHeight);
	}

	private Node initTabResources() {
		HBox coresContainer = new HBox();

		coresContainer.setPrefWidth(prefWidth);

		coresManager = new CoresManager();

		int coresCount = Runtime.getRuntime().availableProcessors();// ManagementFactory.getThreadMXBean().getThreadCount();

		Label coreUsage;
		double coreWidth = prefWidth / coresCount;

		xCPUAxis = new NumberAxis(0, 100, 10);

		NumberAxis yAxis = new NumberAxis(0, 100, 25);

		cpuChart = new LineChart<>(xCPUAxis, yAxis);
		cpuChart.setAnimated(false);
		cpuChart.setTitle("CPU usage");

		XYChart.Series series;
		for (int i = 0; i < coresCount; i++) {
			coreUsage = new Label();
			series = new XYChart.Series();
			series.setName("cpu" + i);

			cpuChart.getData().add(series);

			coreUsage.setPrefWidth(coreWidth);
			coreUsage.setAlignment(Pos.CENTER);

			coresContainer.getChildren().add(coreUsage);

			coresManager.addCpuInfo(new CpuInfo("cpu" + i, coreUsage, series));
		}

		HBox memoryContainer = new HBox();

		xMemoryAxis = new NumberAxis(0, 100, 10);

		memoryChart = new LineChart<>(xMemoryAxis, new NumberAxis(0, 100, 25));
		memoryChart.setTitle("Memory usage");
		memoryChart.setAnimated(false);

		memorySeries = new XYChart.Series();
		memorySeries.setName("Memory usage");
		memoryChart.getData().add(memorySeries);

		VBox vBox = new VBox(cpuChart, coresContainer, memoryChart, memoryContainer);

		return vBox;
	}

	private Node initTabProcesses() {
		userProcessesList = FXCollections.observableArrayList();
		allProcessesList = FXCollections.observableArrayList();

		ProcessesUtil.getProcesses(allProcessesList, monitoredProcesses, true);
		ProcessesUtil.getProcesses(userProcessesList, monitoredProcesses, false);

		TableColumn<HostProcess, Boolean> monitoredCol = new TableColumn<>(" ");
		TableColumn pidCol = new TableColumn("PID");
		TableColumn<HostProcess, String> nameCol = new TableColumn<>("Name");
		TableColumn userCol = new TableColumn("User");
		TableColumn cpuCol = new TableColumn("CPU (%)");
		TableColumn memCol = new TableColumn("Memory (%)");

		monitoredCol.setCellValueFactory(new PropertyValueFactory<>("monitored"));
		monitoredCol.setCellFactory(column -> new CheckBoxTableCell());

		pidCol.setCellValueFactory(new PropertyValueFactory<>("PID"));

		nameCol.setCellValueFactory(new PropertyValueFactory<>("readableName"));

		userCol.setCellValueFactory(new PropertyValueFactory<>("user"));

		cpuCol.setCellValueFactory(new PropertyValueFactory<>("CPU"));

		memCol.setCellValueFactory(new PropertyValueFactory<>("MEM"));

		viewProcesses = new TableView<>();
		viewProcesses.setEditable(false);
		viewProcesses.getColumns().addAll(monitoredCol, pidCol, nameCol, userCol, cpuCol, memCol);
		viewProcesses.setItems(userProcessesList);

		viewProcesses.setOnMousePressed(mouseEventHandler);

		checkBoxAllProcesses = new CheckBox("All user processes");
		checkBoxAllProcesses.setOnAction(actionEventHandler);

		inputFilter = new TextField();
		inputFilter.textProperty().addListener(this);

		Label labelFilter = new Label("Filter:");
		labelFilter.setPadding(new Insets(0, 10, 0, 0));

		HBox hBoxFilter = new HBox(labelFilter, inputFilter);
		hBoxFilter.setAlignment(Pos.BASELINE_RIGHT);
		HBox.setHgrow(hBoxFilter, Priority.ALWAYS);

		HBox hBoxOptions = new HBox(checkBoxAllProcesses, hBoxFilter);
		hBoxOptions.setPrefWidth(prefWidth);
		hBoxOptions.setPadding(new Insets(10));

		VBox.setVgrow(viewProcesses, Priority.ALWAYS);

		return new VBox(viewProcesses, hBoxOptions);
	}

	private Node initTabDetails() {
		// Tạo ListView để hiển thị danh sách các tiến trình
		ObservableList<HostProcess> observableProcessList = FXCollections.observableArrayList(monitoredProcesses.values());
		ListView<HostProcess> processListView = new ListView<>();
		processListView.setItems(observableProcessList);

		ListView<String> processListNameView = new ListView<>();
		processListNameView.setItems(FXCollections.observableArrayList(monitoredProcesses.keySet()));
		VBox detailsContainer = new VBox();

		if (observableProcessList.isEmpty()) { // Sửa ở đây
			Label emptyLabel = new Label("No processes available");
			detailsContainer.getChildren().add(emptyLabel);
		} else {

			// Tạo một StackPane để hiển thị thông tin chi tiết của tiến trình được chọn
			StackPane detailsPane = new StackPane();

			// Đặt một listener cho sự kiện selection của ListView
			processListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
				if (newValue != null) {
					HostProcess selectedProcess = newValue; // newValue là một đối tượng HostProcess
					String processName = selectedProcess.getReadableName(); // Lấy tên từ đối tượng HostProcess
					Label processLabel = new Label(processName);

					LineChart<Number, Number> cpuChart = new LineChart<>(new NumberAxis(), new NumberAxis(0, 100, 10));
					LineChart<Number, Number> memChart = new LineChart<>(new NumberAxis(), new NumberAxis(0, 100, 10));

					XYChart.Series cpuSeries = new XYChart.Series();
					cpuSeries.setName("CPU Usage");
					cpuChart.getData().add(cpuSeries);

					XYChart.Series memSeries = new XYChart.Series();
					memSeries.setName("Memory Usage");
					memChart.getData().add(memSeries);

					HBox processBox = new HBox(processLabel, cpuChart, memChart);
					processBox.setSpacing(10);

					Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
						updateCharts(cpuChart, memChart, selectedProcess);
					}));
					timeline.setCycleCount(Timeline.INDEFINITE);
					timeline.play();

					detailsPane.getChildren().setAll(processBox);
				}
			});

			SplitPane splitPane = new SplitPane();

			splitPane.getItems().addAll(processListView, detailsPane);
			splitPane.setDividerPositions(0.2); // Đặt vị trí chia màn hình (có thể điều chỉnh)

			detailsContainer.getChildren().add(splitPane);
		}

		return detailsContainer;
	}

	private void updateCharts(LineChart<Number, Number> cpuChart, LineChart<Number, Number> memChart,
			HostProcess selectedProcess) {
		try {
			double cpuUsage = Double.parseDouble(selectedProcess.getCPU());
			double memoryUsage = Double.parseDouble(selectedProcess.getMEM());
			long currentTime = System.currentTimeMillis();

			// Cập nhật dữ liệu đồ thị CPU
			XYChart.Series cpuSeries = cpuChart.getData().get(0);
			cpuSeries.getData()
					.add(new XYChart.Data<>((currentTime - selectedProcess.getTimeStartFollow()) / 1000, cpuUsage));
			if (cpuSeries.getData().size() > 60) { // giữ chỉ 60 điểm dữ liệu gần nhất, ví dụ 1 phút dữ liệu
				cpuSeries.getData().remove(0);
			}

			// Cập nhật dữ liệu đồ thị Memory
			XYChart.Series memSeries = memChart.getData().get(0);
			memSeries.getData()
					.add(new XYChart.Data<>((currentTime - selectedProcess.getTimeStartFollow()) / 1000, memoryUsage));
			if (memSeries.getData().size() > 60) { // giữ chỉ 60 điểm dữ liệu gần nhất
				memSeries.getData().remove(0);
			}

			System.out.println(cpuUsage + " " + memoryUsage);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			// Handle the exception accordingly, e.g., logging or showing an error message
			// to the user
		}
	}

	private MenuItem createMenuItem(String title, String id) {
		MenuItem menuItem = new MenuItem(title);
		menuItem.setId(id);

		menuItem.setOnAction(actionEventHandler);

		return menuItem;
	}

	private void initTableContextMenu() {
		TextField textField = new TextField("Type Something"); // we will add a popup menu to this text field
		contextMenu = new ContextMenu();

		MenuItem pause = createMenuItem("Stop", "pausep");
		MenuItem _continue = createMenuItem("Continue", "continuep");
		MenuItem kill = createMenuItem("Kill", "killp");
		MenuItem monitore = createMenuItem("Start monitoring", "monitorep");
		MenuItem stopMonitoring = createMenuItem("Stop monitoring", "smonitorep");

		contextMenu.getItems().addAll(pause, _continue, kill, new SeparatorMenuItem(), monitore, stopMonitoring);
		// ...
		textField.setContextMenu(contextMenu);
	}

	private void updateProcesses() {
		int current = viewProcesses.getSelectionModel().getSelectedIndex();

		if (checkBoxAllProcesses.isSelected()) {
			allProcessesList = FXCollections.observableArrayList();
			ProcessesUtil.getProcesses(allProcessesList, monitoredProcesses, true, filterString);

			viewProcesses.setItems(allProcessesList);
		} else {
			userProcessesList = FXCollections.observableArrayList();
			ProcessesUtil.getProcesses(userProcessesList, monitoredProcesses, false, filterString);

			viewProcesses.setItems(userProcessesList);
		}

		viewProcesses.sort();
		viewProcesses.getSelectionModel().select(current);
		viewProcesses.refresh();
	}

	private void backgroudFinishCallback() {
		HashMap<String, CpuInfo> cpus = coresManager.getCPUsInfo();

		CpuInfo cpuInfo;
		XYChart.Data data;

		for (Entry<String, CpuInfo> entry : cpus.entrySet()) {
			Rectangle rect = new Rectangle(0, 0);
			rect.setVisible(false);

			cpuInfo = entry.getValue();
			data = new XYChart.Data(currentGraphPosition, cpuInfo.getUsage());
			data.setNode(rect);

			if (currentGraphPosition > MAX_DATA_POINTS) {
				cpuInfo.getSeries().getData().remove(0);
			}

			cpuInfo.getSeries().getData().add(data);
			cpuInfo.getLabel().setText(cpuInfo.getName() + ": " + Math.round(cpuInfo.getUsage()) + "%");
		}

		OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

		double usedPercent = 100 * ((double) (1.0
				- ((double) os.getFreePhysicalMemorySize() / (double) os.getTotalPhysicalMemorySize())));

		usedPercent = Math.round(usedPercent > 99.9 ? 100 : usedPercent);

		Rectangle memRect = new Rectangle(0, 0);
		memRect.setVisible(false);

		XYChart.Data memoryData = new XYChart.Data(currentGraphPosition, usedPercent);
		memoryData.setNode(memRect);

		memoryChart.setTitle("Memory usage (" + usedPercent + "%)");

		if (currentGraphPosition > MAX_DATA_POINTS) {
			memorySeries.getData().remove(0);
		}

		memorySeries.getData().add(memoryData);

		if (currentGraphPosition > MAX_DATA_POINTS) {
			xCPUAxis.setLowerBound(currentGraphPosition - MAX_DATA_POINTS);
			xCPUAxis.setUpperBound(currentGraphPosition);

			xMemoryAxis.setLowerBound(currentGraphPosition - MAX_DATA_POINTS);
			xMemoryAxis.setUpperBound(currentGraphPosition);
		}

		currentGraphPosition++;
	}

	@Override
	public void run() {
		if (delayCount == DELAY_PROCESSES) {
			delayCount = 0;
			updateProcesses();

			HashSet<String> toRemove = new HashSet<>();

			// monitored processes
			for (HostProcess hp : monitoredProcesses.values()) {
				if (allProcessesList.contains(hp)) {
					ProcessesUtil.addMemoryLog(hp);
				} else {
					toRemove.add(hp.getKey());
				}
			}

			// remove stopped processes
			for (String hpKey : toRemove) {
				monitoredProcesses.remove(hpKey);
			}
		}
		coresManager.getCPUProc();

		delayCount += LOOP_DELAY;

		Platform.runLater(this::backgroudFinishCallback);
	}

	@Override
	public void handle(WindowEvent t) {
		updateProcessesService.shutdown();
	}

	@Override
	public void changed(ObservableValue<? extends String> ov, String oldValue, String newValue) {
		filterString = newValue;

		updateProcesses();
	}

	private void saveLogs(String fileName, List<Long> logs) {
		FileChooser fileChooser = new FileChooser();

		// Set extension filter
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
		fileChooser.getExtensionFilters().add(extFilter);
		fileChooser.setInitialFileName(fileName + ".txt");

		// Show save file dialog
		File file = fileChooser.showSaveDialog(myStage);

		if (file != null) {
			saveFile(file, logs);
		}
	}

	private void saveFile(File file, List<Long> logs) {
		try (FileWriter fileWriter = new FileWriter(file)) {
			String content = "";

			for (int i = 0; i < logs.size(); i++) {
				content += String.valueOf(logs.get(i)) + "\n";
			}

			fileWriter.write(content);
			fileWriter.close();
		} catch (IOException ex) {
			Logger.getLogger(MainStage.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

}