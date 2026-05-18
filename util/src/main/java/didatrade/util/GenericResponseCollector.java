package didatrade.util;

import java.util.ArrayList;

public class GenericResponseCollector<T>  {
    private GenericResponseProcessor     processor;
    private ArrayList<T>                 collected_responses;
    private int                          received;
    private int                          pending;
    private boolean                      done;

    public GenericResponseCollector(ArrayList<T> responses, int maxresponses) {
	this.processor           = null;
        this.collected_responses = responses;
	this.received            = 0;
	this.pending             = maxresponses;
	this.done                = false;
    }

    public GenericResponseCollector(ArrayList<T> responses, int maxresponses, GenericResponseProcessor p) {
	this.processor           = p;
        this.collected_responses = responses;
	this.received            = 0;
	this.pending             = maxresponses;
	this.done                = false;
    }

    public synchronized void addResponse(T resp) {
	if (!this.done) {
	    collected_responses.add(resp);
	    if (this.processor != null)
		this.done = this.processor.onNext (this.collected_responses, resp);
	}
	this.received++;
	this.pending--;
	if (this.pending==0)
	    this.done = true;
	notifyAll();
    }

    public synchronized void addNoResponse() {
	this.pending--;
	if (this.pending==0)
	    this.done = true;
	notifyAll();
    }
 
    public synchronized void waitForQuorum(int quorum) {
        while ((this.done == false) && (this.received < quorum)) {
            try {
		wait ();
	    }
	    catch (InterruptedException e) {
	    }
	}
	this.done = true;
    }

    
    public synchronized void waitUntilDone() {
        while (this.done == false) {
            try {
		wait ();
	    }
	    catch (InterruptedException e) {
	    }
	}
    }
}
