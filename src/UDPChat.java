import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import javax.swing.text.Element;

public class UDPChat {
    static int port = 6666;
    public InetAddress client;
    public JTextPane output = new JTextPane();
    JTextArea input = new JTextArea();
    public boolean quit = false;
    DatagramSocket socket;
    public String quitMessage = "$quit";
    HTMLDocument doc;
    HTMLEditorKit kit;


    public void createAndShowGUI(DatagramSocket socket){
        Font textFont = new Font("Monospaced", Font.BOLD, 24);
        JFrame mainWindow = new JFrame("Chat");
        mainWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainWindow.setSize(new Dimension(800,800));
        mainWindow.setAlwaysOnTop(true);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;

        output.setFont(textFont);
        output.setContentType("text/html");
        output.setText("<html><body></body></html>");
        output.setEditable(false);
        doc = (HTMLDocument) output.getDocument();
        kit = (HTMLEditorKit) output.getEditorKit();
        StyleSheet styleSheet = doc.getStyleSheet();
        styleSheet.addRule("body { font-size: 24pt; font-family: sans-serif; margin: 5px; }");
        styleSheet.addRule("p { margin-top: 0px; margin-bottom: 15px; }");
        styleSheet.addRule(".origem { color: gray; font-style: italic; font-size: 18pt; }");
        styleSheet.addRule(".recebida { color: green; font-weight: bold; }");
        styleSheet.addRule(".enviada { color: blue; font-weight: bold; }");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.8;
        JScrollPane scrollOutput = new JScrollPane(output);
        panel.add(scrollOutput,gbc);


        input.setFont(textFont);
        input.setForeground(Color.BLUE);
        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER){
                    e.consume();
                    sendMessage();
                }
            }
        });
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.8;
        gbc.weighty = 0.2;
        JScrollPane scrollInput = new JScrollPane(input);
        panel.add(scrollInput, gbc);

        JButton sendButton = new JButton("Enviar");
        sendButton.setFont(textFont);
        sendButton.addActionListener(e -> sendMessage());
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.2;
        gbc.weighty = 0.2;
        panel.add(sendButton, gbc);

        mainWindow.add(panel);

        boolean isFailed = false;

        do {
            String clientAddress = JOptionPane.showInputDialog("Digite o endereco IP do seu contato para comecar a conversar");
            if (clientAddress == null){
                System.exit(0);
            }
            try {
                client = InetAddress.getByName(clientAddress);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(mainWindow, e.getMessage());
                isFailed = true;
            }
        }while (isFailed);
        mainWindow.setVisible(true);
        mainWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeSocket();
                System.exit(0);
            }
        });
        input.setRequestFocusEnabled(true);
    }

    public void closeSocket() {
        System.out.println("Fechando socket");
        try {
            if (socket != null && !socket.isClosed()) {
                DatagramPacket quitPacket = new DatagramPacket(quitMessage.getBytes(),0,
                        quitMessage.getBytes().length,client,port);
                socket.send(quitPacket);
                socket.close();
            }
            System.out.println("Socket fechado");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage() {
        try {
            String mensagem = input.getText().trim();
            if (mensagem.isEmpty()) return;

            DatagramPacket packet = new DatagramPacket(mensagem.getBytes(), mensagem.getBytes().length, client, port);
            socket.send(packet);

            String html = String.format("<p><span class='origem'>Você:</span><br><span class='enviada'>%s</span></p>", mensagem);
            SwingUtilities.invokeLater(() -> {
                try {
                    Element body = doc.getDefaultRootElement().getElement(1);
                    doc.insertBeforeEnd(body, html);
                    output.setCaretPosition(doc.getLength()); // Auto-scroll
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            input.setText("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void receivedMessage(DatagramPacket datagram) {
        try {
            String message = new String(datagram.getData(), 0, datagram.getLength());

            String html = String.format(
                    "<p><span class='origem'>%s diz:</span><br><span class='recebida'>%s</span></p>",
                    datagram.getAddress().getHostAddress(),
                    message
            );

            SwingUtilities.invokeLater(() -> {
                try {
                    Element body = doc.getDefaultRootElement().getElement(1);
                    doc.insertBeforeEnd(body, html);
                    output.setCaretPosition(doc.getLength()); // Auto-scroll
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

     public void start() {
        createAndShowGUI(socket);
        try{
            socket = new DatagramSocket(port);
            System.out.println("Porta: " + port);
            System.out.println("Iniciando comunicacao com " + client + ":" + port);
            do {
                DatagramPacket datagram = new DatagramPacket(new byte[1024], 1024);
                socket.receive(datagram);

                receivedMessage(datagram);
                String message = new String(datagram.getData()).trim();
                if (message.equalsIgnoreCase(quitMessage)) {
                    System.out.println("Fechando socket");
                    quit = true;
                }

                System.out.println(message);
            } while (!quit);
            socket.close();
            System.out.println("Socket fechado");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
