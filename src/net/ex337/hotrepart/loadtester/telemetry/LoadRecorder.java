package net.ex337.hotrepart.loadtester.telemetry;

import java.util.List;

/**
 *
 * The LoadRecorder receives load, collates it and
 * can be polled to retrieve all load since the last
 * poll.
 *
 * @author ian
 */
public interface LoadRecorder extends LoadReceiver {

    /**
     * Adds a worker to generate load for this LoadRecorder
     *
     * @param load the load to be generated.
     */
	public void addLoadGenerator(LoadGenerator load);

    /**
     *
     * The GeneratedLoad objects returned by this method should have their type and time field set.
     *
     * @return a GeneratedLoad collating all load generated by this recorder since the last poll.
     */
	public GeneratedLoad getGeneratedLoadToDate();

    /**
     * @return a list of all Throwables generated by this LoadRecorder in the course of generating this load.
     */
	public List<Throwable> getThrowablesToDate();

    /**
     * Stops generating load.
     */
	public void shutdown();

}
