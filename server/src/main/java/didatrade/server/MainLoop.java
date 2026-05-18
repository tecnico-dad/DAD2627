package didatrade.server;

import java.util.*;

import didatrade.DidaTradeMain;
import didatrade.DidaTradePaxos;
import didatrade.DidaTradePaxosServiceGrpc;

import didatrade.util.GenericResponseCollector;
import didatrade.util.CollectorStreamObserver;
import didatrade.util.PhaseOneBogusProcessor;
import didatrade.util.PhaseTwoResponseProcessor;

import didatrade.configs.ConfigurationScheduler;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;


public class MainLoop implements Runnable  {
    DidaTradeServerState server_state;

    private boolean has_work;
    private int next_log_entry;
    private List<Integer> all_participants;
    private int n_participants;
    private String[] targets;
    private ManagedChannel[] channels;
    private DidaTradePaxosServiceGrpc.DidaTradePaxosServiceStub[] async_stubs;
    
    
    public MainLoop(DidaTradeServerState state) {
	this.server_state   = state;
	this.has_work       = false;
	this.next_log_entry = -1;
    }

    public void run() {
	while (true) {
	    this.next_log_entry++;
	    this.processEntry(this.next_log_entry);
	}
    }
      
    public synchronized void wakeup() {
	this.has_work = true;
	notify();    
    }
   
 
    public synchronized void processEntry(int entry_number) {
		
	PaxosInstance next_entry = this.server_state.paxos_log.testAndSetEntry(entry_number);

	while (next_entry.decided == false) {
	    RequestRecord request_record   = this.server_state.req_history.getFirstPending();
	    int           ballot           = this.server_state.getCurrentBallot ();
	    int           completed_ballot = this.server_state.getCompletedBallot ();

	    List<Integer> acceptors        = this.server_state.scheduler.acceptors (ballot);
	    int           quorum           = this.server_state.scheduler.quorum (ballot);
	    int           n_acceptors      = acceptors.size();
	    
	    if ((ballot > -1) && (request_record != null) && (this.server_state.scheduler.leader(ballot)==this.server_state.my_id)) {
		
		System.out.println("I am the leader for request with id " + request_record.getId());
		boolean ballot_aborted   = false;
		int phase_one_readballot = -1;
		int phase_two_value      = request_record.getId();
		
		// Paxos Phase One
		System.out.println("Going to run paxos phase 1");


		// send phase1
		DidaTradePaxos.PhaseOneRequest.Builder phase_one_request_builder = DidaTradePaxos.PhaseOneRequest.newBuilder();
		phase_one_request_builder.setInstance(entry_number);
		phase_one_request_builder.setRequestballot(ballot);

		DidaTradePaxos.PhaseOneRequest phase_one_request = phase_one_request_builder.build();
		System.out.println("Going to send phase 1" + phase_one_request);
		
		int low_ballot = Math.max (completed_ballot, 0);
		int high_ballot = ballot;

		
		// PhaseOneResponseProcessor phase_one_processor = new PhaseOneResponseProcessor(this.server_state.scheduler, low_ballot, high_ballot);
		PhaseOneBogusProcessor phase_one_processor = new PhaseOneBogusProcessor(this.server_state.scheduler, low_ballot, high_ballot);
		
		ArrayList<DidaTradePaxos.PhaseOneReply> phase_one_responses = new ArrayList<DidaTradePaxos.PhaseOneReply>();
		GenericResponseCollector<DidaTradePaxos.PhaseOneReply>  phase_one_collector = new GenericResponseCollector<DidaTradePaxos.PhaseOneReply>(phase_one_responses, n_acceptors, phase_one_processor);
		
		for (int i = 0; i < n_acceptors; i++) {
		    CollectorStreamObserver<DidaTradePaxos.PhaseOneReply> phase_one_observer  = new CollectorStreamObserver<DidaTradePaxos.PhaseOneReply>(phase_one_collector);
		    this.server_state.async_stubs[acceptors.get(i)].phaseone(phase_one_request, phase_one_observer);
		}
		
		
		phase_one_collector.waitUntilDone();
		if (phase_one_processor.getAccepted() == false) {
		    ballot_aborted = true;
		    int maxballot = phase_one_processor.getMaxballot();
		    if (maxballot > this.server_state.getCurrentBallot())
			this.server_state.setCurrentBallot(maxballot);
		}
		else if (phase_one_processor.getValballot() > -1)
		    phase_two_value = phase_one_processor.getValue();

		
		System.out.println("Paxos phase 1 ended with aborted = " + ballot_aborted + " and read ballot = " + phase_one_processor.getValballot() + " and value " + phase_two_value);


		// Paxos Phase Two
		if (ballot_aborted == false) {			
			System.out.println("Going to run paxos phase 2");
		
			// send phase2
			DidaTradePaxos.PhaseTwoRequest.Builder phase_two_request = DidaTradePaxos.PhaseTwoRequest.newBuilder();
			phase_two_request.setInstance(entry_number);
			phase_two_request.setRequestballot(ballot);
			phase_two_request.setValue(phase_two_value);


			PhaseTwoResponseProcessor phase_two_processor = new PhaseTwoResponseProcessor(quorum);
			
			System.out.println("Calling peers with phase_two_request = " + phase_two_request);
			ArrayList<DidaTradePaxos.PhaseTwoReply> phase_two_responses = new ArrayList<DidaTradePaxos.PhaseTwoReply>();
			GenericResponseCollector<DidaTradePaxos.PhaseTwoReply> phase_two_collector = new GenericResponseCollector<DidaTradePaxos.PhaseTwoReply>(phase_two_responses, n_acceptors, phase_two_processor);
			for (int i = 0; i < n_acceptors  ; i++) {
			    CollectorStreamObserver<DidaTradePaxos.PhaseTwoReply> phase_two_observer = new CollectorStreamObserver<DidaTradePaxos.PhaseTwoReply>(phase_two_collector);
			    this.server_state.async_stubs[acceptors.get(i)].phasetwo(phase_two_request.build(), phase_two_observer);
			}
			
			System.out.println("Waiting for responses...");
			phase_two_collector.waitUntilDone();
			if (phase_two_processor.getAccepted() == false) {
			    ballot_aborted = true;
			    this.server_state.setCurrentBallot (phase_two_processor.getMaxballot());
			}
			System.out.println("Paxos phase 2 ended with ballot_aborted = " + ballot_aborted);
		    }

		// After phase2
		if (ballot_aborted == false) {
		    this.server_state.setCompletedBallot(ballot);
		    next_entry.command_id    = phase_two_value;
		    next_entry.decided       = true;
		}
	    }
	    if (next_entry.decided == false) {
		System.out.println("Entry not decided: waiting");
		this.has_work = false;
		while (this.has_work == false) {
		    try {
			wait ();
		    }
		    catch (InterruptedException e) {
		    }
		}
	    }
	    
	}


	System.out.println("Log entry with number " + this.next_log_entry + " has been decided with command id = "+ next_entry.command_id);
	RequestRecord request_record = this.server_state.req_history.getIfPending(next_entry.command_id);
	// if I receive the paxos decision before the request
	while (request_record == null) {
	    System.out.println("Record not available!");
	    try {
		wait ();
	    }
	    catch (InterruptedException e) {
	    }
	    request_record = this.server_state.req_history.getIfPending(next_entry.command_id);
	}

	// exec request in entry
	// System.out.println("Going to process command with id = " + next_entry.command_id);

	DidaTradeCommand  command = request_record.getRequest();
	boolean result = false;
	int     balance = 0;
	
	DidaTradeAction  action = command.getAction();

	// System.out.println("Action  = " + action);
	switch (action) {
	case DidaTradeAction.POPULATE:
	    result = this.server_state.trade_manager.populate(command.getQuantity());
	    break;
	case DidaTradeAction.ADDUSER:
	    result = this.server_state.trade_manager.add_user(command.getUserId(), command.getQuantity(), command.getStock());
	    break;
	case DidaTradeAction.SELL:
	    result = this.server_state.trade_manager.sell(command.getUserId(), command.getQuantity());
	    break;
	case DidaTradeAction.BUY:
	    result = this.server_state.trade_manager.acquire(command.getUserId(), command.getQuantity());
	    break;
	case DidaTradeAction.BALANCE:
	    balance = this.server_state.trade_manager.balance(command.getUserId());
	    command.setQuantity(balance);
	    result = (balance != -1);
	    break;
	case DidaTradeAction.DUMP:
	    this.server_state.trade_manager.dump();
	    result = true;
	    break;
	default:
	    result = false;
	    System.err.println("*** Unknown command ****");
	    break;
	}

	// sending response
	System.out.println("Setting response for command with id = " + next_entry.command_id + " with result = " + result);
	request_record.setResponse(result);
	this.server_state.req_history.moveToProcessed(request_record.getId());
    }
}
