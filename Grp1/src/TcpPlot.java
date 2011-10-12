

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
/** * A simple introduction to using JFreeChart.This demo is described in the * JFreeChart Developer Guide. */ 
public class TcpPlot 
{ 
	/** * The starting point for the demo. ** @param argsignored. */ 

	XYSeries congWindow;
	XYSeries eff;
	XYSeries ssthresh;
	XYSeries flight;
	public TcpPlot()
	{
	congWindow = new XYSeries("congWindow");
	eff = new XYSeries("eff");
	ssthresh = new XYSeries("ssthresh");
	flight = new XYSeries("flight");
	}


	public void plotTcp(int iter, int congWindowSize, int effWindow, int flightSize, int ssThreshold) 
	{ // create a dataset...

		congWindow.add(iter, congWindowSize);
        ssthresh.add(iter, ssThreshold);
        flight.add(iter, flightSize);
        eff.add(iter, effWindow);

	}

        
public void plotGraph()
{
        //         Add the series to your data set
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(congWindow);
        dataset.addSeries(ssthresh);
        dataset.addSeries(flight);
        dataset.addSeries(eff);



        //         Generate the graph
        JFreeChart chart = ChartFactory.createXYLineChart("TCP RENO GRP1", // Title
                "x-axis", // x-axis Label
                "y-axis", // y-axis Label
                dataset, // Dataset
                PlotOrientation.VERTICAL, // Plot Orientation
                true, // Show Legend
                true, // Use tooltips
                false // Configure chart to generate URLs?
            );
		
		// create and display a frame...
		ChartFrame frame = new ChartFrame("First", chart); 
		frame.pack();
		frame.setVisible(true); 
		} 
	}

