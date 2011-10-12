/*
 * Created on Sep 10, 2005
 *
 * Rutgers University, Department of Electrical and Computer Engineering
 * <P> Copyright (c) 2005 Rutgers University
 */
package ch4.tcp;

/**
 * The <b>main class</b> of a simple simulator for TCP congestion
 * control.
 * The simulated network consists of the network elements of sender-host,
 * router, and receiver-host, connected in a chain as follows:
 * <P><CENTER>
 * <code> SENDER <-> ROUTER <-> RECEIVER </code>
 * </CENTER>
 * <P>The sender host sends only data segments and the receiver host
 * only replies with acknowledgements.  In other works, we assume
 * <i>unidirectional transmission</i>, for the sake of simplicity.
 * <P>
 * By default, the simulator reports the values of the congestion
 * control parameters for every iteration:<BR>
 * <ol>
 * <li> Iteration number, starting with zero</li>
 * <li> Congestion window size in this iteration</li>
 * <li> Effective window size in this iteration</li>
 * <li> Flight size (the number of outstanding bytes) in this iteration</li>
 * <li> Slow start threshold size in this iteration</li>
 * </ol>
 * At the end of the simulation, the <i>utilization of the sender</i>
 * is reported.
 * <P>
 * You can turn ON or OFF different levels of reporting by setting
 * the variable {@link int #currentReportingLevel}.
 * <P>
 * Obviously, there are almost no parameters that can be controlled
 * on this "simulator".  The reason is that its main purpose is to
 * serve as a <i>reference example</i> for students to build their
 * own simple simulators, rather than to have a flexible, multi-functional
 * network simulator.
 * 
 * @author Ivan Marsic
 */
public class TCPSimulator {
	/** Simulator's reporting level 1. Reports: <br>
	 * &nbsp; &#183; when TCP segment loss occurs (detected by three or more
	 * duplicate acknowledgements or timeout timer expiration). <BR>
	 * &nbsp; &#183; when the sender enters the congestion avoidance sending mode. */
	public static final int REPORTING_LEVEL_1 = 1 << 1; 

	/** Simulator's reporting level 2: <br>
	 * Reports every new TCP segment that is created. */
	public static final int REPORTING_LEVEL_2 = 1 << 2; 

	/** This field specifies the current reporting level(s)
	 * for this simulator. */
	public static int currentReportingLevel =
		(REPORTING_LEVEL_1 | REPORTING_LEVEL_2);

	/** Maximum segment size, in bytes. */
	public static final int MSS = 1024;

	/** Maximum window size, in MSS units. */
	public static final int MAX_WIN = 100;

	/** Outcome of a previous transmission: <i>success</i>. */
	public static final int SUCCESS = 0;

	/** Outcome of a previous transmission: <i>three or more duplicate
	 * acknowledgements</i>. */
	public static final int DUP_ACKx3 = SUCCESS + 1;

	/** Outcome of a previous transmission: <i>timeout</i>. */
	public static final int TIMEOUT = DUP_ACKx3 + 1;

	private TCPSender sender = null;
	private TCPReceiver receiver = null;
	private Router router = null;

	/**
	 * Constructor of  the simple TCP congestion control simulator.
	 * Instantiates the network components: Sender, Router, and Receiver.
	 * The input arguments are used to set up the router, so that it
	 * represents the bottleneck resource.
	 * 
	 * @param mismatchRatio_ The given mismatch ratio between the transmission speeds on the input and output links.
	 * @param bufferSize_ The given buffer size for the router's queue.
	 * @see Router
	 */
	public TCPSimulator(int mismatchRatio_, int bufferSize_) {
		// Another option is: TCPSenderReno()
		sender = new TCPSenderTahoe();
		receiver = new TCPReceiver();
		router = new Router(mismatchRatio_, bufferSize_);
	}

