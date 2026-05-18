package didatrade.core;

import java.util.*;
import java.lang.Math;

public class BuyOrder {
    private User user;
    private int  to_buy;
    private int  acquired;

    public BuyOrder(User u, int nitems) {
	this.user     = u;
	this.to_buy   = nitems;
	this.acquired = 0;
    }
    
    public User getUser(){
	return this.user;
    }
    
    public int pending () {
	return this.to_buy-this.acquired;
    }
    
    public synchronized int acquire (int max_items) {
	int items = Math.min(this.pending(), max_items);
	this.acquired += items;
	this.user.addStock(items);
	return items;
    }
}
