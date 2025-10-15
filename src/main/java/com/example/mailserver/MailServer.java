package com.example.mailserver;
import java.net.*;
import java.io.*;
import java.util.*;

public class MailServer {
    private static final int PORT = 9876;
    private static final String ACCOUNTS_DIR = "accounts/";
    private DatagramSocket socket;
    private Map<String, List<String>> userEmails;

    public MailServer() throws SocketException {
        socket = new DatagramSocket(PORT);
        userEmails = new HashMap<>();
        new File(ACCOUNTS_DIR).mkdirs();
        System.out.println("Mail Server đã khởi động trên cổng " + PORT);
    }

    public void start() {
        byte[] receiveData = new byte[1024];

        while (true) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);

                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                System.out.println("Nhận từ client: " + message);
                String response = handleRequest(message);

                byte[] sendData = response.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(
                        sendData, sendData.length, clientAddress, clientPort);
                socket.send(sendPacket);

                receiveData = new byte[1024];

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String handleRequest(String request) {
        String[] parts = request.split("\\|");
        String command = parts[0];

        try {
            switch (command) {
                case "REGISTER":
                    return handleRegister(parts[1]);
                case "SEND_EMAIL":
                    return handleSendEmail(parts[1], parts[2], parts[3]);
                case "LOGIN":
                    return handleLogin(parts[1]);
                case "GET_EMAIL":
                    return handleGetEmail(parts[1], parts[2]);
                default:
                    return "ERROR|Unknown command";
            }
        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }
    }

    private String handleRegister(String username) {
        try {
            File accountDir = new File(ACCOUNTS_DIR + username);

            if (accountDir.exists()) {
                return "ERROR|Account already exists";
            }

            accountDir.mkdirs();
            File newEmailFile = new File(accountDir, "new_email.txt");
            FileWriter writer = new FileWriter(newEmailFile);
            writer.write("Thank you for using this service. we hope that you will feel comfortable........\n");
            writer.close();

            userEmails.put(username, new ArrayList<>());
            System.out.println("Đã tạo account: " + username);
            return "SUCCESS|Account created successfully";

        } catch (IOException e) {
            return "ERROR|Cannot create account";
        }
    }

    private String handleSendEmail(String sender, String recipient, String emailContent) {
        try {
            File accountDir = new File(ACCOUNTS_DIR + recipient);

            if (!accountDir.exists()) {
                return "ERROR|Recipient account does not exist";
            }

            String emailFileName = "email_" + System.currentTimeMillis() + ".txt";
            File emailFile = new File(accountDir, emailFileName);

            FileWriter writer = new FileWriter(emailFile);
            writer.write("From: " + sender + "\n");
            writer.write("Date: " + new Date() + "\n");
            writer.write("-------------------\n");
            writer.write(emailContent);
            writer.close();

            if (!userEmails.containsKey(recipient)) {
                userEmails.put(recipient, new ArrayList<>());
            }
            userEmails.get(recipient).add(emailFileName);

            System.out.println("Đã gửi email từ " + sender + " đến: " + recipient);
            return "SUCCESS|Email sent successfully";

        } catch (IOException e) {
            return "ERROR|Cannot send email";
        }
    }

    private String handleLogin(String username) {
        try {
            File accountDir = new File(ACCOUNTS_DIR + username);

            if (!accountDir.exists()) {
                return "ERROR|Account does not exist";
            }

            File[] files = accountDir.listFiles();
            if (files == null || files.length == 0) {
                return "SUCCESS|No emails";
            }

            StringBuilder fileList = new StringBuilder("SUCCESS|");
            for (File file : files) {
                fileList.append(file.getName()).append(";");
            }

            System.out.println("User " + username + " đã login");
            return fileList.toString();

        } catch (Exception e) {
            return "ERROR|Cannot retrieve emails";
        }
    }

    private String handleGetEmail(String username, String filename) {
        try {
            File emailFile = new File(ACCOUNTS_DIR + username + "/" + filename);

            if (!emailFile.exists()) {
                return "ERROR|Email not found";
            }

            BufferedReader reader = new BufferedReader(new FileReader(emailFile));
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();

            return "SUCCESS|" + content.toString();

        } catch (IOException e) {
            return "ERROR|Cannot read email";
        }
    }

    public static void main(String[] args) {
        try {
            MailServer server = new MailServer();
            server.start();
        } catch (SocketException e) {
            System.err.println("Không thể khởi động server: " + e.getMessage());
        }
    }
}