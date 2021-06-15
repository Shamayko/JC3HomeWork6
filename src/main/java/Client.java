

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Client extends JFrame {
    //Объявляем переменные для графики
    private JTextArea chatArea;
    private JTextField inputField;

    //Создаем потоки
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Socket socket;

    //Создаем потоки записи в файл и чтения, объект класса файл.
    private DataOutputStream fileOutputStream;
    private DataInputStream fileInputStream;
    private String fileName;
    private File file;

    //Открываем соединение, инициируем Гуи
    public Client() {
        try {
            openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        initGUI();
    }

    //Запускаем графику
    public void initGUI() {
        setBounds(100, 100, 500, 500);
        setTitle("DAMNCHAT");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
//Добавляем панельки с вводом текста, кнопочкой отправки сообщения и т.п.
        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        JPanel panel = new JPanel(new BorderLayout());
        JButton sendButton = new JButton("Send");
        panel.add(sendButton, BorderLayout.EAST);
        inputField = new JTextField();
        panel.add(inputField, BorderLayout.CENTER);
        add(panel, BorderLayout.SOUTH);
//Добавляем слушателей на кнопочки отправки сообщения
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
//Создаем слушателя на закрытие окошка
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    outputStream.writeUTF(ChatConstants.STOP_WORD);
                    closeConnection();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

        });
//Делаем все видимым
        setVisible(true);
    }

    //Открываем соединение, сокеты, порты
    public void openConnection() throws IOException {
        socket = new Socket(ChatConstants.HOST, ChatConstants.PORT);
        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());


        //Создаем новый поток-нить выполнения программы
        new Thread(() -> {
            //Создаем бесконечный цикл, который ждет пока не пройдет авторизация
            try {
                while (true) {
                    String strFromServer = inputStream.readUTF();
                    chatArea.append(strFromServer);
                    chatArea.append("\n");
                    if (strFromServer.startsWith(ChatConstants.AUTH_OK)) {
                        break;
                    }

                }
//Творим историю и показываем ее...
                while (true) {
                    String strFromServer = inputStream.readUTF();
                    chatArea.append(strFromServer + "\n");
                    if (strFromServer.contains(" entered the chat")) {
                        String[] messages = strFromServer.split("\\s+");
                        fileName = "history_" + messages[0] + ".txt";
                        file = new File(getFileName());
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        chatArea.append(sendHistory());

                        break;
                    }
                }


                fileOutputStream = new DataOutputStream(new FileOutputStream(getFileName(), true));
                saveHistory(LocalDateTime.now().toString());

//Создаем бесконечный цикл, который читает строку до тех пор пока не будет стоп слова.
                while (true) {
                    String strFromServer = inputStream.readUTF();
                    saveHistory(strFromServer);
                    if (strFromServer.equals(ChatConstants.STOP_WORD)) {
                        break;
                    } else {
                        chatArea.append(strFromServer);
                        chatArea.append("\n");
                    }
                }
                fileOutputStream.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
                Runtime.getRuntime().exit(0);
            }
        }).start();
    }

    //Пишем метод отправки сообщений, ловим исключения
    private void sendMessage() {
        if (!inputField.getText().trim().isEmpty()) {
            try {
                outputStream.writeUTF(inputField.getText());
                inputField.setText("");
                inputField.grabFocus();
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Send error occured");
            }
        }
    }

    public void closeConnection() {

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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


    public void saveHistory(String text) throws IOException {
        fileOutputStream.writeUTF(text + "\n");
    }

    public String sendHistory() {
        List<String> list = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        try {
            fileInputStream = new DataInputStream(new FileInputStream(getFileName()));
            String line;
            while (fileInputStream.available() > 0) {
                line = fileInputStream.readUTF();
                list.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

            if (list.size() >= 100) {
            for (int i = list.size() - 100; i < list.size(); i++) {
                text.append(list.get(i));
            }
        } else {
            for (String s : list) {
                text.append(s);
            }
        }
        return text.toString();
    }

    public String getFileName() {
        return fileName;
    }

    public static void main(String[] args) {
        //открыть новый поток, запуская клиента
        SwingUtilities.invokeLater(Client::new);
    }
}
