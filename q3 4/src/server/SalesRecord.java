package server;

import java.util.Date;
import java.util.List;

public class SalesRecord {
    private final String custName;
    private final Date orderDate;
    private final List<Integer>  qty;

    public SalesRecord(String custName, Date orderDate, List<Integer> qty) {
        this.custName = custName;
        this.orderDate = orderDate;
        this.qty = qty;
    }

    public String getCustName() {
        return custName;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public List<Integer> getQty() {
        return qty;
    }

    @Override
    public String toString() {
        String ret = custName + "_" + orderDate;
        for(int val : qty) ret += "_" + val;
        return ret;
    }
}
