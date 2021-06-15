

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ClientHandler {

    private MyServer server;
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private String login;
    private String name;
    private volatile boolean authorized;

    public String getName() {
        return name;
    }

    public ClientHandler(MyServer server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.inputStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            this.name = "";
            this.authorized = false;


            //Реализуем ExecutorService
            ExecutorService service = Executors.newFixedThreadPool(10);
            service.submit(() -> {
                try {
                    authentication();
                    readMessages();
                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            });
            service.shutdown();

        } catch (IOException ex) {
            System.out.println("Problem with client creating");
        }

    }

    private void readMessages() throws IOException {
        while (true) {
            String messageFromClient = inputStream.readUTF();
            System.out.println("from " + name + ": " + messageFromClient);

            if(authorized) {
                if (messageFromClient.equals(ChatConstants.STOP_WORD)) {
                    return;

//                    Меняем ник
                } else if (messageFromClient.startsWith(ChatConstants.CHANGE_NICKNAME)) {

                    String newNickname = messageFromClient.split("\\s+")[1].trim();
                    if (server.getAuthService().changeNickname(login, newNickname)) {

                        String exName = name;
                        name = newNickname;

                        server.broadcastMessage(String.format("[Server]: nickname '%s' has been changed to '%s'",
                                exName, newNickname));
                        sendMsg(String.format("%s %s", name));
                    } else {
                        server.broadcastMessageToClients("[Server]: nickname has not been changed due to error",
                                Arrays.asList(name));
                    }

                }

                if (messageFromClient.startsWith(ChatConstants.PRIVATE_MESSAGE)) {
                    server.privateMessage(name, "[" + name + "]: " + messageFromClient);
                } else {
                    server.broadcastMessage("[" + name + "]: " + messageFromClient);
                }
            }

        }
    }

    // /auth login pass
    private void authentication() throws IOException, SQLException {
        while (true) {
            String message = inputStream.readUTF();
            if (message.startsWith(ChatConstants.AUTH)) {
                String[] parts = message.split("\\s+");
                String nick = server.getAuthService().getNickByLoginAndPass(parts[1], parts[2]);
                if (nick != null) {
                    //проверим, что такого пока нет
                    if (!server.isNickBusy(nick)) {
                        authorized = true;
                        sendMsg(ChatConstants.AUTH_OK + " " + nick);
                        name = nick;
                        login = parts[1];
                        server.subscribe(this);
                        server.broadcastMessage(name + " entered the chat");
                        return;
                    } else {
                        sendMsg("Nick is already in use");
                    }
                } else {
                    sendMsg("Incorrect login/pass");
                }
            } else if(message.startsWith(ChatConstants.REG)) {

                String[] parts = message.split("\\s+");
                if (server.getAuthService().createUser(parts[1], parts[2], parts[3])) {
                    sendMsg(String.format("[Server]: user %s has been successfully created", parts[3]));
                } else {
                    sendMsg(String.format("[Server]: user %s has not been created due to error", parts[3]));
                }
            }


        }

    }

    public void sendMsg(String message) {
        try {
            outputStream.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        server.unsubscribe(this);
        server.broadcastMessage(name + " left the chat");
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}