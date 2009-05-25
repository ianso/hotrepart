package net.ex337.hotrepart.loadtester.telemetry;

import java.util.List;

/**
 * Represents one packet of generated load.
 * Can result from either one operation or many, see constructors.
 *
 * @author ian
 */
public class GeneratedLoad {
	
	private int numOps = 0;
	private int opsSize = 0; //b
	private long duration = 0; //ms
	private String type = null; //ms
    private long time;

    /**
     *
     * Constructor for an individual operation or series of operations.
     *
     * @param numOps the number of operations
     * @param opsSize the approx size in bytes of the traffic generated
     * @param duration the time taken for the load to be generated.
     */
    public GeneratedLoad(int numOps, int opsSize, long duration) {
		this.numOps = numOps;
		this.opsSize = opsSize;
		this.duration = duration;
	}

    /**
     *
     * Constructor for collating a large number of operations.
     *
     * @param loads the loads to be collated
     * @param type the type of the load
     * @param time the time at which this load was collated.
     */
	public GeneratedLoad(List<GeneratedLoad> loads, String type, long time) {
        this.type = type;
        this.time = time;
		for(GeneratedLoad load : loads) {
			numOps += load.getNumOps();
			opsSize += load.getOpsSize();
			duration += load.getDuration();
		}
	}
	
	public int getNumOps() {
		return numOps;
	}
	public int getOpsSize() {
		return opsSize;
	}

	public long getDuration() {
		return duration;
	}

    public String getCategory() {
        return type;
    }

    public long getTime() {
        return time;
    }
    
    @Override
	public String toString() {
		return
                (type == null ? "" : type+" ") +
                (time == 0 ? "" : "at " + time + " ") +
                numOps+" ops, "+opsSize+"b, "+duration+"ms";
	}

}
