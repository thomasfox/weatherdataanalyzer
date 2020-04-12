package com.github.thomasfox.weatherdataanalyzer.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.linear.DiagonalMatrix;
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

  private static final long AVERAGE_INTERVAL_MILLIS = 30L * 60L * 1000L;

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

    double[] correlationOutput = calculateSpeedCorrelations(dataIntervals);
    double[] fitParameters = calculateFitParameters(correlationOutput);
    double[] fitResult = createFittedValues(fitParameters, correlationOutput.length);
    double characteristicTime = new ExponentialFittingFunction().calculateCharacteristicTime(fitParameters);

    double[] linearValues1 = createLinearValues(
        fitParameters[0],
        fitParameters[1],
        correlationOutput.length);
    double[] linearValues2 = createLinearValues(
        fitParameters[0] + fitParameters[3],
        fitParameters[1] + fitParameters[2] * fitParameters[3],
        (int) characteristicTime);

    XYDataset dataset = getResultDataset(correlationOutput, fitResult, linearValues1, linearValues2);
    JFreeChart chart = createChartFromData(dataset, "wind speed correlations", "time [s]", "Correlation");
    return chartService.createReponseEntityFromChart(chart);
  }

  @RequestMapping(value = "/wind/speed/autocorrelation/scan/direction", produces="image/png")
  public ResponseEntity<byte[]> displaySpeedAutocorrelationScanDirection(
          @RequestParam("from") String fromString,
          @RequestParam("to") String toString,
          @RequestParam(value = "speedFrom", required = false) Double speedFrom,
          @RequestParam(value = "speedTo", required = false) Double speedTo)
  {
    Date from = dateTimeService.parse(fromString);
    Date to = dateTimeService.parse(toString);
    List<TimeRangeWithData> dataIntervals;
    double[] longtermSlopes = new double[18];
    double[] longtermOffsets = new double[18];
    double[] characteristicTimes = new double[18];
    ExponentialFittingFunction fittingFunction = new ExponentialFittingFunction();
    for (int i = 0; i < 18; i++)
    {
      double direction = i * 20;
      dataIntervals = windDataService.getWithSpeedAndDirectionIn(
          from,
          to,
          speedFrom,
          speedTo,
          direction,
          direction + 20,
          AVERAGE_INTERVAL_MILLIS,
          Wind::getSpeedTimeData);

      double[] correlationOutput = calculateSpeedCorrelations(dataIntervals);
      if (correlationOutput[0] != 0d)
      {
        double[] fitParameters = calculateFitParameters(correlationOutput);
        longtermOffsets[i] = fitParameters[0] * 100;
        longtermSlopes[i] = -fitParameters[1] * 100000;
        characteristicTimes[i] = fittingFunction.calculateCharacteristicTime(fitParameters);
      }
    }

    final DefaultXYDataset dataset = new DefaultXYDataset();
    addToDataset(dataset, longtermOffsets, longtermSlopes, characteristicTimes);

    JFreeChart chart = createChartFromData(dataset, "parameters", "direction", "value");
    return chartService.createReponseEntityFromChart(chart);
  }

  private double[] calculateSpeedCorrelations(List<TimeRangeWithData> dataIntervals)
  {
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
    return correlationOutput;
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

    double[] correlationOutput = calculateDirectionCorrelation(dataIntervals);
    double[] fitParameters = calculateFitParameters(correlationOutput);
    double[] fitResult = createFittedValues(fitParameters, correlationOutput.length);
    double[] linearValues1 = createLinearValues(fitParameters[0], fitParameters[1], correlationOutput.length);
    double[] linearValues2 = createLinearValues(1d, fitParameters[1] + fitParameters[2] * fitParameters[3], 50);

    XYDataset dataset = getResultDataset(correlationOutput, fitResult, linearValues1, linearValues2);
    JFreeChart chart = createChartFromData(dataset, "wind speed correlations", "time [s]", "Correlation");
    return chartService.createReponseEntityFromChart(chart);
  }

  private double[] calculateDirectionCorrelation(
      List<TimeRangeWithData> dataIntervals)
  {
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
    return correlationOutput;
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

  XYDataset getResultDataset(double[] rawResult, double[]... fittedResults)
  {
    double[][] rawResultdatasetData = chartService.convertToDatasetData(rawResult);
    rawResultdatasetData = averageService.averageChartDataLogarithmically(rawResultdatasetData, 1.05d, 5);
    final DefaultXYDataset dataset = new DefaultXYDataset();
    dataset.addSeries("timeCorrelation", rawResultdatasetData);

    addToDataset(dataset, fittedResults);
    return dataset;
  }

  private void addToDataset(final DefaultXYDataset dataset, double[]... valuesCollection)
  {
    int i = 0;
    for (double[] values : valuesCollection)
    {
      double[][] fittedResultdatasetData = chartService.convertToDatasetData(values);
      dataset.addSeries("dataset " + i, fittedResultdatasetData);
      i++;
    }
  }

  private JFreeChart createChartFromData(
      XYDataset dataset,
      String title,
      String xAxisTitle,
      String yAxisTitle)
  {
    NumberAxis yAxis = new NumberAxis(yAxisTitle);
    yAxis.setAutoRangeStickyZero(false);
    yAxis.setAutoRangeIncludesZero(false);
    XYPlot plot = new XYPlot(
        dataset,
        new NumberAxis(xAxisTitle),
        yAxis,
        new XYLineAndShapeRenderer(true, false));
    JFreeChart lineChart = new JFreeChart(
        title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
    return lineChart;
  }

  private double[] calculateFitParameters(double[] correlationOutput)
  {
    Collection<WeightedObservedPoint> observations = new ArrayList<>();
    for (int i = 0; i < correlationOutput.length; ++i)
    {
      observations.add(new WeightedObservedPoint(1d, i, correlationOutput[i]));
    }
    CurveFitter curveFitter = new CurveFitter();
    double[] parameters = curveFitter.fit(observations);
    return parameters;
  }

  private double[] createFittedValues(double[] fitParameters, int datasetLength)
  {
    double[] fittedValues = new double[datasetLength];
    ExponentialFittingFunction fittingFunction = new ExponentialFittingFunction();
    for (int i = 0; i < datasetLength; ++i)
    {
      fittedValues[i] = fittingFunction.value(i, fitParameters);
    }
    return fittedValues;
  }

  private double[] createLinearValues(double offset, double slope, int datasetLength)
  {
    double[] result = new double[datasetLength];
    for (int i = 0; i < datasetLength; ++i)
    {
      result[i] = offset + i * slope;
    }
    return result;
  }

  static class CurveFitter extends AbstractCurveFitter
  {
    @Override
    protected LeastSquaresProblem getProblem(Collection<WeightedObservedPoint> observations)
    {

      final int len = observations.size();
      final double[] target = new double[len];
      final double[] weights = new double[len];

      int i = 0;
      for (WeightedObservedPoint obs : observations)
      {
        target[i] = obs.getY();
        weights[i] = obs.getWeight();
        ++i;
      }

      final AbstractCurveFitter.TheoreticalValuesFunction model
          = new AbstractCurveFitter.TheoreticalValuesFunction(new ExponentialFittingFunction(), observations);

      final double[] startPoint = new double[] { 0.8d, 0d , -0.05d, 0.5d};

      return new LeastSquaresBuilder()
          .maxEvaluations(Integer.MAX_VALUE)
          .maxIterations(1000)
          .start(startPoint)
          .target(target)
          .weight(new DiagonalMatrix(weights))
          .model(model.getModelFunction(), model.getModelFunctionJacobian())
          .build();
    }
  }

  static class ExponentialFittingFunction implements ParametricUnivariateFunction
  {
    @Override
    public double[] gradient(double x, double... parameters)
    {
      return new double[] { 1, x , parameters[3] * x * Math.exp(x * parameters[2]), Math.exp(x * parameters[2])};
    }

    @Override
    public double value(double x, double... parameters)
    {
      return parameters[0] + parameters[1] * x + parameters[3] * Math.exp(x * parameters[2]);
    }

    public double calculateCharacteristicTime(double... parameters)
    {
      double longtermOffset = parameters[0];
      double longtermSlope = parameters[1];
      double shorttermSlope = parameters[1] + parameters[2] * parameters[3];
      double shorttermOffset = parameters[0] + parameters[3];
      double characteristicTime = (shorttermOffset - longtermOffset) / (longtermSlope - shorttermSlope);
      return characteristicTime;
    }
  }
}
