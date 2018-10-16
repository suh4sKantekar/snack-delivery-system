package gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static server.Server.*;

public class ClientUser {
    private static final String SERVER_DOWN = "Cant connect now";
    private static final String INVALID_DETAILS = "Invalid name (only a-z,A-Z) / 0 total qty";
    private static final String INVOICE = "INVOICE";
    private static final String SERVER_IP = "localhost";

    JButton bOK;
    JTextField name;
    JComboBox<Integer>[] qty;

    JTextArea invoiceHeading, custDetails, totalPrice, etaBox;
    JTextArea[] pricing;

    private static final Integer MAX_QUANTITY = 10;

    private ClientUser() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
    public static void main(String[] args) {
        ClientUser hand = new ClientUser();
    }

    private void createAndShowGUI() {
        JFrame f=new JFrame();//creating instance of JFrame

        JTextArea jTextArea= new JTextArea("WELCOME TO TEA STALL");
        jTextArea.setEditable(false);
        jTextArea.setOpaque(false);
        jTextArea.setBounds(120,10,200,25);
        f.add(jTextArea);

        jTextArea = new JTextArea("Your name: ");
        jTextArea.setOpaque(false);
        jTextArea.setEditable(false);
        jTextArea.setBounds(20,44,100,25);
        f.add(jTextArea);

        name = new JTextField();
        name.setBounds(100,40,150,25);
        f.add(name);

        qty = new JComboBox[TOTAL_ITEMS];
        for(int i = 0; i < TOTAL_ITEMS; i++) {
            jTextArea = new JTextArea(ITEM_NAMES.get(i) + "( Rs: " + ITEM_COSTS.get(i) + " ): ");
            jTextArea.setOpaque(false);
            jTextArea.setEditable(false);
            jTextArea.setBounds(20,74 + 30*i ,130,25);
            f.add(jTextArea);

            qty[i] = new JComboBox<>();
            for(int j = 0; j <= MAX_QUANTITY; j++) {
                qty[i].addItem(j);
            }
            qty[i].setBounds(170,70 + 30*i,100,25);
            f.add(qty[i]);
        }
        int yCur = 74 + 30*TOTAL_ITEMS;
        bOK =new JButton("Order");//creating instance of JButton
        bOK.setBounds(130,yCur,100, 40);//x axis, y axis, width, height
        bOK.addActionListener(new ButtonListener());
        f.add(bOK);//adding button in JFrame

        invoiceHeading = new JTextArea("");
        invoiceHeading.setOpaque(false);
        invoiceHeading.setEditable(false);
        invoiceHeading.setBounds(130,yCur + 60,100,25);
        f.add(invoiceHeading);


        custDetails = new JTextArea("");
        custDetails.setOpaque(false);
        custDetails.setEditable(false);
        custDetails.setBounds(100,yCur + 90,100,25);
        f.add(custDetails);
        yCur += 120;

        pricing = new JTextArea[TOTAL_ITEMS];
        for(int i = 0; i < TOTAL_ITEMS; i++) {
            pricing[i] = new JTextArea("");
            pricing[i].setOpaque(false);
            pricing[i].setEditable(false);
            pricing[i].setBounds(100,yCur + 30*i,300,25);
            f.add(pricing[i]);
        }
        yCur += 30*TOTAL_ITEMS;

        totalPrice = new JTextArea("");
        totalPrice.setOpaque(false);
        totalPrice.setEditable(false);
        totalPrice.setBounds(130,yCur,100,25);
        f.add(totalPrice);


        etaBox = new JTextArea("");
        etaBox.setOpaque(false);
        etaBox.setEditable(false);
        etaBox.setBounds(100,yCur + 30,350,25);
        f.add(etaBox);

        f.setSize(400,yCur + 120);//400 width and 500 height
        f.setLayout(null);//using no layout managers
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);//making the frame visible
    }


    /**private class ButtonListener2 implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Thread t= new Thread(new Runnable() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("EDIT " + SwingUtilities.isEventDispatchThread());
                        }
                    });
                }
            });
            t.start();
            try {
                t.join();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }**/
    private class ButtonListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            HardWorker hardWorker = new HardWorker();
            hardWorker.execute();
        }
        private class HardWorker extends SwingWorker<String,Void>  {
            @Override
            protected String doInBackground() {
                try {
                    String custName = name.getText();
                    int countZero = 0;
                    for(int i = 0; i < TOTAL_ITEMS; i++) {
                        Integer val = (Integer)qty[i].getSelectedItem();
                        if(val.equals(0)) countZero++;
                    }
                    if(!custName.matches("[a-zA-Z]+") || countZero == TOTAL_ITEMS) {
                        return INVALID_DETAILS;
                    }

                    Socket socket = new Socket(SERVER_IP, PORT);
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    DataInputStream dis = new DataInputStream(socket.getInputStream());

                    String order = "";
                    for(int i = 0; i < TOTAL_ITEMS; i++) {
                        order += qty[i].getSelectedItem();
                        order += ",";
                    }
                    order += custName;

                    dos.writeUTF(order);
                    String ret = dis.readUTF();
                    socket.close();

                    return ret;
                }catch (IOException e) {
                    e.printStackTrace();
                    return SERVER_DOWN;
                }
            }
            @Override
            protected void done()  {
                try {
                    String response = get();
                    System.out.println(response);

                    if(response.equals(SERVER_DOWN) ||
                            response.equals(INVALID) ||
                            response.equals(OUT_OF_STOCK) ||
                            response.equals(INVALID_DETAILS)) {
                        JOptionPane.showMessageDialog(null,"Server says: " + response);
                        return;
                    }


                    String[] responseList = response.split(",");
                    assert responseList.length == TOTAL_ITEMS + 2;

                    String custName = responseList[TOTAL_ITEMS + 1];
                    String eta = responseList[TOTAL_ITEMS];
                    invoiceHeading.setText(INVOICE);
                    custDetails.setText("Customer: "  + custName);
                    etaBox.setText("ETD: " + eta);

                    int ct = 0, tot_price = 0;
                    for(int i = 0; i < TOTAL_ITEMS; ++i ) pricing[i].setText("");
                    for(int i = 0; i < TOTAL_ITEMS; ++i) {
                        try{
                            Integer qty = Integer.parseInt(responseList[i]);
                            if(qty == 0 ) {
                                continue;
                            }

                            int cost = qty * ITEM_COSTS.get(i);
                            pricing[ct].setText(ITEM_NAMES.get(i) + " (available) : " + qty + " * Rs " + ITEM_COSTS.get(i) + " = " + cost);
                            ct++;
                            tot_price += cost;
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                    assert tot_price > 0;
                    totalPrice.setText("TOTAL(Rs) : " + tot_price);

                } catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
    }
}  