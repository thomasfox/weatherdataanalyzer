package com.github.thomasfox.weatherdataanalyzer.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.thomasfox.weatherdataanalyzer.service.AverageService;
import com.github.thomasfox.weatherdataanalyzer.service.ChartService;
import com.github.thomasfox.weatherdataanalyzer.service.DateTimeService;
import com.github.thomasfox.weatherdataanalyzer.service.WindDataService;
import com.github.thomasfox.weatherdataanalyzer.service.model.TimeData;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class FftController
{
  private static final Long AVERAGE_INTERVAL_MILLIS = 60L * 60L * 1000L;

  private final WindDataService windDataService;

  private final AverageService averageService;

  private final DateTimeService dateTimeService;

  private final ChartService chartService;

  @RequestMapping(value = "/wind/speed/fft", produces="image/png")
  public ResponseEntity<byte[]> display(
          @RequestParam("from") String fromString,
          @RequestParam("to") String toString,
          @RequestParam(value = "speedFrom", required = false) Double speedFrom,
          @RequestParam(value = "speedTo", required = false) Double speedTo,
          @RequestParam(value = "directionFrom", required = false) Double directionFrom,
          @RequestParam(value = "directionTo", required = false) Double directionTo)
  {
    Date from = dateTimeService.parse(fromString);
    Date to = dateTimeService.parse(toString);
    List<List<TimeData>> speedPoints;
    if (speedFrom != null || speedTo != null || directionFrom != null || directionTo != null)
    {
      speedPoints = getPointList(from, to, speedFrom, speedTo, directionFrom, directionTo);
    }
    else
    {
      speedPoints = getPointList(from, to);
    }
    List<Double> frequencyAmplitudes = new ArrayList<>();
    Integer amplitudesSize = null;
    for (List<TimeData> singleIntervalData : speedPoints)
    {
      List<Double> intervalResult = getFrequencyAmplitudes(singleIntervalData);
      if (amplitudesSize == null)
      {
        amplitudesSize = intervalResult.size();
      }
      else if (amplitudesSize != intervalResult.size())
      {
        throw new IllegalStateException("fft result with size " + intervalResult.size()
            + " cannot be added to result with size " + amplitudesSize);
      }
      int i = 0;
      for (Double amplitude : intervalResult)
      {
        if (frequencyAmplitudes.size() <= i)
        {
          frequencyAmplitudes.add(amplitude);
        }
        else
        {
          frequencyAmplitudes.set(i, frequencyAmplitudes.get(i) + amplitude);
        }
        i++;
      }
    }
    XYDataset dataset = getFFtResultDataset(frequencyAmplitudes);
    Double frequencyFrom = null;
    Double frequencyTo = null;
    if (speedFrom != null && speedTo != null)
    {
      frequencyFrom = 10000d/AVERAGE_INTERVAL_MILLIS;
      frequencyTo = 0.1d;
    }
    JFreeChart lineChart = createChartFromData(dataset, frequencyFrom, frequencyTo);
    return chartService.createReponseEntityFromChart(lineChart);
  }

  private JFreeChart createChartFromData(XYDataset dataset, Double frequencyFrom, Double frequencyTo)
  {
    LogarithmicAxis xAxis = new LogarithmicAxis("Frequency");
    if (frequencyFrom != null || frequencyTo != null)
    {
      if (frequencyFrom == null)
      {
        frequencyFrom = dataset.getXValue(0, 0);
      }
      if (frequencyTo == null)
      {
        frequencyTo = dataset.getXValue(0,dataset.getItemCount(0) - 1);
      }
      xAxis.setRange(new Range(frequencyFrom, frequencyTo));
    }
    LogarithmicAxis yAxis = new LogarithmicAxis("Amplitude");
    XYPlot plot = new XYPlot(
        dataset,
        xAxis,
        yAxis,
        new XYLineAndShapeRenderer(true, false));
    JFreeChart lineChart = new JFreeChart(
        "Wind speed FFT", JFreeChart.DEFAULT_TITLE_FONT, plot, false);
    return lineChart;
  }

  public List<List<TimeData>> getPointList(Date from, Date to)
  {
    List<List<TimeData>> result = new ArrayList<>();
    result.add(windDataService.getSpeedPoints(from, to));
    return result;
  }

  public List<List<TimeData>> getPointList(
      Date from,
      Date to,
      Double speedFrom,
      Double speedTo,
      Double directionFrom,
      Double directionTo)
  {
    if (speedFrom == null)
    {
      speedFrom = 0d;
    }
    if (speedTo == null)
    {
      speedTo = 1000d;
    }
    if (directionFrom == null)
    {
      directionFrom = 0d;
    }
    if (directionTo == null)
    {
      directionTo = 360d;
    }
    return windDataService.getSpeedPointIntervalsWithSpeedIn(
        from,
        to,
        speedFrom,
        speedTo,
        directionFrom,
        directionTo,
        AVERAGE_INTERVAL_MILLIS);
  }

  private List<Double> getFrequencyAmplitudes(List<TimeData> speedPoints)
  {
    double[] fftInput = createArrayForFftWithDataForEachSecond(speedPoints);
    FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
    Complex[] fftResult = transformer.transform(fftInput, TransformType.FORWARD);
    List<Double> amplitudes = new ArrayList<>();
    for (int i = 0; i < fftInput.length; ++i)
    {
      amplitudes.add(fftResult[i].abs());
    }
    return amplitudes;
  }

  XYDataset getFFtResultDataset(List<Double> rawFftResult)
  {
    int displayedSize = rawFftResult.size() / 2;
    double[][] datasetData = new double[2][displayedSize];
    for (int i = 0; i < displayedSize; i++)
    {
      datasetData[0][i] = ((double) i) / rawFftResult.size() ;
      datasetData[1][i] = rawFftResult.get(i);
    }
    datasetData = averageService.averageChartDataLogarithmically(datasetData, 1.05d, 5);
    final DefaultXYDataset dataset = new DefaultXYDataset();
    dataset.addSeries("Wind speed FFT", datasetData);
    return dataset;
 }

  private double[] createArrayForFftWithDataForEachSecond(List<TimeData> speedPoints)
  {
    long start = speedPoints.get(0).getTimestamp();
    long end = speedPoints.get(speedPoints.size() - 1).getTimestamp();
    int arraySize = calculateFftArraySize(start, end);
    double[] fftInput = fillFftInputArray(speedPoints, start, arraySize);
    return fftInput;
  }

  private int calculateFftArraySize(long start, long end)
  {
    int sizeInSeconds = (int) ((end - start )/ 1000l);

    // FFT array size must be a power of two
    int arrayExponent = (int) Math.ceil(Math.log(sizeInSeconds)/Math.log(2));
    int arraySize = 1;
    for (int i = 1; i <= arrayExponent; i++)
    {
      arraySize *= 2;
    }
    return arraySize;
  }

  private double[] fillFftInputArray(List<TimeData> speedPoints, long start, int arraySize)
  {
    double[] fftInput = new double[arraySize];
    int speedPointIndex = 0;
    for (int i = 0; i < arraySize; i++)
    {
      while (speedPointIndex < speedPoints.size() - 1
          && speedPoints.get(speedPointIndex + 1).getTimestamp() <= i * 1000 + start)
      {
        speedPointIndex++;
      }
      fftInput[i] = speedPoints.get(speedPointIndex).getValue();
    }
    return fftInput;
  }
}
