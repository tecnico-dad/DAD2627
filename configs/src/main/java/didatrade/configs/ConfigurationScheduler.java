package didatrade.configs;

import java.util.*;
import didatrade.configs.Schedule;
import didatrade.configs.ScheduleA;
import didatrade.configs.ScheduleB;

public class ConfigurationScheduler {
    private Schedule s;

    public ConfigurationScheduler() {
	s = new ScheduleA ();
    }

    public ConfigurationScheduler(char option) {
	if (option == 'C')
	    s = new ScheduleC ();
	if (option == 'B')
	    s = new ScheduleB ();
	else 
	    s = new ScheduleA ();
    }

    public void setSchedule (char option) {
	if (option == 'A')
	    s = new ScheduleA ();
	else if (option == 'B')
	    s = new ScheduleB ();   
 	else if (option == 'C')
	    s = new ScheduleC ();   
    }
    
    public List<Integer> learners(int ballot) {
	return s.learners(ballot);
    }
    
    public List<Integer> acceptors(int ballot) {
	return s.acceptors(ballot);
    }
        
    public List<Integer> acceptorsinrange(int low_ballot, int high_ballot) {
	return s.acceptorsinrange(low_ballot, high_ballot);
    }

    public boolean isacceptor (int node, int ballot) {
	return s.isacceptor (node, ballot);
    }
    
    public Integer leader (int ballot) {
	return s.leader(ballot);
    }
    
    public List<Integer> allparticipantsinballot(int ballot) {
	return s.allparticipantsinballot(ballot);
    }
   
    public List<Integer> allparticipants() {
	return s.allparticipants();
    }
    
    public Integer quorum(int ballot) {
	return s.quorum(ballot);
    }

    public boolean fastpaxos(int ballot) {
	return s.fastpaxos(ballot);
    }
}
