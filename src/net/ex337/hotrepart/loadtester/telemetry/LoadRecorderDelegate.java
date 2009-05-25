package net.ex337.hotrepart.loadtester.telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * The implementation of the LoadRecorder logic
 * for the different types of thread pool that
 * generate load.
 *
 * @author ian
 */
public class LoadRecorderDelegate implements LoadRecorder {
	
	private BlockingQueue<GeneratedLoad> generatedLoads;
	private BlockingQueue<Throwable> throwables;
    private String type;
	
	public LoadRecorderDelegate(String type) {
		this.generatedLoads = new LinkedBlockingQueue<GeneratedLoad>();
		this.throwables = new LinkedBlockingQueue<Throwable>();
        this.type = type;
	}

	public void addGeneratedLoad(GeneratedLoad load) {
		generatedLoads.add(load);
	}

	public GeneratedLoad getGeneratedLoadToDate() {
		List<GeneratedLoad> loadsToDate = new ArrayList<GeneratedLoad>();
		generatedLoads.drainTo(loadsToDate);
        return new GeneratedLoad(loadsToDate, type, System.currentTimeMillis());
	}
	
	public void addThrowable(Throwable t) {
		throwables.add(t);
	}
	public void addThrowables(List<Throwable> t) {
		throwables.addAll(t);
	}
	
	public List<Throwable> getThrowablesToDate() {
		List<Throwable> throwableResult = new ArrayList<Throwable>();
		throwables.drainTo(throwableResult);
		return throwableResult;
	}

    /*
     * The below methods should not be delegated, but instead
     * implemented directly by the delegater.
     */

	public void addLoadGenerator(LoadGenerator load) {
		throw new IllegalArgumentException("Should not implement this method");
	}

	public void shutdown() {
		throw new IllegalArgumentException("Should not implement this method");
	}

}