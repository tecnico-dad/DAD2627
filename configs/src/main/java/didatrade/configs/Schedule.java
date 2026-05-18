package didatrade.configs;

import java.util.*;


public abstract class Schedule {

    abstract List<Integer> learners(int ballot);
    
    abstract List<Integer> acceptors(int ballot);

    abstract List<Integer> acceptorsinrange(int low_ballot, int high_ballot);

    abstract boolean isacceptor(int node, int ballot);

    abstract List<Integer> allparticipants ();
 
    abstract List<Integer> allparticipantsinballot (int ballot);
    
    abstract Integer leader (int ballot);

    abstract Integer quorum (int ballot);

    abstract boolean fastpaxos(int ballot);
}
