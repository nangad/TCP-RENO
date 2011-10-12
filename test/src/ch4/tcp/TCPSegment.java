/*
 * Created on Sep 10, 2005
 *
 * Rutgers University, Department of Electrical and Computer Engineering
 * <P> Copyright (c) 2005 Rutgers University
 */
package ch4.tcp;

/**
 * TCP segment, which could carry either data, ACK, or both.
 * Notice that the segment has no <i>data</i> field since data
 * are irrelevant for the purpose of the current simulation.
 * If your simulation requires data, the <i>data</i> field should
 * be added.
 * 
 * @author Ivan Marsic
 */
public class TCPSegment {
	/** Sequence number of this segment, which is the sequence
	 * number of the <i>first byte</i> of data carried in this
	 * segment. */
	public int seqNum = 0;

	/** Segment length [in bytes]. */
	public int length = 0;

	/** Informs whether or not this segment contains ACK. */
	public boolean ack;

	/** Indicates whether this segment is corrupted by an error.
	 * This is invented in lieu of building a full-fledged
	 * error-checking mechanism.  If a router or channel wants
	 * to damage this packet, it just sets the flag to <code>true</code>.*/
	public boolean inError = false;

	/** Ordinal number of this segment.  This is only for tracking
	 * purposes and this field is <i>not</i> present in an actual
	 * TCP segment. */
	public int ordinalNum;

	public TCPSegment(int seqNum_, int length_) {
		this(seqNum_, length_, false);
	}

	public TCPSegment(int seqNum_, int length_, boolean ack_) {
		this.seqNum = seqNum_;
		this.length = length_;
		this.ack = ack_;

		// This must be corrected the count because of 1-byte segments !!
		this.ordinalNum = seqNum / TCPSimulator.MSS + 1;

		// This reporting is for debugging purposes only:
		if (
			(TCPSimulator.currentReportingLevel & TCPSimulator.REPORTING_LEVEL_2) != 0
		) {
			System.out.println(
				"# " + ordinalNum
				+ (ack ? " ack" : ((length == 1) ? " (1-byte)" : ""))
//				+ " " + (ack ? " ack" : Integer.toString(length))
			);
		}
	}
}
