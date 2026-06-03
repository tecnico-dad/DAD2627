
package didatrade.app;


import java.util.*;

import didatrade.core.User;

import didatrade.DidaTradeMain;
import didatrade.DidaTradeMainServiceGrpc;

import didatrade.util.GenericResponseCollector;
import didatrade.util.CollectorStreamObserver;

import didatrade.configs.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;


public class DidaTradeApp {
    public static final int     MAX_PARTICIPANTS = 100;
    private boolean interactive_mode;
    private int user_range;
    private int fraction_range;
    private int sleep_range;
    private int loop_size;
    private Random rnd;
    private String host;
    private int port;
    private int n_servers;
    private int client_id;
    private int sequence_number;
    private int responses_needed;
    private String[] targets;
    private ManagedChannel[] channels;
    private DidaTradeMainServiceGrpc.DidaTradeMainServiceStub[] async_stubs;
    private char schedule;
    private ConfigurationScheduler scheduler;
    
    
    public DidaTradeApp () {
	this.interactive_mode = true;
	this.user_range       = 100;
	this.fraction_range   = 100;
	this.sleep_range      = 6;
	this.loop_size        = 20;
	this.rnd              = new Random();
	this.n_servers        = 7;
	this.client_id        = 1;
	this.port             = 9000;
	this.host             = "localhost";
	this.sequence_number  = 0;
	this.responses_needed = 1;
	this.rnd              = new Random();
	this.targets          = new String[n_servers];
	this.schedule         = 'A';
	this.scheduler        = null;
    }


    private boolean populate (int quantity) {
	this.sequence_number = this.sequence_number+1;
	int reqid            = this.sequence_number*100 + client_id;
	boolean result       = false;

	
	DidaTradeMain.PopulateRequest.Builder populate_request = DidaTradeMain.PopulateRequest.newBuilder();
	populate_request.setReqid(reqid);
	populate_request.setQuantity(quantity);

	
	ArrayList<DidaTradeMain.PopulateReply> populate_responses = new ArrayList<DidaTradeMain.PopulateReply>();
	GenericResponseCollector<DidaTradeMain.PopulateReply> populate_collector = new GenericResponseCollector<DidaTradeMain.PopulateReply> (populate_responses, this.n_servers);
	
	for (int i = 0; i < n_servers; i++) {
	    CollectorStreamObserver<DidaTradeMain.PopulateReply> populate_observer = new CollectorStreamObserver<DidaTradeMain.PopulateReply>(populate_collector);
	    async_stubs[i].populate(populate_request.build(), populate_observer);
	}
	populate_collector.waitForQuorum(responses_needed);
	if (populate_responses.size() >= responses_needed) {
	    Iterator<DidaTradeMain.PopulateReply> populate_iterator = populate_responses.iterator();
	    DidaTradeMain.PopulateReply populate_reply = populate_iterator.next ();
	    result = populate_reply.getResult();
	    if (result) {
		System.out.println("User DB has been populated with " + quantity + "users.\n");
	    } else {
		System.out.println("User DB populate failed\n");
	    }
	}
	else
	    System.out.println("Panic...error executing populate\n");

	return result;
    }

    private boolean adduser (int uid, int cash, int stock) {
	this.sequence_number = this.sequence_number+1;
	int reqid            = this.sequence_number*100 + client_id;
	boolean result       = false;

	DidaTradeMain.AddUserRequest.Builder adduser_request = DidaTradeMain.AddUserRequest.newBuilder();
	adduser_request.setReqid(reqid);
	adduser_request.setUserid(uid);
	adduser_request.setCash(cash);
	adduser_request.setStock(stock);

	ArrayList<DidaTradeMain.AddUserReply> adduser_responses = new ArrayList<DidaTradeMain.AddUserReply>();
	GenericResponseCollector<DidaTradeMain.AddUserReply> adduser_collector = new GenericResponseCollector<DidaTradeMain.AddUserReply> (adduser_responses, this.n_servers);
	
	for (int i = 0; i < n_servers; i++) {
	    CollectorStreamObserver<DidaTradeMain.AddUserReply> adduser_observer = new CollectorStreamObserver<DidaTradeMain.AddUserReply>(adduser_collector);
	    async_stubs[i].adduser(adduser_request.build(), adduser_observer);
	}
	adduser_collector.waitForQuorum(responses_needed);
	if (adduser_responses.size() >= responses_needed) {
	    Iterator<DidaTradeMain.AddUserReply> adduser_iterator = adduser_responses.iterator();
	    DidaTradeMain.AddUserReply adduser_reply = adduser_iterator.next ();
	    result = adduser_reply.getResult();
	    if (result) {
		System.out.println("User " + uid + " added with cash " + cash + "and stock" + stock + "% of stock\n");
	    } else {
		System.out.println("Adduser operation failed\n");
	    }
	}
	else
	    System.out.println("Panic...error in sell operation\n");

	return result;
    }

