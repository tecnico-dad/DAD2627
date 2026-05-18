package didatrade.util;

import io.grpc.stub.StreamObserver;

public class CollectorStreamObserver<T> implements StreamObserver<T> {

    didatrade.util.GenericResponseCollector collector;
    private boolean done;

    public CollectorStreamObserver (GenericResponseCollector c) {
        collector = c;
	this.done = false;
    }

    @Override
    public void onNext(T value) {
        // Handle the received response of type T
        // System.out.println("Received response: " + value);
	if (this.done == false) {
	    collector.addResponse(value);
	    this.done = true;
	}
    }

    @Override
    public void onError(Throwable t) {
        // Handle error
        // System.err.println("Error occurred: " + t.getMessage());
	if (this.done == false) {
	    collector.addNoResponse();
	    this.done = true;
	}
    }

    @Override
    public void onCompleted() {
        // Handle stream completion
        // System.out.println("Stream completed");
	if (this.done == false) {
	    collector.addNoResponse();
	    this.done = true;
	}
    }
}