	/**
	 * Runs the simulator for the given number of transmission rounds
	 * (iterations).  Reports the outcomes of the individual
	 * transmissions.  At the end, reports the overall sender
	 * utilization.
	 * <P>
	 * <b>Notice:</b> The router is invoked to relay only the data
	 * segments (and it may drop some of them).  For the sake
	 * of simplicity, the acknowledgement segments simply
	 * bypass the router, so they are never dropped.
	 * 
	 * @param num_iter_ The number of iterations (transmission rounds) to run the simulator.
	 * @see Router
	 */
	public void run(int num_iter_) {	
		TCPSegment[] segments_ = new TCPSegment[MAX_WIN];
		TCPSegment[] acks_ = new TCPSegment[MAX_WIN];

		// Initialize the arrays with "no packet"
		for (int i_ = 0; i_ < MAX_WIN; i_++) {
			segments_[i_] = null;	// TCP packets, called segments
			acks_[i_] = null;		// Acknowledgement segments
		}

		// Print the headline for the output columns:
		System.out.println(
			"Iter\tCongWindow\tEffctWindow\tFlightSize\tSSThresh"
		);
		System.out.println(
			"================================================================"
		);
		// Receiver's available buffer size:
		int rcvWindow = receiver.getRcvWindow();

		// Iterate for the given number of transmission rounds.
		// Each transmission round is one RTT cycle long.
		for (int i_ = 1; i_ <= num_iter_; i_++) {

			int outcome_ = SUCCESS;
			// Process the ACKs, except for the 1st sending.
			// Skip for the 1st round, because nothing was sent yet.
			if (i_ != 1) {
				outcome_ = sender.processAcks(acks_); 
			}
			if (
				(outcome_ == DUP_ACKx3) &&
				((currentReportingLevel  & REPORTING_LEVEL_1) != 0)
			) {
				System.out.println(
					"iter = " + (i_-1) +
					" ..... Three (or more) duplicate ACKs received! ....."
				);
			} else if (
				(outcome_ == TIMEOUT) &&
				((currentReportingLevel  & REPORTING_LEVEL_1) != 0)
			) {
				System.out.println(
					"iter = " + (i_-1) + " ***** Timeout occured! *****"
				);
			}

			System.out.print((i_-1) + "\t");

			// TCP sender sends the segments.
			sender.send(segments_, rcvWindow, outcome_ != SUCCESS);

			// The router relays the segments (and drops some.)
			router.relay(segments_);

			// TCP receiver receives the segments and
			// generates the acknowledgements.
			rcvWindow = receiver.receive(segments_, acks_);

		} //end for() loop

		System.out.println(
			"================================================================"
		);
		// How many bytes were transmitted:
		int actualTotalTransmitted_ = sender.getTotalBytesTransmitted();

		// How many bytes could have been transmitted with the given
		// bottleneck capacity, if there were no losses due to
		// exceeding the bottleneck capacity:
		int potentialTotalTransmitted_ =
			router.getBottleneckCapacity() * num_iter_;

		// Report the utilization of the sender:
		float utilization_ =
			(float) actualTotalTransmitted_ / (float) potentialTotalTransmitted_;
		System.out.println(
			"Sender utilization: " + Math.round(utilization_*100.0f) + " %"
		);
	} //end the function run()

	/** The main method. Takes the number of iterations as
	 * the input and runs the simulator.
	 * @param argv_ Input argument(s) should contain the number of iterations to run.
	 */
	public static void main(String[] argv_) {	
		if (argv_.length < 1) {
			System.err.println("Please enter the number of iterations!");
			System.exit(1);
		}

		// Notice: You could alter this program, so these values
		// are entered on the command line, if desired so.
		int mismatch_ratio_ = 10;
		int buffer_size_ = 7;

		// Create the simulator.
		TCPSimulator simulator =
			new TCPSimulator(mismatch_ratio_, buffer_size_);

		// Extract the number of iterations (transmission rounds) to run
		// from the command line argument.
		Integer numIter_ = new Integer(argv_[0]);

		// Run the simulator for the given number of transmission rounds.
		simulator.run(numIter_.intValue());
    }
}
