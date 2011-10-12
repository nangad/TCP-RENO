//import ch4.tcp.First;


public class TCPSenderReno extends TCPSender
{
    //private Lock timeoutStateLock = new Lock();
    //private boolean timeoutState = false;
    //private int timerValueMs = 3000;
    //private TCPTimeoutTimerTask timerTask = new TCPTimeoutTimerTask();
    //private Timer timer = null;

    public static final int FAST_RECOVERY = 2;
    //public boolean force = false; //vaithi
    public void TCPSenderReno()
    {
    }

    public void send(TCPSegment[] segments, int rcvWindow, int outcome, int iter, TcpPlot tcpPlot) 
    {
        for (int i = 0; i < segments.length; i++) 
            segments[i] = null;

        int flightSize = lastByteSent - lastByteAcked;
        
        int maxWin = Math.min(congWindow, rcvWindow); 
        int effecWin = maxWin - flightSize; 
        if (effecWin <= 0) 
        {
        	effecWin = 1;
        	
        }

        System.out.println(congWindow + "\t\t" + effecWin + "\t\t" + flightSize + "\t\t" + SSThresh);
        //System.out.println("congWindow = " + congWindow + ", rcvWindow = " + rcvWindow);

		tcpPlot.plotTcp(iter,congWindow,effecWin,flightSize,SSThresh);

        
        if (outcome == TCPSimulator.DUP_ACKx3)
        {
            // fast retransmit lost segment
            segments[0] = new TCPSegment(lastByteAcked + 1, TCPSimulator.MSS);
            sendMode = FAST_RECOVERY;
            if ((TCPSimulator.currentReportingLevel & TCPSimulator.REPORTING_LEVEL_1) != 0) 
            {
                System.out.println("############## Fast-retransmitted lost segment number " + ((lastByteAcked + 1)/TCPSimulator.MSS + 1));
                System.out.println("############## Sender entering fast recovery.");
            }
            return;
        }

        int burst_size = effecWin/TCPSimulator.MSS;
        if (burst_size > 0)
        {
            //System.out.println("sending " + burst_size + " segments.");
            for (int i = 0; i < effecWin/TCPSimulator.MSS; i++)
            {
                segments[i] = new TCPSegment(lastByteSent + 1, TCPSimulator.MSS); 
                //System.out.println("Length " +segments[i].length); //vaithi
                if(effecWin>0)lastByteSent += TCPSimulator.MSS;
            }
        }
        else
        {
            segments[0] = new TCPSegment(lastByteSent + 1, 1);
            //System.out.println("Length else " +segments[0].length); //vaithi
            lastByteSent += 1;
        }
    }

    public int processAcks(TCPSegment[] acks)
    {
        int retVal = TCPSimulator.SUCCESS;
        for (int i = 0; i < acks.length; i++)
        {
            if (acks[i] == null)
                continue;
            //System.out.println("processing ACK " + acks[i].seqNum);
            boolean dupACKLocal = false;
            if (sendMode == SLOW_START)
                dupACKLocal = processAcksSlowStart(acks[i]);
            else if (sendMode == CONG_AVOID)
                dupACKLocal = processAcksCongestionAvoidance(acks[i]);
            else    // sendMode == FAST_RECOVERY
                dupACKLocal = processAcksFastRecovery(acks[i]);
            dupACKsGlobal += dupACKLocal ? 1 : 0;
        }
        if (sendMode != FAST_RECOVERY && dupACKsGlobal > 2)
        {
            int flightSize = lastByteSent - lastByteAcked;
            //System.out.println("lastByteSent = " + lastByteSent + ", lastByteAcked = " + lastByteAcked);
            System.out.println("floor is " +Math.floor(flightSize/2));
            SSThresh = (int) Math.max(Math.floor(flightSize/2), 2 * TCPSimulator.MSS); //vaithi
            congWindow = SSThresh + dupACKsGlobal * TCPSimulator.MSS;      // "inflate" the window
            //System.out.println("inflated congWindow = " + congWindow + ", dupACKsGlobal = " + dupACKsGlobal);
            dupACKsGlobal = 0;
            retVal = TCPSimulator.DUP_ACKx3;
        }
        else if (sendMode == FAST_RECOVERY && dupACKsGlobal > 0)
        {
            congWindow += dupACKsGlobal * TCPSimulator.MSS;    // "inflate" window if additional dup ACKs 
            dupACKsGlobal = 0;
        }

        if (lastByteSent == lastByteAcked)
           resetMonitoringVariables(); 
       /* else
        {
            timer--;
            if (timer <= 0) 
            {
                int flightSize = lastByteSent - lastByteAcked;
                SSThresh = (int) Math.max(Math.floor(flightSize/2), 2 * TCPSimulator.MSS);
                congWindow = TCPSimulator.MSS;
                sendMode = SLOW_START;
                retVal = TCPSimulator.TIMEOUT;
                timer = TIMER_DEFAULT;
            }
        }*/
        return retVal;
    }

