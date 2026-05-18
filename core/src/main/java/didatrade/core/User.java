package didatrade.core;

import java.util.*;

public class User {
    private int id;
    private int wallet;
    private int stock;

    public User(int id) {
	this.id     = id;
	this.wallet = 0;
	this.stock  = 0;
    }

    public User(int id, int cash, int items) {
	this.id     = id;
	this.wallet = cash;
	this.stock  = items;
	
    }
    
    public int getId () {
	return this.id;
    }
    
    public int getBalance () {
	return this.wallet;
    }
    
    public int getStock () {
	return this.stock;
    }

    public Boolean removeStock (int items) {
	if (this.stock >= items) {
	    this.stock -= items;
	    return true;
	}
	else
	    return false;
    }

    public void addStock(int items) {
	this.stock += items;
    }
    
    public Boolean withdraw (int val) {
	if (this.wallet >= val) {
	    this.wallet -= val;
	    return true;
	}
	else
	    return false;
    }

    public void deposit(int val) {
	this.wallet += val;
    }
    
}
