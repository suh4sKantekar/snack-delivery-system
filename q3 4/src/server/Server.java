package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReentrantLock;


public class Server {
    public static final Integer DELIVERY_TIME = 2*60;
    public static final Integer DRINKS_TIME = 1*60;
    public static final Integer MILLS = 1000;

    public static final Integer DRINKS = 2;
    public static final Integer SNACKS = 2;
    public static final Integer TOTAL_ITEMS = DRINKS + SNACKS; // tea,coffee,s1, ...
    public static final Integer INIT_SNACKS = 100;
    public static final Integer THRES_SNACKS = 10;

    public static final List<String> ITEM_NAMES = new ArrayList<>(Arrays.asList(
            "TEA","COFFEE","BISCUTS","CHIPS"
    ));

    public static final List<Integer> ITEM_COSTS = new ArrayList<>(Arrays.asList(
            5,10,5,20
    ));
    public static final String INVALID = "Invalid order";
    public static final String OUT_OF_STOCK = "Out of stock";
    public static final String ADMIN = "ADMIN";
    public static final Integer PORT = 8000;
    public static final String PATTERN = "yyyy-MM-dd";
    public static final int DELTA_DAYS = 10;

    private static Date serverStartTime;

    private static volatile List<Long> bookedTimes = new ArrayList<>();

    private static volatile BlockingDeque<Thread> preOrder = new LinkedBlockingDeque<>();
    private static volatile BlockingDeque<Thread> preDelivery = new LinkedBlockingDeque<>();

    private static volatile BlockingDeque<Thread>[] fifoItems;
    private static volatile Date[] drinksFree;
    private static volatile Integer[] snacksLeft;

    private static volatile List<SalesRecord> salesRecordList = new ArrayList<>();
    private static final ReentrantLock salesListLock = new ReentrantLock();

    public static void main(String args[]) {
        assert (ITEM_NAMES.size() == TOTAL_ITEMS);
        assert (ITEM_COSTS.size() == TOTAL_ITEMS);

        serverStartTime = new Date();
        bookedTimes.add(-4L*DELIVERY_TIME);
        bookedTimes.add(Long.MAX_VALUE);

        drinksFree = new Date[DRINKS];
        for(int i = 0; i < DRINKS; i++) drinksFree[i] = serverStartTime;

        snacksLeft = new Integer[SNACKS];
        for(int i = 0; i < SNACKS; i++) snacksLeft[i] = INIT_SNACKS;

        fifoItems = new BlockingDeque[TOTAL_ITEMS];
        for(int i = 0; i < TOTAL_ITEMS; i++) {
            fifoItems[i] = new LinkedBlockingDeque<>();
        }

        new Server().startServer();
    }

    public void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Waiting for clients to connect...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                String request = dis.readUTF();

