package gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static server.Server.*;

public class ClientAdmin {
    private static final String SERVER_IP = "localhost";

    JButton b1,b2,b3;
    JComboBox<String> dateJComboBox;
    private ClientAdmin() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
    public static void main(String[] args) {
        ClientAdmin hand = new ClientAdmin();
    }

    private void createAndShowGUI() {
        JFrame f=new JFrame();//creating instance of JFrame

        JTextArea jTextArea= new JTextArea("WELCOME TO TEA STALL");
        jTextArea.setEditable(false);
        jTextArea.setOpaque(false);
        jTextArea.setBounds(120,10,200,25);
        f.add(jTextArea);

        b1 =new JButton("Order");//creating instance of JButton
        b1.setBounds(130,300,100, 40);//x axis, y axis, width, height
        b1.addActionListener(new ButtonListener());
        f.add(b1);//adding button in JFrame


        b2 =new JButton("Order2");//creating instance of JButton
        b2.setBounds(130,400,100, 40);//x axis, y axis, width, height
        b2.addActionListener(new ButtonListener2());
        f.add(b2);//adding button in JFrame

        b3 =new JButton("Order3");//creating instance of JButton
        b3.setBounds(130,500,100, 40);//x axis, y axis, width, height
        b3.addActionListener(new ButtonListener3());
        f.add(b3);//adding button in JFrame

        dateJComboBox = new JComboBox<>();
        SimpleDateFormat  sdf = new SimpleDateFormat(PATTERN);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR,-DELTA_DAYS);
        for(int i = 0; i < DELTA_DAYS; i++) {
            cal.add(Calendar.DAY_OF_YEAR,1);
            dateJComboBox.addItem(sdf.format(cal.getTime()));
        }
        dateJComboBox.setBounds(130,200,100,25);
        f.add(dateJComboBox);


        f.setSize(400,500 + 120);//400 width and 500 height
        f.setLayout(null);//using no layout managers
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);//making the frame visible
    }

    private class ButtonListener3 implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            HardWorker hardWorker = new HardWorker();
            hardWorker.execute();
        }
        private class HardWorker extends SwingWorker<String,Void>  {
            @Override
            protected String doInBackground() {
                try {
                    Socket socket = new Socket(SERVER_IP, PORT);
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    DataInputStream dis = new DataInputStream(socket.getInputStream());

                    dos.writeUTF("ADMIN,"+dateJComboBox.getSelectedItem() +"," +dateJComboBox.getSelectedItem());
                    String ret = dis.readUTF();
                    socket.close();

                    return ret;
                }catch (IOException e) {
                    e.printStackTrace();
                    return  "";
                }
            }
            @Override
            protected void done()  {
                try {
                    String response = get();
                    //edit
                    System.out.println("response3 " + response);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    private class ButtonListener2 implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            HardWorker hardWorker = new HardWorker();
            hardWorker.execute();
        }
        private class HardWorker extends SwingWorker<String,Void>  {
            @Override
            protected String doInBackground() {
                try {
                    Socket socket = new Socket(SERVER_IP, PORT);
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    DataInputStream dis = new DataInputStream(socket.getInputStream());

                    dos.writeUTF("ADMIN,ADMIN");
                    String ret = dis.readUTF();
                    socket.close();

                    return ret;
                }catch (IOException e) {
                    e.printStackTrace();
                    return  "";
                }
            }
            @Override
            protected void done()  {
                try {
                    String response = get();
                    System.out.println("response2 " + response);
                } catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
    }
    private class ButtonListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            HardWorker hardWorker = new HardWorker();
            hardWorker.execute();
        }
        private class HardWorker extends SwingWorker<String,Void>  {
            @Override
            protected String doInBackground() {
                try {
                    Socket socket = new Socket(SERVER_IP, PORT);
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    DataInputStream dis = new DataInputStream(socket.getInputStream());

                    dos.writeUTF("ADMIN");
                    String ret = dis.readUTF();
                    socket.close();

                    return ret;
                }catch (IOException e) {
                    e.printStackTrace();
                    return  "";
                }
            }
            @Override
            protected void done()  {
                try {
                    String response = get();
                    System.out.println("response " +    response);
                } catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
    }
}