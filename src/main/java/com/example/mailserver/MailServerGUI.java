package com.example.mailserver;

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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.net.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class MailServerGUI extends Application {
    private static final int PORT = 9876;
    private static final String ACCOUNTS_DIR = "accounts/";
    private static final String USERS_FILE = "users.txt";
    private DatagramSocket socket;
    private Map<String, String> userCredentials; // username -> password
    private Map<String, List<String>> userEmails;
    private boolean serverRunning = false;
    private Thread serverThread;

    // GUI Components
    private TextArea logArea;
    private Label statusLabel;
    private Label portLabel;
    private Label ipLabel;
    private Label accountsLabel;
    private Label emailsLabel;
    private Button startButton;
    private Button stopButton;
    private Button clearButton;
    private ListView<String> accountListView;
    private ObservableList<String> accountList;
    private int totalEmails = 0;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("VKU Mail Server");

        userCredentials = new HashMap<>();
        userEmails = new HashMap<>();
        accountList = FXCollections.observableArrayList();

        // Load users from file
        loadUsers();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2c3e50;");

        VBox topPanel = createTopPanel();
        HBox centerPanel = createCenterPanel();
        HBox bottomPanel = createBottomPanel();

        root.setTop(topPanel);
        root.setCenter(centerPanel);
        root.setBottom(bottomPanel);

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            if (serverRunning) {
                stopServer();
            }
        });

        loadExistingAccounts();
    }

    private VBox createTopPanel() {
        VBox topPanel = new VBox(10);
        topPanel.setPadding(new Insets(20));
        topPanel.setStyle("-fx-background-color: linear-gradient(to right, #667eea 0%, #764ba2 100%);");

        Label titleLabel = new Label("VKU MAIL SERVER");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        titleLabel.setTextFill(Color.WHITE);

        Label subtitleLabel = new Label("UDP Socket Mail Server Manager");
        subtitleLabel.setFont(Font.font("Arial", 14));
        subtitleLabel.setTextFill(Color.LIGHTGRAY);

        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(10, 0, 0, 0));
        statusBar.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("⚫ Server Stopped");
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        statusLabel.setTextFill(Color.LIGHTCORAL);

        portLabel = new Label("📡 Port: " + PORT);
        portLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        portLabel.setTextFill(Color.WHITE);

        ipLabel = new Label("🌐 IP: Getting...");
        ipLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        ipLabel.setTextFill(Color.WHITE);

        // Get server IP
        new Thread(() -> {
            try {
                String serverIP = InetAddress.getLocalHost().getHostAddress();
                Platform.runLater(() -> ipLabel.setText("🌐 IP: " + serverIP));
            } catch (Exception e) {
                Platform.runLater(() -> ipLabel.setText("🌐 IP: Unknown"));
            }
        }).start();

        accountsLabel = new Label("👥 Accounts: 0");
        accountsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        accountsLabel.setTextFill(Color.WHITE);

        emailsLabel = new Label("✉️ Emails: 0");
        emailsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        emailsLabel.setTextFill(Color.WHITE);

        statusBar.getChildren().addAll(statusLabel, portLabel, ipLabel, accountsLabel, emailsLabel);
        topPanel.getChildren().addAll(titleLabel, subtitleLabel, statusBar);

        return topPanel;
    }

    private HBox createCenterPanel() {
        HBox centerPanel = new HBox(15);
        centerPanel.setPadding(new Insets(15));

        VBox leftPanel = new VBox(10);
        leftPanel.setPrefWidth(250);
        leftPanel.setPadding(new Insets(15));
        leftPanel.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        Label accountsTitle = new Label("📋 Danh sách Accounts");
        accountsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        accountListView = new ListView<>(accountList);
        accountListView.setPrefHeight(500);
        accountListView.setStyle("-fx-font-size: 13px;");

        Button refreshAccountsBtn = new Button("🔄 Làm mới");
        refreshAccountsBtn.setPrefWidth(220);
        refreshAccountsBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshAccountsBtn.setOnAction(e -> loadExistingAccounts());

        leftPanel.getChildren().addAll(accountsTitle, accountListView, refreshAccountsBtn);

        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(15));
        rightPanel.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        Label logTitle = new Label("📊 Server Log");
        logTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; " +
                "-fx-control-inner-background: #1e1e1e; -fx-text-fill: #00ff00;");
        logArea.setPrefHeight(500);

        rightPanel.getChildren().addAll(logTitle, logArea);
        centerPanel.getChildren().addAll(leftPanel, rightPanel);

        return centerPanel;
    }

    private HBox createBottomPanel() {
        HBox bottomPanel = new HBox(15);
        bottomPanel.setPadding(new Insets(15));
        bottomPanel.setAlignment(Pos.CENTER);
        bottomPanel.setStyle("-fx-background-color: #34495e;");

        startButton = new Button("▶️ Start Server");
        startButton.setPrefWidth(150);
        startButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10;");
        startButton.setOnAction(e -> startServer());

        stopButton = new Button("⏹️ Stop Server");
        stopButton.setPrefWidth(150);
        stopButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10;");
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> stopServer());

        clearButton = new Button("🗑️ Clear Log");
        clearButton.setPrefWidth(150);
        clearButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10;");
        clearButton.setOnAction(e -> logArea.clear());

        bottomPanel.getChildren().addAll(startButton, stopButton, clearButton);

        return bottomPanel;
    }

    private void startServer() {
        try {
            socket = new DatagramSocket(PORT);
            serverRunning = true;

            Platform.runLater(() -> {
                statusLabel.setText("🟢 Server Running");
                statusLabel.setTextFill(Color.LIGHTGREEN);
                startButton.setDisable(true);
                stopButton.setDisable(false);
            });

            log("✅ Server started on port " + PORT);

            serverThread = new Thread(() -> runServer());
            serverThread.setDaemon(true);
            serverThread.start();

        } catch (SocketException e) {
            log("❌ ERROR: Cannot start server - " + e.getMessage());
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Server Error");
                alert.setHeaderText("Cannot start server");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            });
        }
    }

    private void stopServer() {
        serverRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        Platform.runLater(() -> {
            statusLabel.setText("⚫ Server Stopped");
            statusLabel.setTextFill(Color.LIGHTCORAL);
            startButton.setDisable(false);
            stopButton.setDisable(true);
        });

        log("⏹️ Server stopped");
    }

    private void runServer() {
        byte[] receiveData = new byte[2048];

        while (serverRunning) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);

                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                String clientIP = clientAddress.getHostAddress();

                log("📨 Received from " + clientIP + ":" + clientPort + " - " + message.split("\\|")[0]);

                String response = handleRequest(message, clientIP);

                byte[] sendData = response.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(
                        sendData, sendData.length, clientAddress, clientPort);
                socket.send(sendPacket);

                log("📤 Sent response to " + clientIP);

                receiveData = new byte[2048];

            } catch (IOException e) {
                if (serverRunning) {
                    log("❌ ERROR: " + e.getMessage());
                }
            }
        }
    }

    private String handleRequest(String request, String clientIP) {
        String[] parts = request.split("\\|");
        String command = parts[0];

        try {
            switch (command) {
                case "REGISTER":
                    return handleRegister(parts[1], parts[2]);
                case "LOGIN":
                    return handleLogin(parts[1], parts[2]);
                case "SEND_EMAIL":
                    return handleSendEmail(parts[1], parts[2], parts[3], parts[4], clientIP);
                case "GET_EMAILS":
                    return handleGetEmails(parts[1]);
                case "GET_EMAIL":
                    return handleGetEmail(parts[1], parts[2]);
                default:
                    return "ERROR|Unknown command";
            }
        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }
    }

    private void loadUsers() {
        try {
            File usersFile = new File(USERS_FILE);
            if (usersFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(usersFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        userCredentials.put(parts[0], parts[1]);
                    }
                }
                reader.close();
            }
        } catch (IOException e) {
            log("⚠️ Cannot load users file");
        }
    }

    private void saveUser(String username, String password) {
        try {
            FileWriter writer = new FileWriter(USERS_FILE, true);
            writer.write(username + ":" + password + "\n");
            writer.close();
        } catch (IOException e) {
            log("❌ Cannot save user");
        }
    }

    private String handleRegister(String username, String password) {
        try {
            if (userCredentials.containsKey(username)) {
                log("⚠️ Registration failed: Account '" + username + "' already exists");
                return "ERROR|Account already exists";
            }

            File accountDir = new File(ACCOUNTS_DIR + username);
            accountDir.mkdirs();

            File newEmailFile = new File(accountDir, "welcome.txt");
            FileWriter writer = new FileWriter(newEmailFile);
            writer.write("From: System\n");
            writer.write("To: " + username + "\n");
            writer.write("Subject: Welcome to VKU Mail!\n");
            writer.write("Date: " + new Date() + "\n");
            writer.write("IP: Server\n");
            writer.write("-----------------------------------\n");
            writer.write("Thank you for using this service. We hope that you will feel comfortable........\n");
            writer.close();

            userCredentials.put(username, password);
            saveUser(username, password);
            userEmails.put(username, new ArrayList<>());

            log("✅ Account created: " + username);

            Platform.runLater(() -> loadExistingAccounts());

            return "SUCCESS|Account created successfully";

        } catch (IOException e) {
            log("❌ ERROR creating account: " + e.getMessage());
            return "ERROR|Cannot create account";
        }
    }

    private String handleLogin(String username, String password) {
        if (!userCredentials.containsKey(username)) {
            log("⚠️ Login failed: Account '" + username + "' does not exist");
            return "ERROR|Account does not exist";
        }

        if (!userCredentials.get(username).equals(password)) {
            log("⚠️ Login failed: Wrong password for '" + username + "'");
            return "ERROR|Wrong password";
        }

        log("✅ User '" + username + "' logged in successfully");
        return "SUCCESS|Login successful";
    }

    private String handleSendEmail(String sender, String recipient, String subject, String emailContent, String clientIP) {
        try {
            if (!userCredentials.containsKey(recipient)) {
                log("⚠️ Send failed: Recipient '" + recipient + "' does not exist");
                return "ERROR|Recipient account does not exist";
            }

            File accountDir = new File(ACCOUNTS_DIR + recipient);

            String emailFileName = "email_" + System.currentTimeMillis() + ".txt";
            File emailFile = new File(accountDir, emailFileName);

            FileWriter writer = new FileWriter(emailFile);
            writer.write("From: " + sender + "\n");
            writer.write("To: " + recipient + "\n");
            writer.write("Subject: " + subject + "\n");
            writer.write("Date: " + new Date() + "\n");
            writer.write("IP: " + clientIP + "\n");
            writer.write("-----------------------------------\n");
            writer.write(emailContent);
            writer.close();

            if (!userEmails.containsKey(recipient)) {
                userEmails.put(recipient, new ArrayList<>());
            }
            userEmails.get(recipient).add(emailFileName);

            totalEmails++;
            Platform.runLater(() -> emailsLabel.setText("✉️ Emails: " + totalEmails));

            log("📧 Email sent: From=" + sender + " [" + clientIP + "], To=" + recipient + ", Subject=" + subject);

            return "SUCCESS|Email sent successfully";

        } catch (IOException e) {
            log("❌ ERROR sending email: " + e.getMessage());
            return "ERROR|Cannot send email";
        }
    }

    private String handleGetEmails(String username) {
        try {
            File accountDir = new File(ACCOUNTS_DIR + username);

            if (!accountDir.exists()) {
                return "SUCCESS|No emails";
            }

            File[] files = accountDir.listFiles();
            if (files == null || files.length == 0) {
                return "SUCCESS|No emails";
            }

            StringBuilder fileList = new StringBuilder("SUCCESS|");
            for (File file : files) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    String line;
                    String subject = "No Subject";

                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("Subject: ")) {
                            subject = line.substring(9);
                            break;
                        }
                    }
                    reader.close();

                    fileList.append(file.getName()).append(":::").append(subject).append(";");
                } catch (IOException e) {
                    fileList.append(file.getName()).append(":::No Subject;");
                }
            }

            log("📥 User '" + username + "' retrieved " + files.length + " emails");
            return fileList.toString();

        } catch (Exception e) {
            log("❌ ERROR retrieving emails: " + e.getMessage());
            return "ERROR|Cannot retrieve emails";
        }
    }

    private String handleGetEmail(String username, String filename) {
        try {
            File emailFile = new File(ACCOUNTS_DIR + username + "/" + filename);

            if (!emailFile.exists()) {
                log("⚠️ Email not found: " + filename);
                return "ERROR|Email not found";
            }

            BufferedReader reader = new BufferedReader(new FileReader(emailFile));
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();

            log("📖 Email read: " + filename + " by " + username);

            return "SUCCESS|" + content.toString();

        } catch (IOException e) {
            log("❌ ERROR reading email: " + e.getMessage());
            return "ERROR|Cannot read email";
        }
    }

    private void loadExistingAccounts() {
        File accountsDir = new File(ACCOUNTS_DIR);
        accountsDir.mkdirs();

        File[] accounts = accountsDir.listFiles(File::isDirectory);

        accountList.clear();
        totalEmails = 0;

        if (accounts != null) {
            for (File account : accounts) {
                accountList.add(account.getName());

                File[] emails = account.listFiles();
                if (emails != null) {
                    totalEmails += emails.length;
                }
            }
        }

        Platform.runLater(() -> {
            accountsLabel.setText("👥 Accounts: " + accountList.size());
            emailsLabel.setText("✉️ Emails: " + totalEmails);
        });

        log("📂 Loaded " + accountList.size() + " accounts, " + totalEmails + " total emails");
    }

    private void log(String message) {
        Platform.runLater(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String timestamp = sdf.format(new Date());
            logArea.appendText("[" + timestamp + "] " + message + "\n");
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}