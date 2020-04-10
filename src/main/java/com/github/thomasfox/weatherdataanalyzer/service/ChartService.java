package com.github.thomasfox.weatherdataanalyzer.service;

import static com.github.thomasfox.weatherdataanalyzer.Constants.DEFAULT_IMAGE_HEIGHT;
import static com.github.thomasfox.weatherdataanalyzer.Constants.DEFAULT_IMAGE_WIDTH;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ChartService
{
  public ResponseEntity<byte[]> createReponseEntityFromChart(JFreeChart lineChart)
  {
    ByteArrayOutputStream image = new ByteArrayOutputStream();
    try
    {
      ChartUtils.writeChartAsPNG(
          image,
          lineChart,
          DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT,
          new ChartRenderingInfo(),
          false,
          5);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }

    ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(image.toByteArray(), HttpStatus.OK);
    return responseEntity;
  }
}
