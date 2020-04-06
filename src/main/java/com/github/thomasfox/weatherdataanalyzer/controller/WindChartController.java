package com.github.thomasfox.weatherdataanalyzer.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.thomasfox.weatherdataanalyzer.service.WindDataService;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class WindChartController
{
  private static final int DEFAULT_IMAGE_WIDTH = 600;

  private static final int DEFAULT_IMAGE_HEIGHT = 400;

  private final WindDataService windDataService;

  @RequestMapping(value = "/wind/speed/graph", method = RequestMethod.GET, produces="image/png")
  public ResponseEntity<byte[]> display(
          @RequestParam("from") @DateTimeFormat(iso = ISO.DATE) Date from,
          @RequestParam("to") @DateTimeFormat(iso = ISO.DATE) Date to)
      throws IOException
  {
    JFreeChart lineChart = ChartFactory.createTimeSeriesChart(
        "wind",
        "time",
        "Wind speed[kts]",
        createDataset(from, to),
        false,
        false,
        false);
    XYPlot plot = lineChart.getXYPlot();
    DateAxis axis = (DateAxis) plot.getDomainAxis();
    axis.setDateFormatOverride(new SimpleDateFormat("dd.MM.yyyy HH:mm"));

    ByteArrayOutputStream image = new ByteArrayOutputStream();
    ChartUtils.writeChartAsPNG(
        image,
        lineChart,
        DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT,
        new ChartRenderingInfo(),
        false,
        5);

    ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(image.toByteArray(), HttpStatus.OK);
    return responseEntity;
  }

  private XYDataset createDataset(Date from, Date to)
  {
    final TimeSeries series = new TimeSeries("Wind speed");

    long intervals = 100;
    long timeStart = from.getTime();
    long timeEnd = to.getTime();
    long intervalSpan = (timeEnd - timeStart) / intervals;
    for (int i = 0; i < intervals; i++)
    {
      long averageIntervalStart = timeStart + i * intervalSpan;
      long averageIntervalEnd = averageIntervalStart + intervalSpan;

      double windSpeed = windDataService.getAverageSpeed(
              new Date(averageIntervalStart),
              new Date(averageIntervalEnd));
      Date intervalTime = new Date((averageIntervalStart + averageIntervalEnd) / 2);
      series.add(new Millisecond(intervalTime, TimeZone.getTimeZone("UTC"), Locale.getDefault()), windSpeed);
    }
    return new TimeSeriesCollection(series);
 }
}
