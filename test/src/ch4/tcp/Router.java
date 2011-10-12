package ch4.tcp;
/*
 * Created on Sep 10, 2005
 *
 * Rutgers University, Department of Electrical and Computer Engineering
 */

/**
 * This class is a simple simulation of a network router.
 * Expressly crafted to "route" TCP packets.  What it does it to
 * enforce that no more packets are let pass through than what the
 * <i>bottleneck resource capacity</i> allows.
 * <P>
 * The bottleneck resource that we study using this router is the
 * buffer space, which determines the maximum possible queue length
 * (the queue capacity).
 * If more packets arrive than the queue (buffer) can hold, the excess
 * packets are discarded.
 * <P>
 * <b>Notice:</b> It is assumed that this router drops only the
 * data segments, if they arrive in excess of the capacity.
 * There is <i>no limit</i> acknowledgement segments, so
 * acknowledgement segments are <i>never</i> discarded.<BR>
 * In fact, if you check the <code>TCPSimulator</code> code, method
 * <code>run()</code>, the router is not even invoked for the
 * acknowledgement segments.  Of course, this is only for the sake
 * of simplicity, and in the real world all packets are subjected
 * to the same treatment at the network level.
 * 
 * @author Ivan Marsic
 */
public class Router {
	/** Mismatch ratio of transmission speeds between the input and
	 * output links of this router.
	 * For example, a mismatch ratio of 10 means that the input
	 * link can transmit packets ten time faster than the output link.
	 * Because of this, the packets arriving at the router must
	 * enter the queue (the temporary buffer storage) and wait for
	 * their turn for transmission. */
	private int mismatchRatio;

	/** Available buffer capacity, in packets. If more packets arrive
	 * than the buffer size allows for queuing, the excess packets
	 * will be discarded.
	 * <P>
	 * To support a meaningful experimentation, the value of this
	 * variable is forced to be smaller than {@link int #mismatchRatio}. */
	private int bufferSize;

	/**
	 * Constructor silently enforces that this router does introduce
	 * a resource bottleneck in the network.  That is, the input argument
	 * specifying the router's buffer size is forced to be smaller
	 * than the mismatch ratio between the router's input and output
	 * links.
	 * 
	 * @param mismatchRatio_ The given mismatch ratio between the transmission speeds on the input and output links.
	 * @param bufferSize_ The given buffer size for the router's queue.
	 */
	public Router(int mismatchRatio_, int bufferSize_) {
		mismatchRatio = mismatchRatio_;
		bufferSize = bufferSize_;

		// Silently enforce the condition that:  bufferSize < mismatchRatio
		// for otherwise this router would not represent the network bottleneck.
		if (bufferSize >= mismatchRatio) {
			bufferSize = mismatchRatio - 1;
		}
	}

	/**
	 * Accessor for retrieving the capacity of the bottleneck resource
	 * of this router.  This value tells us the maximum number of
	 * bytes the router can relay per unit of time (RTT in our case).
	 * If the router receives more than the bottleneck capacity, the
	 * excess packets will be discarded.
	 * <P>
	 * The router can at any time be transmitting one packet and
	 * can hold up to {@link int #bufferSize} packets in queue. So,
	 * the router's capacity is calculated as: <BR>
	 * <CENTER>
	 * <code> (bufferSize + 1) &#215; TCPSimulator.MSS </code>
	 * </CENTER>
	 * 
	 * @return Returns this router's transmission capacity [in bytes].
	 */
	public int getBottleneckCapacity() {
		// Buffer size in bytes equals to buffer size in packets
		// times the maximum size of each packet, which is MSS.
		// (Notice that we ignore the TCP segment header.)
		return (bufferSize + 1) * TCPSimulator.MSS;
	}

	/**
	 * Lets the first {@link int #bufferSize} packets pass
	 * unaffected and discards the rest up to {@link int #mismatchRatio}.
	 * For the remaining packets, samples one out of every
	 * <code>mismatchRatio</code> and discards the rest.
	 * <P>
	 * In our example, we assume the mismatch of 10:1 of the pipe size.
	 * Then, of the first bunch of 10, 7 will get through and the last
	 * three will be discarded.  For the subsequent bunches of 10
	 * out of every 10 lets the (arbitrarily decided) 2nd go and
	 * discards all the others.  For example, if packets 1, 2, ..., 16
	 * are received, only the packets 1, 2, ..., 7, and 12
	 * will be passed.  The rest are discarded.
	 * The discarded packets are marked with <code>null</code> in
	 * the <code>packets_</code> array.
	 * 
	 * @param packets_ The array of packets to be "routed".
	 */
	public void relay(TCPSegment[] packets_) {
		// Process the first bunch of the packets.
		// The router can buffer up to "bufferSize" packets,
		// so all packets in excess of this value but up to
		// the "mismatchRatio" will be discarded.
		// In our example, bufferSize=7 so the 8th, 9th, and 10th
		// packets of the first bunch of 10 (=mismatch_ratio)
		// will be discarded.
		for (int i = bufferSize; i < mismatchRatio; i++) {
			// Mark the discarded packet with "null"
			packets_[i] = null;
		}

		// Arbitrarily decide to let pass the 2nd packet of each
		// of the subsequent bunches of "mismatchRatio" packets.
		// The index is "1" because indexes go from 0, 1, 2, ...
		// and "1" is the 2nd.
		int idx_let_pass_ = 1;

		// Now, process all the remaining bunches, discarding all
		// the packets but the 2nd of each bunch.
		for (int i = mismatchRatio; i < packets_.length; i++) {
			if ((i % mismatchRatio) != idx_let_pass_) {
				// Mark the discarded packet with "null"
				packets_[i] = null;
			}
		}
	}
}
