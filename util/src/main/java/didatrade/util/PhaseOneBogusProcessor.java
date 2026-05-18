package didatrade.util;

import java.util.ArrayList;
import java.util.Hashtable;

import didatrade.DidaTradePaxos;
import didatrade.DidaTradePaxosServiceGrpc;

import didatrade.configs.ConfigurationScheduler;

public class PhaseOneBogusProcessor extends GenericResponseProcessor<DidaTradePaxos.PhaseOneReply>  {
    private ConfigurationScheduler   scheduler;
    private boolean                  accepted;
    private int                      value;
    private int                      valballot;
    private int                      maxballot;
    private int                      low_ballot;
    private int                      high_ballot;
   
    
    public PhaseOneBogusProcessor (ConfigurationScheduler s, int l, int h) {
	// System.out.println("Phase 1 processor constructor with low_ballot =" + l + " high_ballot = " + h);
	this.accepted    = true;
	this.value       = -1;
	this.valballot   = -1;
	this.maxballot   = -1;
	this.low_ballot  = l;
	this.high_ballot = h;
	this.scheduler   = s;
    }

    public boolean getAccepted() {
	return this.accepted;
    }
    
    public int getValue() {
	return this.value;
    }
    
    public int getValballot() {
	return this.valballot;
    }
    
    public int getMaxballot() {
	return this.maxballot;
    }
    
    public synchronized boolean onNext(ArrayList<DidaTradePaxos.PhaseOneReply> all_responses, DidaTradePaxos.PhaseOneReply last_response){
	this.maxballot = last_response.getMaxballot();
	this.value     = last_response.getValue();
	this.valballot = last_response.getValballot();
	return true;
    }
}
