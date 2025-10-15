package com.example.mailserver;// MailClientGUI.java
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.net.*;
import java.io.IOException;

public class MailClientGUI extends Application {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 9876;
    private DatagramSocket socket;
    private InetAddress serverIP;
    private String currentUser = null;
    private String clientIP = "";

    private Stage primaryStage;
    private Scene loginScene;
    private Scene mainScene;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("VKU Mail Client");

        try {
            socket = new DatagramSocket();
            serverIP = InetAddress.getByName(SERVER_ADDRESS);

            // Lấy IP của client
            try (DatagramSocket tempSocket = new DatagramSocket()) {
                tempSocket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                clientIP = tempSocket.getLocalAddress().getHostAddress();
            }

        } catch (Exception e) {
            showAlert("Error", "Không thể kết nối đến server!", Alert.AlertType.ERROR);
            return;
        }

        createLoginScene();
        primaryStage.setScene(loginScene);
        primaryStage.setWidth(500);
        primaryStage.setHeight(450);
        primaryStage.show();
    }

    private void createLoginScene() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #667eea 0%, #764ba2 100%);");

        // Logo/Title
        Label titleLabel = new Label("VKU MAIL");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.WHITE);

        Label subtitleLabel = new Label("UDP Socket Mail Service");
        subtitleLabel.setFont(Font.font("Arial", 14));
        subtitleLabel.setTextFill(Color.WHITE);

        Label ipLabel = new Label("Your IP: " + clientIP);
        ipLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        ipLabel.setTextFill(Color.LIGHTGREEN);

        // Login form container
        VBox formBox = new VBox(15);
        formBox.setPadding(new Insets(30));
        formBox.setAlignment(Pos.CENTER);
        formBox.setMaxWidth(350);
        formBox.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        Label usernameLabel = new Label("Username:");
        usernameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Nhập username");
        usernameField.setStyle("-fx-font-size: 14px; -fx-padding: 10;");

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button loginButton = new Button("Đăng nhập");
        loginButton.setPrefWidth(150);
        loginButton.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10;");

        Button registerButton = new Button("Đăng ký");
        registerButton.setPrefWidth(150);
        registerButton.setStyle("-fx-background-color: #764ba2; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10;");

        buttonBox.getChildren().addAll(loginButton, registerButton);

        formBox.getChildren().addAll(usernameLabel, usernameField, buttonBox);

        root.getChildren().addAll(titleLabel, subtitleLabel, ipLabel, formBox);

        // Event handlers
        loginButton.setOnAction(e -> handleLogin(usernameField.getText()));
        registerButton.setOnAction(e -> handleRegister(usernameField.getText()));

        usernameField.setOnAction(e -> handleLogin(usernameField.getText()));

        loginScene = new Scene(root);
    }

    private void createMainScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f5f5f5;");

        // Top bar
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(15));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #667eea;");

        Label userLabel = new Label("👤 " + currentUser);
        userLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        userLabel.setTextFill(Color.WHITE);

        Label ipLabel = new Label("🌐 IP: " + clientIP);
        ipLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        ipLabel.setTextFill(Color.LIGHTGREEN);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutButton = new Button("Đăng xuất");
        logoutButton.setStyle("-fx-background-color: #764ba2; -fx-text-fill: white; " +
                "-fx-font-weight: bold;");
        logoutButton.setOnAction(e -> handleLogout());

        topBar.getChildren().addAll(userLabel, ipLabel, spacer, logoutButton);

        // Left panel - Email list
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(15));
        leftPanel.setPrefWidth(350);
        leftPanel.setStyle("-fx-background-color: white;");

        Label inboxLabel = new Label("📥 Hộp thư đến");
        inboxLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        ListView<EmailItem> emailListView = new ListView<>();
        emailListView.setPrefHeight(400);
        emailListView.setCellFactory(lv -> new EmailCell());

        Button refreshButton = new Button("🔄 Làm mới");
        refreshButton.setPrefWidth(320);
        refreshButton.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshButton.setOnAction(e -> loadEmails(emailListView));

        leftPanel.getChildren().addAll(inboxLabel, emailListView, refreshButton);

        // Right panel - Email content/compose
        VBox rightPanel = new VBox(15);
        rightPanel.setPadding(new Insets(15));
        rightPanel.setStyle("-fx-background-color: white;");

        TabPane tabPane = new TabPane();

        // Tab 1: Compose email
        Tab composeTab = new Tab("✉️ Soạn thư");
        composeTab.setClosable(false);

        VBox composeBox = new VBox(10);
        composeBox.setPadding(new Insets(15));

        Label toLabel = new Label("Người nhận:");
        toLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        TextField toField = new TextField();
        toField.setPromptText("Nhập username người nhận");

        Label subjectLabel = new Label("Tiêu đề:");
        subjectLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        TextField subjectField = new TextField();
        subjectField.setPromptText("Nhập tiêu đề email");

        Label contentLabel = new Label("Nội dung:");
        contentLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        TextArea contentArea = new TextArea();
        contentArea.setPrefRowCount(12);
        contentArea.setPromptText("Nhập nội dung email...");

        Button sendButton = new Button("📤 Gửi email");
        sendButton.setPrefWidth(150);
        sendButton.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10;");
        sendButton.setOnAction(e -> handleSendEmail(toField.getText(), subjectField.getText(),
                contentArea.getText(), toField, subjectField,
                contentArea, emailListView));

        composeBox.getChildren().addAll(toLabel, toField, subjectLabel, subjectField,
                contentLabel, contentArea, sendButton);
        composeTab.setContent(composeBox);

        // Tab 2: Read email
        Tab readTab = new Tab("📖 Đọc thư");
        readTab.setClosable(false);

        VBox readBox = new VBox(10);
        readBox.setPadding(new Insets(15));

        Label readTitleLabel = new Label("Chi tiết email:");
        readTitleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        TextArea readArea = new TextArea();
        readArea.setPrefRowCount(20);
        readArea.setEditable(false);
        readArea.setWrapText(true);
        readArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px;");

        readBox.getChildren().addAll(readTitleLabel, readArea);
        readTab.setContent(readBox);

        tabPane.getTabs().addAll(composeTab, readTab);
        rightPanel.getChildren().add(tabPane);

        // Email list selection handler
        emailListView.setOnMouseClicked(e -> {
            EmailItem selectedEmail = emailListView.getSelectionModel().getSelectedItem();
            if (selectedEmail != null) {
                loadEmailContent(selectedEmail.getFilename(), readArea);
                tabPane.getSelectionModel().select(readTab);
            }
        });

        // Layout
        root.setTop(topBar);
        root.setLeft(leftPanel);
        root.setCenter(rightPanel);

        mainScene = new Scene(root, 1000, 650);

        // Load emails initially
        loadEmails(emailListView);
    }

    // Email Item class
    private static class EmailItem {
        private String filename;
        private String subject;

        public EmailItem(String filename, String subject) {
            this.filename = filename;
            this.subject = subject;
        }

        public String getFilename() { return filename; }
        public String getSubject() { return subject; }
    }

    // Custom Cell for ListView
    private static class EmailCell extends ListCell<EmailItem> {
        @Override
        protected void updateItem(EmailItem item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                VBox vbox = new VBox(5);
                vbox.setPadding(new Insets(5));

                Label subjectLabel = new Label("📧 " + item.getSubject());
                subjectLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));

                Label filenameLabel = new Label(item.getFilename());
                filenameLabel.setFont(Font.font("Arial", 10));
                filenameLabel.setTextFill(Color.GRAY);

                vbox.getChildren().addAll(subjectLabel, filenameLabel);
                setGraphic(vbox);
            }
        }
    }

    private String sendRequest(String request) {
        try {
            byte[] sendData = request.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                    sendData, sendData.length, serverIP, SERVER_PORT);
            socket.send(sendPacket);

            byte[] receiveData = new byte[4096];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            return new String(receivePacket.getData(), 0, receivePacket.getLength());
        } catch (IOException e) {
            return "ERROR|Connection failed";
        }
    }

    private void handleRegister(String username) {
        if (username.trim().isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập username!", Alert.AlertType.ERROR);
            return;
        }

        String request = "REGISTER|" + username;
        String response = sendRequest(request);
        String[] parts = response.split("\\|");

        if (parts[0].equals("SUCCESS")) {
            showAlert("Thành công", "Đăng ký tài khoản thành công!", Alert.AlertType.INFORMATION);
        } else {
            showAlert("Lỗi", parts[1], Alert.AlertType.ERROR);
        }
    }

    private void handleLogin(String username) {
        if (username.trim().isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập username!", Alert.AlertType.ERROR);
            return;
        }

        String request = "LOGIN|" + username;
        String response = sendRequest(request);
        String[] parts = response.split("\\|");

        if (parts[0].equals("SUCCESS")) {
            currentUser = username;
            createMainScene();
            primaryStage.setScene(mainScene);
            primaryStage.setWidth(1000);
            primaryStage.setHeight(650);
        } else {
            showAlert("Lỗi", parts[1], Alert.AlertType.ERROR);
        }
    }

    private void handleLogout() {
        currentUser = null;
        primaryStage.setScene(loginScene);
        primaryStage.setWidth(500);
        primaryStage.setHeight(450);
    }

    private void handleSendEmail(String recipient, String subject, String content,
                                 TextField toField, TextField subjectField, TextArea contentArea,
                                 ListView<EmailItem> emailListView) {
        if (recipient.trim().isEmpty() || subject.trim().isEmpty() || content.trim().isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ thông tin!", Alert.AlertType.ERROR);
            return;
        }

        String request = "SEND_EMAIL|" + currentUser + "|" + recipient + "|" + subject + "|" + content;
        String response = sendRequest(request);
        String[] parts = response.split("\\|");

        if (parts[0].equals("SUCCESS")) {
            showAlert("Thành công", "Gửi email thành công!", Alert.AlertType.INFORMATION);
            toField.clear();
            subjectField.clear();
            contentArea.clear();
            loadEmails(emailListView);
        } else {
            showAlert("Lỗi", parts[1], Alert.AlertType.ERROR);
        }
    }

    private void loadEmails(ListView<EmailItem> emailListView) {
        String request = "LOGIN|" + currentUser;
        String response = sendRequest(request);
        String[] parts = response.split("\\|");

        emailListView.getItems().clear();

        if (parts[0].equals("SUCCESS") && parts.length > 1 && !parts[1].equals("No emails")) {
            String[] emails = parts[1].split(";");
            for (String email : emails) {
                if (!email.trim().isEmpty()) {
                    String[] emailParts = email.split(":::");
                    if (emailParts.length == 2) {
                        emailListView.getItems().add(new EmailItem(emailParts[0], emailParts[1]));
                    }
                }
            }
        }
    }

    private void loadEmailContent(String filename, TextArea readArea) {
        String request = "GET_EMAIL|" + currentUser + "|" + filename;
        String response = sendRequest(request);
        String[] parts = response.split("\\|", 2);

        if (parts[0].equals("SUCCESS") && parts.length > 1) {
            readArea.setText(parts[1]);
        } else {
            readArea.setText("Không thể đọc email!");
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    @Override
    public void stop() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}