    private boolean sell (int uid, int quantity) {
	this.sequence_number = this.sequence_number+1;
	int reqid            = this.sequence_number*100 + client_id;
	boolean result       = false;

	DidaTradeMain.SellRequest.Builder sell_request = DidaTradeMain.SellRequest.newBuilder();
	sell_request.setReqid(reqid);
	sell_request.setUserid(uid);
	sell_request.setQuantity(quantity);

	ArrayList<DidaTradeMain.SellReply> sell_responses = new ArrayList<DidaTradeMain.SellReply>();
	GenericResponseCollector<DidaTradeMain.SellReply> sell_collector = new GenericResponseCollector<DidaTradeMain.SellReply> (sell_responses, this.n_servers);
	
	for (int i = 0; i < n_servers; i++) {
	    CollectorStreamObserver<DidaTradeMain.SellReply> sell_observer = new CollectorStreamObserver<DidaTradeMain.SellReply>(sell_collector);
	    async_stubs[i].sell(sell_request.build(), sell_observer);
	}
	sell_collector.waitForQuorum(responses_needed);
	if (sell_responses.size() >= responses_needed) {
	    Iterator<DidaTradeMain.SellReply> sell_iterator = sell_responses.iterator();
	    DidaTradeMain.SellReply sell_reply = sell_iterator.next ();
	    result = sell_reply.getResult();
	    if (result) {
		System.out.println("User " + uid + " did sell " + quantity + "% of stock\n");
	    } else {
		System.out.println("Sell operation failed\n");
	    }
	}
	else
	    System.out.println("Panic...error in sell operation\n");

	return result;
    }

    private boolean buy (int uid, int quantity) {
	this.sequence_number = this.sequence_number+1;
	int reqid            = this.sequence_number*100 + client_id;
	boolean result       = false;
	
	DidaTradeMain.BuyRequest.Builder buy_request = DidaTradeMain.BuyRequest.newBuilder();
	buy_request.setReqid(reqid);
	buy_request.setUserid(uid);
	buy_request.setQuantity(quantity);

	
	ArrayList<DidaTradeMain.BuyReply> buy_responses = new ArrayList<DidaTradeMain.BuyReply>();
	GenericResponseCollector<DidaTradeMain.BuyReply> buy_collector = new GenericResponseCollector<DidaTradeMain.BuyReply> (buy_responses, this.n_servers);
	
	for (int i = 0; i < n_servers; i++) {
	    CollectorStreamObserver<DidaTradeMain.BuyReply> buy_observer = new CollectorStreamObserver<DidaTradeMain.BuyReply>(buy_collector);
	    async_stubs[i].buy(buy_request.build(), buy_observer);
	}
	buy_collector.waitForQuorum(responses_needed);
	if (buy_responses.size() >= responses_needed) {
	    Iterator<DidaTradeMain.BuyReply> buy_iterator = buy_responses.iterator();
	    DidaTradeMain.BuyReply buy_reply = buy_iterator.next ();
	    result = buy_reply.getResult();
	    if (result) {
		System.out.println("User " + uid + " did spend " + quantity + "% of balance\n");
	    } else {
		System.out.println("Buy operation failed\n");
	    }
	}
	else
	    System.out.println("Panic...error in buy operation\n");

	return result;
    }

