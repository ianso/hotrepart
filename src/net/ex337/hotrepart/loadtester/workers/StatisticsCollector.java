package net.ex337.hotrepart.loadtester.workers;


import net.ex337.hotrepart.loadtester.telemetry.GeneratedLoad;
import net.ex337.hotrepart.loadtester.telemetry.LoadReceiver;
import net.ex337.hotrepart.loadtester.telemetry.LoadRecorder;

/**
 *
 * This worker fires at regular intervals to collate load generated by all the loaders
 * it was initialised with.
 *
 * @author ian
 */
public class StatisticsCollector implements Runnable {

    private LoadReceiver receiver;
	private LoadRecorder[] recorders;
	
	public StatisticsCollector(LoadReceiver receiver, LoadRecorder... recorders) {
        this.receiver = receiver;
        this.recorders = recorders;
	}

	public void run () {
		
		for(LoadRecorder r : recorders) {
            receiver.addThrowables(r.getThrowablesToDate());
            GeneratedLoad load = r.getGeneratedLoadToDate();
            receiver.addGeneratedLoad(load);
		}
		
	}
	
}