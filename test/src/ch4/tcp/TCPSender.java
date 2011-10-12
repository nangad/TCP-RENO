package ch4.tcp;
/*
 * Created on Sep 10, 2005
 *
 * Rutgers University, Department of Electrical and Computer Engineering
 * <P> Copyright (c) 2005 Rutgers University
 */

/**
 * The base class for TCP senders.
 * <P>
 * <b>Note</b>: If you are in doubt or some of this code is conflicting
 * your textbook, please check the ultimate sources:
 * <a href="http://www.apps.ietf.org/rfc/rfc2581.html">RFC 2581</a>
 * and <a href="http://www.apps.ietf.org/rfc/rfc2001.html">RFC 2001</a>.
 * <BR><i>Do not rely on any textbooks for precise details!</i>
 * <BR> Read the textbook(s) for high-level understanding of
 * the material; read the RFCs for precise details.
 * 
 * @author Ivan Marsic
 */
public abstract class TCPSender {

 	/** Pointer to the last byte sent so far.
	 * Recall that the bytes are numbered from zero, so the sequence
	 * number of the first byte is zero, etc. */
 	protected int lastByteSent = -1;

 	/** Pointer to the last byte ACKed so far.
 	 * Recall that the bytes are numbered from zero, so the sequence
	 * number of the first byte is zero, etc. */
 	protected int lastByteAcked = -1;

 	/** Current congestion window size, in bytes. */
 	protected int congWindow = TCPSimulator.MSS;

 	/** The threshold value at which the slow start sending mode
 	 * should kick in. */
 	protected int SSThresh = 65535;

 	/** Type of sending mode: <i>Slow start</i>. */
 	protected static final int SLOW_START = 0;

 	/** Type of sending mode: <i>Congestion avoidance</i>,
 	 * i.e., additive increase. */
 	protected static final int CONG_AVOID = 1;

 	/** Current sending mode; default value is SLOW_START. */
 	protected int sendMode = SLOW_START;

 	/** Default value of the timer, in our case equals to {@value} &#215; RTT. */
 	protected static final int TIMER_DEFAULT = 3;

 	/** Retransmission timer, in RTT/iteration units. The timer is
 	 * activated at the begining of a transmission cycle.
 	 * When all outstanding segments are acknowledged, the timer is
 	 * deactivated.  When a <i>regular</i> acknowledgement is received
 	 * <b>and</b> there are still outstanding, non-acknowledged segments,
 	 * the timer should be <b>re-started</b>. */
 	protected int timer = TIMER_DEFAULT;

 	/** Counter of duplicate acknowledgements over multiple
 	 * subsequent RTT periods. Dup-acks must be counted over
 	 * subsequent RTT periods (iterations), not only in a single RTT
 	 * cycle because they still carry the same meaning. */
 	protected int dupACKsGlobal = 0;
 
	/**
	 * Accessor for retrieving the statistics of the total number
	 * of bytes <i>successfully</i> transmitted so far during this
 	 * simulation.  This value is used for statistics gathering and
 	 * reporting purposes.
 	 * 
	 * @return Returns the cumulative number of bytes <i>successfully</i>  transmitted thus far.
	 */
	public int getTotalBytesTransmitted() {
		// NOTE: This assumes that the very first byte received in
		// the entire session had the sequence number equal to _zero_ !!
		return (lastByteAcked + 1);
	}

	/**
 	 * Processes ACKs received from the receiver.
 	 * Checks for duplicate ACKs.  May detect timeout timer
 	 * expiration.
 	 * Informs about the transmission outcome, as one of the
 	 * options: {@link int TCPSimulator#SUCCESS},
 	 * {@link int TCPSimulator#DUP_ACKx3},
 	 * or {@link int TCPSimulator#TIMEOUT}.
 	 * 
 	 * @param acks_ The acknowledgements array, as received from the receiver.
 	 * @return Returns a summary about the outcome of the previous transmission.
 	 */
 	public abstract int processAcks(TCPSegment[] acks_);

 	/**
 	 * "Sends" segments by indicating in the array what segments
 	 * should be sent in this iteration.
 	 * After this method, the array <code>segments_</code> will
 	 * contain the segments to be sent.
 	 * The sender sends only two type of segments: full MSS
 	 * segments and 1-byte segments; the receiver must be able
 	 * to distinguish between these.
 	 * 
 	 * @param segments_ The array of segments, to be filled in this method.
 	 * @param rcvWindow_ The size of the currently available space in the receiver's buffer.
 	 * @param lostPacket_ Informs whether or not a packet loss was detected for the previous transmission.
 	 */
 	public abstract void send(TCPSegment[] segments_, int rcvWindow_, boolean lostPacket_);
}
