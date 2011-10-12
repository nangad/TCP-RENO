/*
 * Created on Sep 10, 2005
 *
 * Rutgers University, Department of Electrical and Computer Engineering
 * <P> Copyright (c) 2005 Rutgers University
 */

/**
 * This class implements a simple TCP receiver.
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
public class TCPReceiver {
	/** The receiver buffer to buffer the segments that arrive
	 * out-of-sequence. */
	protected TCPSegment[] rcvBuffer =
		new TCPSegment[50000]; //changed to the buffer size to be able to have more iterations

	/** The field records the last byte received in-sequence.
	 * Recall that the bytes are numbered from zero, so the sequence
	 * number of the first byte is zero, etc. */
	protected int lastByteRecvd = -1;

	/** The next byte currently expected from the sender.
	 * Recall that the bytes are numbered from zero, so the sequence
	 * number of the first byte is zero, etc. */
	protected int nextByteExpected = 0;

	/** Receive window size, in bytes. */
	protected int rcvWindow = 65536;

	/** Index of the last segment that has been buffered.
	 * <code>-1</code> means: <i>There are no buffered segments</i>.
	 * Keep in mind that because this is an array index, the
	 * zero value indexes the first element of the array. */
	protected int lastBufferedIdx = -1;

	/**
	 * Constructor.
	 */
	public TCPReceiver() {
		// Initialize the receiver's buffer with "null", which symbolizes "no packet"
		for (int i_ = 0; i_ < rcvBuffer.length; i_++) {
			rcvBuffer[i_] = null;
		}
		lastBufferedIdx = -1;	// No segments are buffered initially.
	}

	/**
	 * Accessor for retrieving the current size of the
	 * available buffer space, in bytes.
	 * 
	 * @return Returns current size of the available buffer space, in bytes.
	 */
	public int getRcvWindow() {
		return rcvWindow;
	}

	/**
	 * Receives the segments from the sender, passes the
	 * ones that arrived in-order to the application.
	 * Buffers the ones that arrived out-of-sequence.
	 * The receiver returns <i>cumulative</i> acknowledgements,
	 * which means that if the newly received segment fills the
	 * gap created by out-of-sequence segments that were received
	 * earlier, the cumulative ACK will acknowledge those earlier
	 * segments, as well.
	 * <P>
	 * The value <code>null</code> of the <code>segments_</code> input
	 * array element means that the corresponding segment was
	 * <i>lost</i> in transport (i.e., at the Router).
	 * 
	 * @param segments_ The received segments array.
	 * @param acks_ The acknowledgements array, to be formed in this method and returned.
	 * @return Returns the current size of the receive window, {@link int #rcvWindow}.
	 * @see Router
	 */
	public int receive(TCPSegment[] segments_, TCPSegment[] acks_) {
		// Initialize the acknowledgements array with "null". 
		for (int i_ = 0; i_ < acks_.length; i_++) {
			acks_[i_] = null;
		}
		// Notice that ACKs must be entered contiguously,
		// with no gaps, which means that there must be no "null"
		// between the valid sequence numbers of the segments that
		// are being acknowledged.

		for (int i_ = 0; i_ < segments_.length; i_++) {
			// "null" means: "lost segment" -- skip it but keep
			// going, because this gap may be due to loss-in-transport
			if (segments_[i_] != null) {

				// Check if the segment arrived in-sequence.
				// Recall that we're expecting the segment with
				// sequence number equal "nextByteExpected"
				if (segments_[i_].seqNum == nextByteExpected) {

					// Set the expected seq. num. to the next segment.
					nextByteExpected =
						segments_[i_].seqNum + segments_[i_].length;

					// Check is there were any out-of-sequence segments
					// previously buffered:
					if (lastBufferedIdx == -1) {
						// No previously buffered segments.
						// Make record of the last byte received in-sequence.
						lastByteRecvd =
							segments_[i_].seqNum + segments_[i_].length - 1;

					} else {
						// Some segments were previously buffered.
						// Checked whether this segment filled any gaps for
						// the possible buffered segments.  If yes,
						// this will update "lastByteRecvd"
						checkBufferedSegments();
					}

					// Acknowledge the received segment.
					// NOTE: This is a _cumulative_ acknowledgement,
					// in that it possibly acknowledges some segments which
					// were earlier received and buffered, but now the gap
					// was filled.
					acks_[i_] = new TCPSegment(
						nextByteExpected, 1, true
						// ACK segment length is irrelevant, set as "1"
					);
				} //ends IF condition: segments_[i_].seqNum == nextByteExpected

    			else {	
    				// Out-of-sequence segment, buffer it.
    				acks_[i_] = outOfSequenceSegment(segments_[i_]);
    				// This must be a duplicate ACK !!!
    			}
			} //ends this IF conditon: (segments[i] != null)
			// We don't do anything for lost segments, they are just
			// silently skipped, since the ACKs array is already
			// initialized with "null" for all.
		} //ends the 2nd for() loop

		return rcvWindow;
	}

	/**
	 * Helper method to handle the out-of-sequence segments.
	 * Such segments are buffered in the {@link int[] #rcvBuffer}.
	 * The returned value will be a <i>duplicate acknowledgement</i>.
	 * 
	 * @param segment_ The segment that is currently being processed (i.e., the seq. num. of the segment's last byte).
	 * @return Returns the acknowledgement segment for the input data segment.
	 */
	protected TCPSegment outOfSequenceSegment(TCPSegment segment_) {
		// Bufer the out-of-sequence segment.
		// Notice that we assume implicitly that all currently
		// buffered segments have lower sequence number than the one
		// that is presently being buffered.
		// Also, we assume that all buffered segments are ordered in
		// the ascending order of their sequence number.
		lastBufferedIdx++;
		rcvBuffer[lastBufferedIdx] = segment_;
		lastByteRecvd = segment_.seqNum + segment_.length - 1;

		// Because we just buffered one segment, we need to
		// reduce the size of the receive window by the
		// segment's length.
		rcvWindow -= segment_.length;

		int ackSeqNum_ = nextByteExpected;	// duplicate ACK !!!
		return new TCPSegment(ackSeqNum_, 1, true);
	}

	/**
	 * Helper method, checks if the newly received segment(s)
	 * fill the gaps for the segments that were previously
	 * received out-of-sequence and are stored in a temporary
	 * storage ("buffered").
	 * These segments are waiting for the gap to be filled.
	 * Once an arriving segment fills the gap, the first buffered
	 * segment will become "next expected segment".
	 * That is the condition for which this method checks.
	 * If what is currently the "next expected segment" is one of
	 * the buffered segments that have been received earlier,
	 * picks that segments up now and removes it from the receive
	 * buffer.
	 */
	protected void checkBufferedSegments() {

		// Check all the buffered segments, if any.
		// Recall that "lastBuffered" is the array index, so
		// zero value indexes the first element of the array.
		while (lastBufferedIdx >= 0) {

			// Check if the previously buffered out-of-sequence segment
			// is presently in-sequence, so can be removed from the
			// buffer:
			if (rcvBuffer[0].seqNum == nextByteExpected) {

				// Remove the segment from the buffer:
				nextByteExpected =
					rcvBuffer[0].seqNum + rcvBuffer[0].length;

				// Because we removed one segment from the buffer, we need
				// to _reclaim_ the freed buffer space, and increase the
				// receive window size by the removed segment's length.
				rcvWindow += rcvBuffer[0].length;

				// Perform the segment's removel.
				rcvBuffer[0] = null;
				lastBufferedIdx--;

				// Quit if there are no more buffered segments:
				if (lastBufferedIdx == -1) { 
					break;
				}

				// In case there is more than one segment in the receive
				// buffer, shift the remaining segments towards
				// the beginning of the array to compensate for
				// the just removed segment.
				TCPSegment[] temp_ = new TCPSegment[lastBufferedIdx + 1];
				System.arraycopy(
					rcvBuffer, 1, temp_, 0, lastBufferedIdx + 1
				);
				System.arraycopy(temp_, 0, rcvBuffer, 0, lastBufferedIdx + 1);

				// Erase the previous last buffered segment
				// because it's now shifted to the lower index
				// and now this is a _duplicate_.
				rcvBuffer[lastBufferedIdx + 1] = null;

			} else {
				// Quit because the remaining buffered segments
				// are all out-of-order.
				break;
			}
		}
	}
}