    private int balance (int uid) {
	this.sequence_number = this.sequence_number+1;
	int reqid            = this.sequence_number*100 + client_id;
	boolean result       = false;
	int balance          = -1;
	
	DidaTradeMain.BalanceRequest.Builder balance_request = DidaTradeMain.BalanceRequest.newBuilder();
	balance_request.setReqid(reqid);
	balance_request.setUserid(uid);

	
	ArrayList<DidaTradeMain.BalanceReply> balance_responses = new ArrayList<DidaTradeMain.BalanceReply>();
	GenericResponseCollector<DidaTradeMain.BalanceReply> balance_collector = new GenericResponseCollector<DidaTradeMain.BalanceReply> (balance_responses, this.n_servers);
	
	for (int i = 0; i < n_servers; i++) {
	    CollectorStreamObserver<DidaTradeMain.BalanceReply> balance_observer = new CollectorStreamObserver<DidaTradeMain.BalanceReply>(balance_collector);
	    async_stubs[i].balance(balance_request.build(), balance_observer);
	}
	balance_collector.waitForQuorum(responses_needed);
	if (balance_responses.size() >= responses_needed) {
	    Iterator<DidaTradeMain.BalanceReply> balance_iterator = balance_responses.iterator();
	    DidaTradeMain.BalanceReply balance_reply = balance_iterator.next ();
	    result = balance_reply.getResult();
	    if (result) {
		balance = balance_reply.getBalance();
		System.out.println("User " + uid + " balance is " + balance + "\n");
	    } else {
		balance = -1;
		System.out.println("Balance operation failed\n");
	    }
	}
	else
	    System.out.println("Panic...error in balance operation\n");

	return balance;
    }


    private boolean show () {
	this.sequence_number = this.sequence_number+1;
	int reqid = this.sequence_number*100 + client_id;
	boolean result = false;

	// System.out.println("Reqid " + reqid);
	
	DidaTradeMain.DumpRequest.Builder dump_request = DidaTradeMain.DumpRequest.newBuilder();
	dump_request.setReqid(reqid);
	
	ArrayList<DidaTradeMain.DumpReply> dump_responses = new ArrayList<DidaTradeMain.DumpReply>();
	GenericResponseCollector<DidaTradeMain.DumpReply> dump_collector = new GenericResponseCollector<DidaTradeMain.DumpReply> (dump_responses, n_servers);
	
	for (int i = 0; i < n_servers; i++) {
	    CollectorStreamObserver<DidaTradeMain.DumpReply> dump_observer = new CollectorStreamObserver<DidaTradeMain.DumpReply>(dump_collector);
	    async_stubs[i].dump(dump_request.build(), dump_observer);
	}
	dump_collector.waitForQuorum(responses_needed);
	if (dump_responses.size() >= responses_needed) {
	    Iterator<DidaTradeMain.DumpReply> dump_iterator = dump_responses.iterator();
	    DidaTradeMain.DumpReply dump_reply = dump_iterator.next ();
	    result = dump_reply.getResult();
	    if (result) {
		System.out.println("Dump requested\n");
	    } else {
		System.out.println("Dump failed\n");
	    }
	}
	else
	    System.out.println("Panic...error on dump request\n");
	
	return result;
   }

    private void doStuff(int task) {
	int counter = 0;
	int next_action = task;

	while (counter < this.loop_size) {
	    if (task == -1)
		next_action = rnd.nextInt(2);

	    int uid      = rnd.nextInt(this.user_range);
	    int quantity = rnd.nextInt(100);
	    if (next_action == 0)
		this.sell (uid, quantity);
	    else
		this.buy (uid, quantity);
            counter++;
        }
    }
	

    public void parseArgs (String[] args) {
	int length = args.length;
	int cursor = 0;

	cursor = 1;
	while (cursor < length) {
	    String option = args[cursor];
	    String[] option_parts = option.split(" ");
            String option_name = option_parts[0].toLowerCase();
            String option_parameter = option_parts.length > 1 ? option_parts[1] : null;

	    switch (option_name) {
	        case "--help":
                    System.out.printf("\n--help");
		    System.out.printf("\n--lenght looplenght");  
  		    System.out.printf("\n--sleep sleeprange");  
 		    System.out.printf("\n-i (iterative mode)\n");
		    cursor++;
		    break;
		case "--lenght":
		    if (option_parameter==null)
			System.err.println("missing looplenght");
		    else 
			this.loop_size = Integer.parseInt(option_parameter);
		    break;
		case "--sleep":
		    if (option_parameter==null)
			System.err.println("missing sleeprange");
		    else 
			this.sleep_range= Integer.parseInt(option_parameter);
		    break;
		case "-i":
		    this.interactive_mode = true;
		    break;
	        default:
		    System.err.println("Unknown option");
		    break;
	     }
	    cursor++;
	}
    }

