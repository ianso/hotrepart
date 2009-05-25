package net.ex337.hotrepart.loadtester.telemetry;

import java.util.List;

/**
 *
 * The interface used to report generated load.
 *
 * @author ian
 */
public interface LoadReceiver {

    /**
     *
     * Adds a packet of load to the receiver.
     *
     * @param load the load generated
     */
	public void addGeneratedLoad(GeneratedLoad load);

    /**
     * Registers an exception to be logged.
     * @param t the exception
     */
	public void addThrowable(Throwable t);

    /**
     * Registers a list of exceptions to be logged.
     * @param t the exception
     */
    public void addThrowables(List<Throwable> t);

}
