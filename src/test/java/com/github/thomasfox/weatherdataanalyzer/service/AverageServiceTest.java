package com.github.thomasfox.weatherdataanalyzer.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

public class AverageServiceTest
{
  private final AverageService averageService = new AverageService();

  @Test
  public void testAverageChartDataByOne()
  {
    double[][] averageResult = averageService.averageChartData(getChartData(), 1);
    assertArrayEquals(averageResult, getChartData());
  }

  private double[][] getChartData()
  {
    double[][] result = new double[2][10];
    for (int i = 1; i < 10; i++)
    {
      result[0][i] = i;
      result[1][i] = i + 1;
    }
    return result;
  }
}
