package com.github.thomasfox.weatherdataanalyzer.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class AverageService
{
  public double[][] averageChartData(double[][] datasetData, int averageFraction)
  {
    List<Double> xValues = new ArrayList<>();

    List<Double> yValues = new ArrayList<>();
    double xSum = 0;
    double ySum = 0;
    double pointsInBucket = 0;
    for (int i = 0; i < datasetData[0].length; i++)
    {
      xSum += datasetData[0][i];
      ySum += datasetData[1][i];
      pointsInBucket++;
      if (pointsInBucket == averageFraction)
      {
        xValues.add(xSum / averageFraction);
        yValues.add(ySum / averageFraction);
        xSum = 0;
        ySum = 0;
        pointsInBucket = 0;
      }
    }
    if (pointsInBucket != 0)
    {
      xValues.add(xSum / pointsInBucket);
      yValues.add(ySum / pointsInBucket);
    }
    double[][] result = new double[2][xValues.size()];
    for (int i = 0; i < xValues.size(); i++)
    {
      result[0][i] = xValues.get(i);
      result[1][i] = yValues.get(i);
    }
    return result;
  }

  /**
   * Averages a data set for displaying it with a logarithmic x axis.
   * Each bucket is the factor <code>averageFactor</code> larger than the smaller bucket.
   *
   * @param datasetData the data to average. The x values must be increasing as the index increases.
   * @param averageFactor the size by which each bucket increases. Must be larger than 1.
   * @param minNumberOfPointsPerBucket minimum number of points per bucket.
   *        If it is not reached in a bucket, the bucket size is increased.
   *
   * @return the averaged data
   */
  public double[][] averageChartDataLogarithmically(
      double[][] datasetData,
      double averageFactor,
      int minNumberOfPointsPerBucket)
  {
    if (averageFactor <= 1)
    {
      throw new IllegalArgumentException("average factor must be larger than 1");
    }
    List<Double> xValues = new ArrayList<>();
    List<Double> yValues = new ArrayList<>();
    double xSum = 0;
    double ySum = 0;
    double intervalStart = datasetData[0][0];
    double intervalEnd = intervalStart * averageFactor;
    int pointsInBucket = 0;
    double lastX = datasetData[0][0];
    for (int i = 0; i < datasetData[0].length; i++)
    {
      double x = datasetData[0][i];
      if (x > intervalEnd && pointsInBucket >= minNumberOfPointsPerBucket)
      {
        xValues.add(xSum / pointsInBucket);
        yValues.add(ySum / pointsInBucket);
        xSum = 0;
        ySum = 0;
        pointsInBucket = 0;
        if (lastX > intervalEnd)
        {
          intervalStart = lastX;
        }
        else
        {
          intervalStart = intervalEnd;
        }
        intervalEnd = intervalStart * averageFactor;
      }
      xSum += x;
      ySum += datasetData[1][i];
      pointsInBucket++;
      lastX = x;
    }
    if (pointsInBucket != 0)
    {
      xValues.add(xSum / pointsInBucket);
      yValues.add(ySum / pointsInBucket);
    }
    double[][] result = new double[2][xValues.size()];
    for (int i = 0; i < xValues.size(); i++)
    {
      result[0][i] = xValues.get(i);
      result[1][i] = yValues.get(i);
    }
    return result;
  }

}
