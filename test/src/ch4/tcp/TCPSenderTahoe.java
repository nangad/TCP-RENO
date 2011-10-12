/*
 * Created on Sep 10, 2005
 *
 * Rutgers University, Department of Electrical and Computer Engineering
 * <P> Copyright (c) 2005 Rutgers University
 */
package ch4.tcp;

/**
 * TCP Tahoe implementation of a sender.
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
public class TCPSenderTahoe extends TCPSender {

	/* (non-Javadoc)
	 * @see ch4.tcp.TCPSender#processAcks(ch4.tcp.TCPSegment[])
	 */
	public int processAcks(TCPSegment[] acks_) {
		// Summary of the transmission outcome, to be returned at the end.
		int retVal_ = TCPSimulator.SUCCESS;

		for (int i_ = 0; i_ < acks_.length; i_++) {
			// Quit, assuming that ACKs are entered contiguously,
			// i.e., with no gaps.  "null" means: "no more ACKs"
			if (acks_[i_] == null) { break; }

			// Indicator of a dupACK received in this segment only.
			boolean dupACKlocal_ = false;

			// If the current mode is "slow start":
			if (sendMode == SLOW_START) {

				dupACKlocal_ = processAcksSlowStart(acks_[i_]);

			} else if (sendMode == CONG_AVOID) {

				dupACKlocal_ = processAcksCongestionAvoidance(acks_[i_]);

			} else {
				System.out.println("TCPSenderTahoe.processAcks(): Wrong sending mode.");
				// Perhaps exit or throw an exception here !?!
			}

			// Update the global count of duplicate ACKs.
			dupACKsGlobal += dupACKlocal_ ? 1 : 0;

			// If three or more duplicate ACKs are received so far:
			if (dupACKsGlobal > 2) {
				// Perform the necessary actions.
				onThreeDuplicateACKs();

				retVal_ = TCPSimulator.DUP_ACKx3;
				break;
			}
		}

		// Check if everything got ACKed:
		if (lastByteSent == lastByteAcked) {
			// Deactivate the timer and reset to the initial value.
			// Reset also the global counter of duplicate ACKs.
			resetMonitoringVariables();

		} else {
			// Some segment(s) are still outstanding,
			// count-down the time:
			timer--;

			// TODO NOTE: This perhaps should check "<=" and consider
			// the timer expired when equal to zero.
			// Please check the IETF RFC for details and implement
			// this correctly.
			if (timer < 0) {

				onExpiredTimeoutTimer();

				retVal_ = TCPSimulator.TIMEOUT;
			}
		}
		return retVal_;
	}

	/**
	 * Helper method to process an individual acknowledgement segment
	 * in the <i>slow start</i> transmission mode.
	 * Determines if this is a <i>duplicate</i> acknowledgement and
	 * reports it.
	 * <P>
	 * The method assumes that the input argument is never equal to
	 * <code>null</code>.
	 * 
	 * @param ack_ The current acknowledgement segment, to be processed.
	 * @return Returns <code>true</code> if this is a duplicate acknowledgement, <code>false</code> otherwise.
	 */
	protected boolean processAcksSlowStart(TCPSegment ack_) {

		// Is this a regularly acknowledged segment (i.e., not a duplicate ACK)?
		if (ack_.seqNum > (lastByteAcked + 1)) {
			lastByteAcked = ack_.seqNum - 1;

			// Update the congestion window size.
			congWindow += TCPSimulator.MSS;

			// The size of the congestion window relative to the
			// size of the SSThresh determines the sending mode:
			if (
				(sendMode == SLOW_START) && (congWindow > SSThresh)
			) {
				// Congestion window exceeded the slow-start-threshold,
				// change the sending mode
				sendMode = CONG_AVOID;

				if (
					(TCPSimulator.currentReportingLevel & TCPSimulator.REPORTING_LEVEL_1) != 0
				) {
					System.out.println("############## Sender entering congestion avoidance.");
				}
			}

			// Every time we receive an ACK for a full segment:
			resetMonitoringVariables();

			return false;

		} else {	// duplicate ACK, report it:
			return true;
		}
	}

	/**
	 * Helper method to process an individual acknowledgement segment
	 * in the <i>congestion avoidance</i> transmission mode.
	 * Determines if this is a <i>duplicate</i> acknowledgement and
	 * reports it.
	 * <P>
	 * The method assumes that the input argument is never equal to
	 * <code>null</code>.
	 * 
	 * @param ack_ The current acknowledgement segment, to be processed.
	 * @return Returns <code>true</code> if this is a duplicate acknowledgement, <code>false</code> otherwise.
	 */
	protected boolean processAcksCongestionAvoidance(TCPSegment ack_) {

		// Is this a regularly acknowledged segment (i.e., not a duplicate ACK)?
		if (ack_.seqNum > (lastByteAcked + 1)) {
			lastByteAcked = ack_.seqNum - 1;

			// Increment the congestion window linearly:
			congWindow +=
				(TCPSimulator.MSS * TCPSimulator.MSS) / congWindow
				+ TCPSimulator.MSS / 8;
			// Notice this last term: + MSS/8
			// TODO Stevens's book on TCP mentions this
			// additional term in congestion window equation: + MSS/8
			// Check whether this is actually required by the RFC-2581.

			// Since we received an ACK for a full segment...
			resetMonitoringVariables();

			return false;

		} else {	// duplicate ACK, report it:
			return true;
		}
	}

	/**
	 * Helper method, called on three or more duplicate ACKs.
	 * Tahoe sender doesn't care about the number of
	 * duplicate ACKs as long as it's at least three.
	 * Also, after this kinds of event, the sending mode in
	 * TCP Tahoe is always reset to <i>slow-start</i>.
	 */
	protected void onThreeDuplicateACKs() {
		if (dupACKsGlobal > 2) {
			// reduce the slow start threshold
			SSThresh = congWindow / 2;
			SSThresh = Math.max(SSThresh, 2*TCPSimulator.MSS);

			// congestion window = 1 x MSS:
			congWindow = TCPSimulator.MSS;
			// retransmit the oldest packet

			// Reset the sending mode to "slow start" (TCP Tahoe).
			sendMode = SLOW_START;

			// Re-start the timer, for the outstanding segments.
			// Reset also the global counter of duplicate ACKs.
			resetMonitoringVariables();
		}
	}

	/**
	 * Helper method, called on the expired timeout timer.
	 * Also, after this kinds of event, the sending mode in
	 * any TCP is always reset to <i>slow-start</i>.
	 */
	protected void onExpiredTimeoutTimer() {
		// TODO NOTE: This perhaps should check "<=" and consider
		// the timer expired when equal to zero.
		// Please check the IETF RFC for details and implement
		// this correctly.
		if (timer < 0) {

			// reduce the slow start threshold
			SSThresh = congWindow / 2;
			SSThresh = Math.max(SSThresh, 2*TCPSimulator.MSS); 			

			// congestion window = 1 x MSS
			congWindow = TCPSimulator.MSS;

			// Reset the sending mode to "slow start".
			sendMode = SLOW_START;

			// Re-start the timer, for the outstanding segments.
			resetMonitoringVariables();
		}
	}

	/**
	 * Helper method to reset the variables monitoring the
	 * signs for packet loss.  The packet loss is detected
	 * by observing one or both of these events:
	 * <ul>
	 * <li> Three (or more) duplicate acknowledgements,
	 * monitored by the variable {@link int TCPSender#dupACKsGlobal}; </li>
	 * <li> Timeout timer expiration, monitored by the
	 * variable {@link int TCPSender#timer}. </li>
	 * </ul>
	 */
	protected void resetMonitoringVariables() {
		// Reset the global counter of duplicate ACKs.
		dupACKsGlobal = 0;

		// Re-start the timer, for the outstanding segments.
		timer = TIMER_DEFAULT;
	}

	/**
	 * TCP Tahoe implementation of the <code>send()</code> method.
	 * 
	 * @param segments_	The array of segments, to be filled in this method.
	 * @param rcvWindow_ The size of the currently available space in the receiver's buffer.
	 * 
	 * @see ch4.tcp.TCPSender#send(ch4.tcp.TCPSegment[], int, boolean)
	 */
	public void send(TCPSegment[] segments_, int rcvWindow_, boolean lostPacket_) {
		// Initialize the segment array before filling it with
		// the segments to be transmitted.
		for (int i_ = 0; i_ < segments_.length; i_++) {
			segments_[i_] = null;
		}

		// Calculate the sending parameters.
		int flightSize_ = lastByteSent - lastByteAcked;
		int effectiveWindow_ =
			Math.min(congWindow, rcvWindow_) - flightSize_;

		// Enforce the constraint of the minimum size of the effective window.
		if (effectiveWindow_ <= 0) {
			effectiveWindow_ = 1;
		}
		// Print the relevant parameters for congestion control.
		System.out.println(
        	congWindow + "\t\t" + effectiveWindow_ +
        	"\t\t" + flightSize_ + "\t\t" + SSThresh
       	);

		// If a segment was lost,
		// i.e., there was a timeout or 3 x dupACKs:
		if (lostPacket_) {
			// Re-send the (presumably) lost segment.
			// Recall that in this case the Tahoe sender, which
			// sends only one segment when a loss is detected!
			segments_[0] =
				new TCPSegment(lastByteAcked + 1, TCPSimulator.MSS);

			return;
		}

		// Send only whole MSS segments,
		// i.e., the Nagle algorithm is not employed here.
		int burst_size_ = effectiveWindow_ / TCPSimulator.MSS;

		if (burst_size_ > 0) {
			// Send the "burst_size_" worth of segments:
			for (int seg_ = 0; seg_ < burst_size_; seg_++) {
				segments_[seg_] = new TCPSegment(
					lastByteSent + 1, TCPSimulator.MSS
				);
				lastByteSent += segments_[seg_].length;
			}

		} else {
			// Send a single 1-byte segment to keep connection alive.
			segments_[0] = new TCPSegment(lastByteSent + 1, 1);
			lastByteSent += segments_[0].length;
		}
	}
}
