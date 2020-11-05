import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.atomic.AtomicInteger;

public class FrmColorPickerJava {
    private JTabbedPane tabbedPane1;
    private JPanel panel1;
    private JTextField txtPortServer;
    private JTextField txtIPServer;
    private JButton btnListen;
    private JTextField txtRed;
    private JTextField txtGreen;
    private JTextField txtBlue;
    private JTextField txtPortClient;
    private JTextField txtIPClient;
    private JTextField txtClientRed;
    private JTextField txtClientGreen;
    private JTextField txtClientBlue;
    private JButton customColorButton;
    private JButton btnKirim;
    private JPanel colorClient;
    private JPanel colorServer;
    private String codeColor = "0,0,0";

    public static void main(String[] args) {
        JFrame frame = new JFrame("Frm Color Picker");
        frame.setContentPane(new FrmColorPickerJava().panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private Color colorPicker() {
        Color current = Color.BLACK;
        if (colorClient.getBackground() != null) {
            current = colorClient.getBackground();
        }
        Color resultColor = JColorChooser.showDialog(null, "Choose Color", current);
        if (resultColor == null) {
            resultColor = current;
        }
        return resultColor;
    }

    public FrmColorPickerJava() {
        btnListen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    new Thread( () -> {
                        try {
                            EchoServer(txtIPServer.getText(), Integer.parseInt(txtPortServer.getText()));
                        } catch (IOException ioException){
                            ioException.printStackTrace();
                        }
                    }).start();
                } catch (Exception ex){
                    JOptionPane.showMessageDialog(null, ex.getMessage());
                }
            }
        });

        customColorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Color selectedColor = colorPicker();
                colorClient.setBackground(selectedColor);

                txtClientRed.setText(String.valueOf(selectedColor.getRed()));
                txtClientGreen.setText(String.valueOf(selectedColor.getGreen()));
                txtClientBlue.setText(String.valueOf(selectedColor.getBlue()));

                codeColor = txtClientRed.getText() + "," +
                            txtClientGreen.getText() + "," +
                            txtClientBlue.getText() + ",";

                JOptionPane.showMessageDialog(null, "Set Color Success");
            }
        });
        btnKirim.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    AtomicInteger messageWritten = new AtomicInteger(0);
                    AtomicInteger messageRead = new AtomicInteger(0);

                    EchoClient(txtIPClient.getText(), Integer.parseInt(txtPortClient.getText()), codeColor, messageWritten, messageRead);
                } catch (Exception ex){
                    JOptionPane.showMessageDialog(null, ex.getMessage());
                }
            }
        });
    }


    public void EchoClient  (String host, int port, final String message, final AtomicInteger messageWritten, final AtomicInteger messageRead) throws IOException {
        //create a socket channel
        AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open();

        //try to connect to the server side
        socketChannel.connect(new InetSocketAddress(host, port), socketChannel, new CompletionHandler<Void, AsynchronousSocketChannel>() {
            @Override
            public void completed(Void result, AsynchronousSocketChannel attachment) {
                //start to read message
                startRead(attachment, messageRead);

                //write a message to server side
                startWrite(attachment, message, messageWritten);
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
                System.out.println("Fail to connect to server");
            }
        });
    }

    private void startRead(final AsynchronousSocketChannel socketChannel, final AtomicInteger messageRead) {
        final ByteBuffer buffer = ByteBuffer.allocate(2048);

        socketChannel.read(buffer, socketChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
            @Override
            public void completed(Integer result, AsynchronousSocketChannel attachment) {
                //message is read from server
                messageRead.getAndIncrement();

                //print the message
                System.out.println("Read Message: " + new String(buffer.array()));
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
                System.out.println("Fail to read message from server");
            }
        });
    }

    private void startWrite(final AsynchronousSocketChannel socketChannel, final String message, final AtomicInteger messageWritten){
        ByteBuffer buffer = ByteBuffer.allocate(2048);
        buffer.put(message.getBytes());
        buffer.flip();
        messageWritten.getAndIncrement();
        socketChannel.write(buffer, socketChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
            @Override
            public void completed(Integer result, AsynchronousSocketChannel attachment) {
                //After message written
                //NOTHING TO DO
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
                System.out.println("Fail to write the message to server");
            }
        });
    }
    ///////////////////
    // Socket Server //
    ///////////////////
    public void EchoServer(String bindAddr, int bindPort) throws IOException {
        InetSocketAddress socketAddress = new InetSocketAddress(bindAddr, bindPort);

        //create a socket channel and bind to local bind address
        AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open().bind(socketAddress);

        //start to accept the connection from client
        serverSocketChannel.accept(serverSocketChannel, new CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>() {
            @Override
            public void completed(AsynchronousSocketChannel result, AsynchronousServerSocketChannel attachment) {
                //a connection is accepted, start to accept next connection
                attachment.accept(attachment, this);
                //start to read message from the client
                startRead(result);
            }

            @Override
            public void failed(Throwable exc, AsynchronousServerSocketChannel attachment) {
                System.out.println("Fail to accept a connection");
            }
        });
    }

    private void startRead(AsynchronousSocketChannel socketChannel) {
        final ByteBuffer buffer = ByteBuffer.allocate(2048);

        //read message from client
        socketChannel.read(buffer, socketChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
            //Some message is read from client, this callback will be called
            @Override
            public void completed(Integer result, AsynchronousSocketChannel attachment) {
                buffer.flip();

                //echo the message
                startWrite(attachment, buffer);

                //sttart to read next Message again
                startRead(attachment);

                String bufArray = new String(buffer.array());
                String[] receiveColorCode = bufArray.split(",");
                txtRed.setText(receiveColorCode[0]);
                txtGreen.setText(receiveColorCode[1]);
                txtBlue.setText(receiveColorCode[2]);

                int red = Integer.parseInt(txtRed.getText());
                int green = Integer.parseInt(txtGreen.getText());
                int blue = Integer.parseInt(txtBlue.getText());

                Color color = new Color(red, green, blue);
                colorServer.setBackground(color);
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
                System.out.println("Fail to read message from client");
            }
        });
    }

    private void startWrite(AsynchronousSocketChannel socketChannel, final ByteBuffer buffer){
        socketChannel.write(buffer, socketChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
            @Override
            public void completed(Integer result, AsynchronousSocketChannel attachment) {
                //finish to write message to client, nothing to do
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
                //fail to write message to client
                System.out.println("Fail to write message to client");
            }
        });
    }
}
