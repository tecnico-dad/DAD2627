package didatrade.core;

import java.util.*;
import java.lang.Math;

public class SaleOrder {
    private User user;
    private int  for_sale;
    private int  sold;

    public SaleOrder(User u, int nitems) {
	this.user     = u;
	this.for_sale = nitems;
	this.sold     = 0;
    }

    public User getUser(){
	return this.user;
    }
    
    public int available () {
	return this.for_sale-this.sold;
    }
    
    public synchronized int sell (int max_items) {
	int items = Math.min(this.available(), max_items);
	this.sold += items;
	this.user.deposit(items);
	return items;
    }
    
}
