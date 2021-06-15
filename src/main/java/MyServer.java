


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MyServer {

    private List<ClientHandler> clients;
    private AuthService authService;

    //Создаем конструктор МайСервера
    public MyServer() {
        //создаем объект сервер класса СерверСокет с портом из ЧатКонстант
        try (ServerSocket server = new ServerSocket(ChatConstants.PORT)) {
            //Запускаем сервис авторизации
            authService = new BaseAuthService();
//            authService = new UsersDB();
            authService.start();
            clients = new ArrayList<>();

            //Ждем подклчения клиента в бесконечном цикле
            while (true) {
                System.out.println("Server is waiting for connection");
                Socket socket = server.accept();
                System.out.println("Client connected");
                new ClientHandler(this, socket);
            }

//Ловим исключение и в любом случае вызываем метод остановки сервиса аутентификации, если он не нул.
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (authService != null) {
                authService.stop();
            }
        }
    }

    public AuthService getAuthService() {
        return authService;
    }

    //Проверяем не занят ли ник через сравнение полученного ника с имеющимся в клиентах.
    public synchronized boolean isNickBusy(String nick) {
        return clients.stream().anyMatch(client -> client.getName().equals(nick));


    }

    //Методы доавбления и удаления клиента в работу с клиентхендлером.
    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    //Метод рассылки сообщения всем.
    public synchronized void broadcastMessage(String message) {
        clients.forEach(client -> client.sendMsg(message));
    }

    public synchronized void broadcastMessageToClients(String message, List<String> nicknames){

        clients.stream()
                .filter(c -> nicknames.contains(c.getName()))
                .forEach(c -> c.sendMsg(message));

    }

    //Метод приватной отправки сообщения
    public synchronized void privateMessage(String name, String message) {
        String[] parts = message.split("\\s+");
        List<String> list = new ArrayList<>(Arrays.asList(parts));
        String address = list.get(2);
        list.remove(1);
        list.remove(1);
        String messageBack = String.join(" ", list);
        clients.stream()
                .filter(clients -> clients.getName().equals(address) || clients.getName().equals(name))
                .forEach(client -> client.sendMsg(messageBack));
    }

}