    public void goInteractive() {
	Scanner scanner = new Scanner(System.in);
        String command;

	boolean keep_going = true;
        
        while (keep_going) {
            System.out.print("app> ");
            command = scanner.nextLine();
            String[] commandParts = command.split(" ");
            String mainCommand = commandParts[0].toLowerCase();
            String parameter1 = commandParts.length > 1 ? commandParts[1] : null;
            String parameter2 = commandParts.length > 2 ? commandParts[2] : null;
            String parameter3 = commandParts.length > 3 ? commandParts[3] : null;

            switch (mainCommand) {
	    case "help":
		System.out.println("\thelp");
		System.out.println("\tpopulate <n_users>");
		System.out.println("\tadd <uid> <wallet> <stock>");
		System.out.println("\tbuy <uid> <quantity>");
		System.out.println("\tsell <uid> <quantity>");
		System.out.println("\tbalance <uid>");
		System.out.println("\tshow");
		System.out.println("\tbuyloop");
		System.out.println("\tsellloop");
		System.out.println("\trandomloop");
		System.out.println("\tlenght loop-lenght");
		System.out.println("\texit");
		break;
	    case "populate":
		System.out.println("populate " + parameter1);
		if (parameter1 != null) {
		    try {
			int n_users =  Integer.parseInt(parameter1);
			if (populate(n_users)){
			    this.user_range = n_users;
			    System.out.println("DB has been populated with " + n_users + " users.");
			}
			else
			    System.out.println("failed to populate.");
		    } catch (NumberFormatException e) {
			System.out.println("usage: populate n_users");
		    }
		} else 
		    System.out.println("usage: populate n_users");
		break;
	    case "add":
		System.out.println("add " + parameter1 + " " + parameter2 + " " + parameter3);
                    if (parameter1 != null && parameter2 != null && parameter3 != null) {
			try {
			    int uid   =  Integer.parseInt(parameter1);
			    int cash  =  Integer.parseInt(parameter2);
			    int stock =  Integer.parseInt(parameter3);
			    if (this.adduser(uid, cash, stock))
				System.out.println("user " + uid + "created.");
			    else
				System.out.println("user " + uid + " create failed.");
			} catch (NumberFormatException e) {
			    System.out.println("usage: add uid cash stocl");
		        }
		    } else 
			System.out.println("usage: add uid cash stock");
                    break;
	    case "sell":
		System.out.println("sell " + parameter1 + " " + parameter2);
                    if (parameter1 != null && parameter2 != null) {
			try {
			    int uid =  Integer.parseInt(parameter1);
			    int quantity =  Integer.parseInt(parameter2);
			    if (this.sell(uid, quantity))
				System.out.println("user " + uid + " sold " + quantity + "% of its stock.");
			    else
				System.out.println("user " + uid + " sell operation failed.");
			} catch (NumberFormatException e) {
			    System.out.println("usage: sell uid quantity");
		        }
		    } else 
			System.out.println("usage: sell uid quantity");
                    break;
	    case "buy":
		System.out.println("buy " + parameter1 + " " + parameter2);
                    if (parameter1 != null && parameter2 != null) {
			try {
			    int uid =  Integer.parseInt(parameter1);
			    int quantity =  Integer.parseInt(parameter2);
			    if (this.buy(uid, quantity))
				System.out.println("user " + uid + " aquired " + quantity + "% of its balance.");
			    else
				System.out.println("user " + uid + " buy operation failed.");
			} catch (NumberFormatException e) {
			    System.out.println("usage: buy uid quantity");
		        }
		    } else 
			System.out.println("usage: buy uid quantity");
                    break;
	    case "balance":
		System.out.println("balance " + parameter1);
                    if (parameter1 != null) {
			try {
			    int uid =  Integer.parseInt(parameter1);
			    int balance = this.balance(uid);
			    if (balance != -1)
				System.out.println("user " + uid + " balance is  " + balance + ".");
			    else
				System.out.println("user " + uid + " balanceoperation failed.");
			} catch (NumberFormatException e) {
			    System.out.println("usage: balance uid");
		        }
		    } else 
			System.out.println("usage: balance uid");
                    break;
	    case "show":
		try {
		    this.show();
		} catch (Exception e) {
		}
		break;
	    case "lenght":
		System.out.println("lenght " + parameter1);
		if (parameter1 != null) {
		    try {
			loop_size=  Integer.parseInt(parameter1);
		    } catch (NumberFormatException e) {
			System.out.println("usage: lenght loop-lenght");
		    }
		} else 
		    System.out.println("usage: lenght loop-lenght");
		break;
	    case "looprandom":
		try {
		    this.doStuff(-1);
		} catch (Exception e) {
		}
		break;
	    case "loopsell":
		try {
		    this.doStuff(0);
		} catch (Exception e) {
		}
		break;
	    case "loopbuy":
		try {
		    this.doStuff(1);
		} catch (Exception e) {
		}
		break;
	    case "exit":
		keep_going = false;
		break;
	    case "":
		break;
	    default:
		System.out.println("Unknown command: " + mainCommand);
		break;
            }
	}
    }
    
