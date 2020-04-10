package com.github.thomasfox.weatherdataanalyzer.controller;

import java.util.Date;
import java.util.List;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.thomasfox.weatherdataanalyzer.repository.model.Wind;
import com.github.thomasfox.weatherdataanalyzer.service.AverageService;
import com.github.thomasfox.weatherdataanalyzer.service.ChartService;
import com.github.thomasfox.weatherdataanalyzer.service.DateTimeService;
import com.github.thomasfox.weatherdataanalyzer.service.WindDataService;
import com.github.thomasfox.weatherdataanalyzer.service.model.TimeRangeWithData;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class AutocorrelationController
{
  private static final int CORRELATION_LENGTH = 300;

  private static final long AVERAGE_INTERVAL_MILLIS = 15L * 60L * 1000L;

  private final WindDataService windDataService;

  private final AverageService averageService;

  private final DateTimeService dateTimeService;

  private final ChartService chartService;

  @RequestMapping(value = "/wind/speed/autocorrelation", produces="image/png")
  public ResponseEntity<byte[]> displaySpeedAutocorrelation(
          @RequestParam("from") String fromString,
          @RequestParam("to") String toString,
          @RequestParam(value = "speedFrom", required = false) Double speedFrom,
          @RequestParam(value = "speedTo", required = false) Double speedTo,
          @RequestParam(value = "directionFrom", required = false) Double directionFrom,
          @RequestParam(value = "directionTo", required = false) Double directionTo)
  {
    Date from = dateTimeService.parse(fromString);
    Date to = dateTimeService.parse(toString);
    List<TimeRangeWithData> dataIntervals;
    if (speedFrom != null || speedTo != null || directionFrom != null || directionTo != null)
    {
      dataIntervals = windDataService.getWithSpeedAndDirectionIn(
          from,
          to,
          speedFrom,
          speedTo,
          directionFrom,
          directionTo,
          AVERAGE_INTERVAL_MILLIS,
          Wind::getSpeedTimeData);
    }
    else
    {
      dataIntervals = windDataService.getDataForTimeRangeInList(from, to, Wind::getSpeedTimeData);
    }

    double[] correlationOutput = createArrayFilledWithZeros();

    for (TimeRangeWithData dataInterval : dataIntervals)
    {
      double[] correlationInput = windDataService.fillArrayWithDataForEachSecond(
          dataInterval,
          (int) (dataInterval.getRange().getDurationMillis() / 1000) + 1);

      for (int timeDifference = 0; timeDifference < CORRELATION_LENGTH; timeDifference++)
      {
        double corelationForTimeDifference = 0d;
        for (int i = 0; i < correlationInput.length - timeDifference; i++)
        {
          double value = correlationInput[i];
          double otherValue = correlationInput[i + timeDifference];
          double maxValue = Math.max(value, otherValue);
          if (maxValue > 0d)
          {
            double correlation
                = value * otherValue
                    / (maxValue * maxValue * (correlationInput.length - timeDifference) * dataIntervals.size());
            corelationForTimeDifference += correlation;
          }
          else
          {
            corelationForTimeDifference += 1d / ((correlationInput.length - timeDifference) * dataIntervals.size());
          }
        }
        correlationOutput[timeDifference] += corelationForTimeDifference;
      }
    }
    XYDataset dataset = getResultDataset(correlationOutput);
    JFreeChart chart = createChartFromData(dataset, "wind speed correlations");
    return chartService.createReponseEntityFromChart(chart);
  }

  @RequestMapping(value = "/wind/direction/autocorrelation", produces="image/png")
  public ResponseEntity<byte[]> displayDirectionAutocorrelation(
          @RequestParam("from") String fromString,
          @RequestParam("to") String toString,
          @RequestParam(value = "speedFrom", required = false) Double speedFrom,
          @RequestParam(value = "speedTo", required = false) Double speedTo,
          @RequestParam(value = "directionFrom", required = false) Double directionFrom,
          @RequestParam(value = "directionTo", required = false) Double directionTo)
  {
    Date from = dateTimeService.parse(fromString);
    Date to = dateTimeService.parse(toString);
    List<TimeRangeWithData> dataIntervals;
    if (speedFrom != null || speedTo != null || directionFrom != null || directionTo != null)
    {
      dataIntervals = windDataService.getWithSpeedAndDirectionIn(
          from,
          to,
          speedFrom,
          speedTo,
          directionFrom,
          directionTo,
          AVERAGE_INTERVAL_MILLIS,
          Wind::getDirectionTimeData);
    }
    else
    {
      dataIntervals = windDataService.getDataForTimeRangeInList(from, to, Wind::getDirectionTimeData);
    }

    double[] correlationOutput = createArrayFilledWithZeros();

    for (TimeRangeWithData dataInterval : dataIntervals)
    {
      double[] correlationInput = windDataService.fillArrayWithDataForEachSecond(
          dataInterval,
          (int) (dataInterval.getRange().getDurationMillis() / 1000) + 1);

      for (int timeDifference = 0; timeDifference < CORRELATION_LENGTH; timeDifference++)
      {
        double corelationForTimeDifference = 0d;
        for (int i = 0; i < correlationInput.length - timeDifference; i++)
        {
          double value = correlationInput[i];
          double otherValue = correlationInput[i + timeDifference];
          double difference = Math.abs(value - otherValue);
          if (difference > 180)
          {
            difference = 360 - difference;
          }
          double correlation = (90d - difference)
              / (90d * (correlationInput.length - timeDifference) * dataIntervals.size());
          corelationForTimeDifference += correlation;
        }
        correlationOutput[timeDifference] += corelationForTimeDifference;
      }
    }
    XYDataset dataset = getResultDataset(correlationOutput);
    JFreeChart chart = createChartFromData(dataset, "wind direction correlations");
    return chartService.createReponseEntityFromChart(chart);
  }


  private double[] createArrayFilledWithZeros()
  {
    double[] correlationOutput = new double[CORRELATION_LENGTH];
    for (int i = 0; i < CORRELATION_LENGTH; i++)
    {
      correlationOutput[i] = 0;
    }
    return correlationOutput;
  }

  XYDataset getResultDataset(double[] rawResult)
  {
    double[][] datasetData = new double[2][rawResult.length];
    for (int i = 0; i < rawResult.length; i++)
    {
      datasetData[0][i] = i;
    }
    datasetData[1] = rawResult;
    datasetData = averageService.averageChartDataLogarithmically(datasetData, 1.05d, 5);
    final DefaultXYDataset dataset = new DefaultXYDataset();
    dataset.addSeries("timeCorrelation", datasetData);
    return dataset;
  }

  private JFreeChart createChartFromData(
      XYDataset dataset,
      String title)
  {
    XYPlot plot = new XYPlot(
        dataset,
        new NumberAxis("time [s]"),
        new NumberAxis("Correlation"),
        new XYLineAndShapeRenderer(true, false));
    JFreeChart lineChart = new JFreeChart(
        title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
    return lineChart;
  }
}
