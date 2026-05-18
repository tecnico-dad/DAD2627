
package didatrade.server;

import java.util.*;

import didatrade.DidaTradeMain;
import didatrade.DidaTradePaxos;
import didatrade.DidaTradePaxosServiceGrpc;

import didatrade.util.GenericResponseCollector;
import didatrade.util.CollectorStreamObserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.Context;

public class DidaTradePaxosServiceImpl extends DidaTradePaxosServiceGrpc.DidaTradePaxosServiceImplBase {
    DidaTradeServerState server_state;

    public DidaTradePaxosServiceImpl(DidaTradeServerState state) {
	this.server_state = state;
    }
 
    @Override
    public void phaseone(DidaTradePaxos.PhaseOneRequest request, StreamObserver<DidaTradePaxos.PhaseOneReply> responseObserver) {
	// System.out.println("Receive phase1 request: \n" + request);

	int instance          = request.getInstance();
	int ballot            = request.getRequestballot();
	PaxosInstance entry   = this.server_state.paxos_log.testAndSetEntry(instance, ballot);
	boolean accepted      = false;
	int  value            = entry.command_id;
	int  valballot        = entry.write_ballot;

	if (ballot >= this.server_state.getCurrentBallot()) {
	    accepted = true;
	    this.server_state.setCurrentBallot(ballot);
	    entry.read_ballot = ballot;
	}

	int maxballot = this.server_state.getCurrentBallot();

	// System.out.println("Instance = " + instance + " ballot = " + ballot + " current_ballot = " + this.server_state.getCurrentBallot() + " val = " + value + " valballot = " + valballot + " maxballot = " + maxballot + " accepted = " + accepted);
	
	DidaTradePaxos.PhaseOneReply.Builder response_builder = DidaTradePaxos.PhaseOneReply.newBuilder();
	response_builder.setInstance(instance);
	response_builder.setServerid(this.server_state.my_id);
	response_builder.setRequestballot(ballot);
	response_builder.setAccepted(accepted);
	response_builder.setValue(value);
	response_builder.setValballot(valballot);
	response_builder.setMaxballot(maxballot);

	DidaTradePaxos.PhaseOneReply response = response_builder.build();

	// System.out.println("Sending phase1 response: " + response);
	
	responseObserver.onNext(response);
	responseObserver.onCompleted();
    }

    @Override
    public void phasetwo(DidaTradePaxos.PhaseTwoRequest request, StreamObserver<DidaTradePaxos.PhaseTwoReply> responseObserver) {
	// System.out.println ("Receive phase two request: \n" + request);

	int instance          = request.getInstance();
	int ballot            = request.getRequestballot();
	int value             = request.getValue();
	PaxosInstance entry   = this.server_state.paxos_log.testAndSetEntry(instance);
	boolean accepted      = false;
	int  maxballot        = ballot;

	if (ballot >= this.server_state.getCurrentBallot()) {
	    accepted           = true;
	    entry.command_id   = value;
	    entry.write_ballot = ballot;
	    this.server_state.setCurrentBallot(ballot);
	}
	else
	    maxballot = this.server_state.getCurrentBallot();
	

	DidaTradePaxos.PhaseTwoReply.Builder response_builder = DidaTradePaxos.PhaseTwoReply.newBuilder();
	response_builder.setAccepted(accepted);
	response_builder.setInstance(instance);
	response_builder.setServerid(this.server_state.my_id);
	response_builder.setRequestballot(ballot);
	response_builder.setMaxballot(maxballot);


	DidaTradePaxos.PhaseTwoReply response = response_builder.build();
	
	// System.out.println("Sending phase2 response: " + response);
	
	responseObserver.onNext(response);
	responseObserver.onCompleted();
	
	// Notify learners
	if (accepted == true) {
	    
	    Context ctx = Context.current().fork();
	    ctx.run(() -> {
		    List<Integer> learners = this.server_state.scheduler.learners(ballot);
		    int n_targets          = learners.size();
		    
		    DidaTradePaxos.LearnRequest.Builder learn_request_builder = DidaTradePaxos.LearnRequest.newBuilder();
		    learn_request_builder.setInstance(instance);
		    learn_request_builder.setValue(value);
		    learn_request_builder.setBallot(ballot);
		    
		    DidaTradePaxos.LearnRequest learn_request = learn_request_builder.build();
		    
		    // System.out.println("Sending learn request: \n" + learn_request);
		    
		    System.out.println("Paxos acceptor: going to notify learners for entry " + instance + " with timestamp " + ballot + " request = " + learn_request);
		    ArrayList<DidaTradePaxos.LearnReply> learn_responses = new ArrayList<DidaTradePaxos.LearnReply>();
		    GenericResponseCollector<DidaTradePaxos.LearnReply> learn_collector = new GenericResponseCollector<DidaTradePaxos.LearnReply>(learn_responses, n_targets);;
		    for (int i = 0; i < n_targets; i++) {
			CollectorStreamObserver<DidaTradePaxos.LearnReply> learn_observer = new CollectorStreamObserver<DidaTradePaxos.LearnReply>(learn_collector);
			this.server_state.async_stubs[learners.get(i)].learn(learn_request, learn_observer);
		    }
		    // System.out.println("Learn request completed for instance = " + instance);
		});
	}
	
	
    }

    @Override
    public void learn(DidaTradePaxos.LearnRequest request, StreamObserver<DidaTradePaxos.LearnReply> responseObserver) {
	// System.out.println("Receive learn request: \n" + request);

	int instance         = request.getInstance();
	int ballot           = request.getBallot();
	int value            = request.getValue();

	
	synchronized (this) {
	    PaxosInstance entry  = this.server_state.paxos_log.testAndSetEntry(instance);
	    
	    // System.out.println("Paxos learner: learnin entry " + instance + " with timestamp " + ballot);

	    this.server_state.setCurrentBallot(ballot);
	    
	    if (ballot == entry.accept_ballot) {
		entry.n_accepts++;
		System.out.println("Paxos learner for instance " + instance + " : number of accepts " +  entry.n_accepts);
		if (entry.n_accepts >= this.server_state.scheduler.quorum(ballot)) {
		    System.out.println("Paxos learner: waking up the main loop");
		    this.server_state.updateCompletedBallot(ballot);
		    entry.decided = true;
		    this.server_state.main_loop.wakeup ();
		}
	    }
	    else if (ballot > entry.accept_ballot) {
		System.out.println("Paxos learner for instance " + instance + " : resetting ");
		entry.command_id     = value;
		entry.accept_ballot  = ballot;
		entry.n_accepts      = 1;
	    }
	}
	
	DidaTradePaxos.LearnReply.Builder response_builder = DidaTradePaxos.LearnReply.newBuilder();
	response_builder.setInstance(instance);
	response_builder.setBallot(ballot);

	DidaTradePaxos.LearnReply response = response_builder.build();
	
	// System.out.println("Sending learn response");
		
	responseObserver.onNext(response);
	responseObserver.onCompleted();
    }

}