                if(request.startsWith(ADMIN)) {
                    new Thread(new AdminTask(clientSocket,request)).start();
                } else {
                    Thread clientThread = new Thread(new ClientTask(clientSocket, request, new Date()));
                    preOrder.addLast(clientThread);
                    preDelivery.addLast(clientThread);
                    clientThread.start();
                }
            }
        } catch (IOException e) {
            System.err.println("Unable to process client request");
            e.printStackTrace();
        }
    }

    private class AdminTask implements Runnable {
        private final Socket clientSocket;
        private String request;

        public AdminTask(Socket clientSocket, String request) {
            this.clientSocket = clientSocket;
            this.request = request;
        }

        @Override
        public void run() {
            String[] requestArray = request.split(",");
            assert requestArray.length > 0 && requestArray.length < 4;
            try {
                DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                if(requestArray.length == 1) {
                    getSalesList(dos);
                } else if(requestArray.length == 2) {
                    getBelowThreshold(dos);
                } else {
                    getTotalSales(dos,requestArray[1],requestArray[2]);
                }
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private boolean isBetween(Date d, Date dStart, Date dEnd) {
            if(d.after(dEnd)) return false;
            if(d.before(dStart)) return false;
            if(dStart.after(dEnd)) return false;
            return true;
        }

        private void getTotalSales(DataOutputStream dos,String start,String end) {
            try {
                DateFormat dateFormat =  new SimpleDateFormat(PATTERN);
                Date startDate = dateFormat.parse(start);

                Date endDate = dateFormat.parse(end);
                endDate.setHours(23);endDate.setMinutes(59);endDate.setSeconds(59);

                String out = "";
                List<Integer> qtySold = new ArrayList<>(Collections.nCopies(TOTAL_ITEMS,0));
                salesListLock.lock();
                try {
                    for(SalesRecord salesRecord : salesRecordList) {
                        if(isBetween(salesRecord.getOrderDate(),startDate,endDate)) {
                            for(int i = 0; i < TOTAL_ITEMS; i++) {
                                qtySold.set(i, qtySold.get(i) + salesRecord.getQty().get(i));
                            }
                        }
                    }
                } finally {
                    salesListLock.unlock();
                }

                for(int i = 0; i < TOTAL_ITEMS; i++) {
                    out += ITEM_NAMES.get(i) + "_" + qtySold.get(i) + ",";
                }

                if(out.length() > 0 ) {
                    out = out.substring(0,out.length()-1);
                }

                dos.writeUTF(out);
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void getBelowThreshold(DataOutputStream dos) {
            try{
                String out = "";
                for(int i = 0; i < SNACKS; i++) {
                    if(snacksLeft[i] <= THRES_SNACKS) {
                        out += ITEM_NAMES.get(i + DRINKS) + "_" + snacksLeft[i] + ",";
                    }
                }
                if(out.length() > 0 ) {
                    out = out.substring(0,out.length()-1);
                }
                dos.writeUTF(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void getSalesList(DataOutputStream dos) {
            salesListLock.lock();
            try {
                Collections.sort(salesRecordList, new RecordComparator());

                String out = "";
                for(SalesRecord salesRecord :  salesRecordList) {
                    out += salesRecord.toString() + ",";
                }
                if(out.length() > 0 ) {
                    out = out.substring(0,out.length()-1);
                }

                dos.writeUTF(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                salesListLock.unlock();
            }
        }

        public class RecordComparator implements Comparator {
            public int compare(Object o1 , Object o2) {
                SalesRecord s1 = (SalesRecord) o1;
                SalesRecord s2 = (SalesRecord) o2;
                if (s1.getOrderDate().before(s2.getOrderDate())) {
                    return -1;
                } else if (s1.getOrderDate().after(s2.getOrderDate())) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    }

    private class ClientTask implements Runnable {
        private final Socket clientSocket;
        private final Date orderTime;
        private List<Integer> orderAmount;
        private List<Integer> stockAvailable;
        private String custName;
        private String request;

        private ClientTask(Socket clientSocket, String request, Date orderTime) {
            this.clientSocket = clientSocket;
            this.orderTime = orderTime;
            this.orderAmount = new ArrayList<>();
            this.stockAvailable = new ArrayList<>(Collections.nCopies(TOTAL_ITEMS,0));
            this.request = request;
        }

        private Long nextFreeSlot(Long delta) {
            assert bookedTimes.size() >= 2;
            Long bookingTime = -1L;
            for(int i = 0; i < bookedTimes.size() - 1; i++) {
                assert bookedTimes.get(i) + DELIVERY_TIME - 1 < bookedTimes.get(i+1);
                Long maxTime = Math.max(bookedTimes.get(i) + DELIVERY_TIME , delta);
                if(maxTime + DELIVERY_TIME - 1 < bookedTimes.get(i+1)) {
                    bookingTime = maxTime;
                    break;
                }
            }
            assert bookingTime != -1L;
            bookedTimes.add(bookingTime);
            Collections.sort(bookedTimes);
            return bookingTime + DELIVERY_TIME - 1;
        }

        @Override
        public void run() {
            System.out.println("Got a client ! at " + orderTime);
            //modularize
            try {
                DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

                String[] orderArray = request.split(",");
                assert orderArray.length == TOTAL_ITEMS + 1;
                for(int i = 0; i < orderArray.length; i++) {
                    String amt = orderArray[i];
                    if(i == TOTAL_ITEMS) {
                        System.out.println("Customer " + amt);
                        this.custName = amt;
                        break;
                    }

                    try{
                        orderAmount.add(Integer.parseInt(amt));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();

                        dos.writeUTF(INVALID);
                        preOrder.remove(Thread.currentThread());
                        preDelivery.remove(Thread.currentThread());
                        clientSocket.close();
                        return;
                    }
                }

                while(Thread.currentThread() != preOrder.peekFirst()) {
                    try {
                        Thread.sleep(Long.MAX_VALUE);
                    } catch (InterruptedException e) {
                        System.out.println("I woke up from preorder " + orderTime);
                    }
                }

                List<Task> tasks = new ArrayList<>();
                for(int i = 0; i < TOTAL_ITEMS; i++) {
                    if(orderAmount.get(i) > 0) {
                        tasks.add(new Task(orderTime,orderAmount.get(i),i));
                    }
                }
                List<Thread> taskThreads = new ArrayList<>();
                for(Task task : tasks) {
                    Thread thread = new Thread(task);
                    taskThreads.add(thread);
                    fifoItems[task.getItem()].addLast(thread);
                    thread.start();
                }

                preOrder.remove(Thread.currentThread());
                if(preOrder.size() > 0 && preOrder.getFirst().getState() == Thread.State.TIMED_WAITING) {
                    preOrder.getFirst().interrupt();
                }

                Integer outOfStock = 0;
                Date readyTime = orderTime;

                for(int i = 0; i < tasks.size(); i++) {
                    try {
                        taskThreads.get(i).join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Task task = tasks.get(i);
                    if(task.getAvailableAmount() == 0) {
                        outOfStock++;
                    }
                    stockAvailable.set(task.getItem(),task.getAvailableAmount());
                    if(task.getFinishTime().after(readyTime)) {
                       readyTime = task.getFinishTime();
                    }
                }

                if(outOfStock == tasks.size()) {
                    dos.writeUTF(OUT_OF_STOCK);
                    preDelivery.remove(Thread.currentThread());
                    clientSocket.close();
                    return;
                }

                String orderOut = "";
                List<Integer> salesAmt = new ArrayList<>();
                for(int i = 0; i < TOTAL_ITEMS; i++) {
                    orderOut += stockAvailable.get(i) + ",";
                    salesAmt.add(stockAvailable.get(i));
                }

                while(Thread.currentThread() != preDelivery.peekFirst()) {
                    try {
                        Thread.sleep(Long.MAX_VALUE);
                    } catch (InterruptedException e) {
                        System.out.println("I woke up from predelivery " + orderTime);
                    }
                }

                SalesRecord salesRecord = new SalesRecord(custName,orderTime,salesAmt);
                salesListLock.lock();
                try{
                    salesRecordList.add(salesRecord);
                } finally {
                    salesListLock.unlock();
                }

                Long delta = (readyTime.getTime() - serverStartTime.getTime()) / MILLS;
                Long finishTime = nextFreeSlot(delta);
                Date eta = new Date(serverStartTime.getTime() + finishTime * MILLS);
                orderOut += eta;

                preDelivery.remove(Thread.currentThread());
                if(preDelivery.size() > 0 && preDelivery.getFirst().getState() == Thread.State.TIMED_WAITING) {
                    preDelivery.getFirst().interrupt();
                }

                dos.writeUTF(orderOut + "," + custName);
                clientSocket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class Task implements Runnable {
        private final Date orderTime;
        private final Integer orderAmount;
        private final Integer item;

        private Date finishTime;
        private Integer availableAmount;

        public Task(Date orderTime, Integer orderAmount, Integer item) {
            this.orderTime = orderTime;
            this.orderAmount = orderAmount;
            this.item = item;
        }

        public Date getFinishTime() {
            return finishTime;
        }

        public Integer getAvailableAmount() {
            return availableAmount;
        }

        public Integer getItem() {
            return item;
        }

        @Override
        public void run() {
            while(Thread.currentThread() != fifoItems[item].peekFirst()) {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    System.out.println("I woke up from precalc " + orderTime + " " + item);
                }
            }

            if(item < DRINKS) {
                availableAmount = orderAmount;
                if(drinksFree[item].before(orderTime)) {
                    drinksFree[item] = orderTime;
                }
                finishTime = new Date(drinksFree[item].getTime() + DRINKS_TIME * orderAmount * MILLS);
                drinksFree[item] = finishTime;
            } else{
                int index = item - DRINKS;
                finishTime = new Date(orderTime.getTime());
                availableAmount = Math.min(orderAmount,snacksLeft[index]);
                snacksLeft[index] -= availableAmount;
            }

            fifoItems[item].remove(Thread.currentThread());
            if(fifoItems[item].size() > 0 && fifoItems[item].getFirst().getState() == Thread.State.TIMED_WAITING) {
                fifoItems[item].getFirst().interrupt();
            }
        }
    }

}