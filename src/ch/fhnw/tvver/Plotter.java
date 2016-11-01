package ch.fhnw.tvver;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.ILineSeries;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.Range;

public class Plotter {

	private Display display;
	private Shell shell;
	private String title;
	private int width;
	private int height;

	private Chart frequencyChart;

	public Plotter(final String title, final int width, final int height) {
		this.title = title;
		this.width = width;
		this.height = height;
	}

	public void plot() {
		this.display = Display.getDefault();
		this.shell = new Shell(display);
		shell.setText(this.title);
		shell.setSize(this.width, this.height);
		shell.setLayout(new FillLayout());

		initFrequencyChart(shell);

		shell.open();
	}

	private void initFrequencyChart(Shell shell) {
		// create a chart
		this.frequencyChart = new Chart(shell, SWT.NONE);

		// set titles
		frequencyChart.getTitle().setText("Frequency");
		IAxis xAxis = frequencyChart.getAxisSet().getXAxis(0);
		xAxis.setRange(new Range(0, 100));
		xAxis.getTitle().setText("Frequency");
		IAxis yAxis = frequencyChart.getAxisSet().getYAxis(0);
		yAxis.setRange(new Range(0, 1024));
		yAxis.getTitle().setText("Amplitude");
		frequencyChart.getSeriesSet().createSeries(SeriesType.LINE, "frequency");

	}

	public void update(float[] data) {
		double[] convertedData = new double[data.length];
		for (int index = 0; index < data.length; index++) {
			convertedData[index] = data[index];
		}
		ILineSeries lineSeries = (ILineSeries) frequencyChart.getSeriesSet().getSeries("frequency");
		Display.getDefault().asyncExec(() -> {
			if(!shell.isDisposed()){
				lineSeries.setYSeries(convertedData);
				frequencyChart.getAxisSet().adjustRange();
			}
		});
		
		
	}

}