    protected boolean processAcksSlowStart(TCPSegment ack)
    {
        if (ack.seqNum > (lastByteAcked + 1))    // not a duplicate ACK
        {
            lastByteAcked = ack.seqNum - 1; 
            congWindow += TCPSimulator.MSS;
            if ((sendMode == SLOW_START) && (congWindow > SSThresh)) 
            {
                sendMode = CONG_AVOID;
                if ((TCPSimulator.currentReportingLevel & TCPSimulator.REPORTING_LEVEL_1) != 0) 
                {
                    System.out.println("############## Sender entering congestion avoidance.");
                }
            }
            resetMonitoringVariables();      // received non-duplicate ACK for higher byte #, so reset duplicate ACK counter
            return false;
        }
        else if (ack.seqNum == lastByteAcked + 1)   // ACK considered duplicate only if no other ACKs cumulatively cover this ACK's acknowledged segment
            return true;
        else    // ACK is duplicate, but another ACK cumulatively acknowledged the segment that this ACK covers, so not counted as duplicate
            return false;
    }

    protected boolean processAcksCongestionAvoidance(TCPSegment ack)
    {
        if (ack.seqNum > (lastByteAcked + 1))    // not a duplicate ACK
        {
            lastByteAcked = ack.seqNum - 1;
            congWindow += Math.ceil((TCPSimulator.MSS * TCPSimulator.MSS)) / congWindow; //vaithi
            resetMonitoringVariables();      // received non-duplicate ACK for higher byte #, so reset duplicate ACK counter
            return false;
        }
        else if (ack.seqNum == lastByteAcked + 1)   // ACK considered duplicate only if no other ACKs cumulatively cover this ACK's acknowledged segment
            return true;
        else    // ACK is duplicate, but another ACK cumulatively acknowledged the segment that this ACK covers, so not counted as duplicate
            return false;
    }

    protected boolean processAcksFastRecovery(TCPSegment ack)
    {
        if (ack.seqNum > (lastByteAcked + 1))    // not a duplicate ACK
        {
            lastByteAcked = ack.seqNum - 1;
            congWindow = SSThresh;     // "deflate" the window
            resetMonitoringVariables();      // received non-duplicate ACK for higher byte #, so reset duplicate ACK counter
            sendMode = SLOW_START; //original
            //sendMode = CONG_AVOID; //vaithi
            if ((TCPSimulator.currentReportingLevel & TCPSimulator.REPORTING_LEVEL_1) != 0) 
            {
                System.out.println("############## Sender entering slow start.");
            }
            return false;
        }
        else if (ack.seqNum == lastByteAcked + 1)   // ACK considered duplicate only if no other ACKs cumulatively cover this ACK's acknowledged segment
            return true;
        else    // ACK is duplicate, but another ACK cumulatively acknowledged the segment that this ACK covers, so not counted as duplicate
            return false;
    }

    protected void resetMonitoringVariables() 
    {
        if ((TCPSimulator.currentReportingLevel & TCPSimulator.REPORTING_LEVEL_1) != 0) 
        {
            System.out.println("############## Resetting dupACKsGlobal and timer.");
        }
        dupACKsGlobal = 0;
        timer = TIMER_DEFAULT;
    }

}