    private void initComms () {
	// Let us use plaintext communication because we do not have certificates
	this.channels = new ManagedChannel[n_servers];

	for (int i = 0; i < n_servers; i++) {
	    this.channels[i] = ManagedChannelBuilder.forTarget(targets[i]).usePlaintext().build();
	}
	
	this.async_stubs = new DidaTradeMainServiceGrpc.DidaTradeMainServiceStub[n_servers];

	for (int i = 0; i < n_servers; i++) {
	    this.async_stubs[i] = DidaTradeMainServiceGrpc.newStub(channels[i]);
	}
    }

    private void terminateComms () {
	for (int i = 0; i < n_servers; i++) {
	    this.channels[i].shutdownNow();
	}
    }

    public void main_loop (String[] args) throws Exception {
	System.out.println(DidaTradeApp.class.getSimpleName());

	
	// receive and print arguments
	System.out.printf("Received %d arguments%n", args.length);
	for (int i = 0; i < args.length; i++) {
	    System.out.printf("arg[%d] = %s%n", i, args[i]);
	}

		
	// check arguments
	if (args.length < 4) {
	    System.err.println("Argument(s) missing!");
	    System.err.printf("Usage: java %s client id host port schedule%n", DidaTradeApp.class.getName());
	    return;
	}

	// set client id
	this.client_id =  Integer.parseInt(args[0]);
	if ((this.client_id < 1) || (this.client_id > 99)) {
	    System.err.println("Error: client id needs to be in interval [1,99].");
	    return;
	}

	// set servers
	this.host = args[1];
        this.port = Integer.parseInt(args[2]);

	// set scheduler
	this.schedule  = args[3].charAt(0);
	this.scheduler = new ConfigurationScheduler (schedule);
	this.n_servers = scheduler.allparticipants().size();

	// check arguments
	this.parseArgs(args);

	// print parameters
	System.out.println("Client id = " + this.client_id + " serverhost = " + this.host + " port = " + this.port);
	System.out.println(" loop_size = " + this.loop_size);
	System.out.println(" interactive mode = " + this.interactive_mode);

	// set servers
	for (int i = 0; i < this.n_servers; i++) {
	    int target_port = this.port +i;
	    this.targets[i] = new String();
	    this.targets[i] = this.host + ":" + target_port;
	    System.out.printf("targets[%d] = %s%n", i, this.targets[i]);
	}

	// init the communication stuff
	this.initComms();

	// do work
	if (interactive_mode == false)
	    this.doStuff(-1);
	else
	    this.goInteractive();

	// shutdown
        System.out.println("closing channels...");
	this.terminateComms();
        System.out.println("Exiting...");
	
    }
    
    public static void main(String[] args) throws Exception {
	System.out.println("Starting...");
	
	DidaTradeApp app = new DidaTradeApp();

	app.main_loop(args);
    }
}



