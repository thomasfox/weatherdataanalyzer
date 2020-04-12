package com.github.thomasfox.weatherdataanalyzer.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.BiFunction;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.thomasfox.weatherdataanalyzer.repository.model.DoubleValueCount;
import com.github.thomasfox.weatherdataanalyzer.service.ChartService;
import com.github.thomasfox.weatherdataanalyzer.service.DateTimeService;
import com.github.thomasfox.weatherdataanalyzer.service.WindDataService;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class WindChartController
{
  private static final int DEFAULT_IMAGE_WIDTH = 600;

  private static final int DEFAULT_IMAGE_HEIGHT = 400;

  private final WindDataService windDataService;

  private final DateTimeService dateTimeService;

  private final ChartService chartService;

  @RequestMapping(value = "/wind/speed/graph", produces="image/png")
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

  @RequestMapping(value = "/wind/speed/histogram", produces="image/png")
  public ResponseEntity<byte[]> getWindSpeedHistogram(
      @RequestParam("from") String fromString,
      @RequestParam("to") String toString)
  {
    return getHistogram(fromString, toString, windDataService::getSpeedHistogramForTimeRange, "speed", "kts", 1d, true);
  }

  @RequestMapping(value = "/wind/direction/histogram", produces="image/png")
  public ResponseEntity<byte[]> getWindDirectionHistogram(
      @RequestParam("from") String fromString,
      @RequestParam("to") String toString)
  {
    return getHistogram(fromString, toString, windDataService::getDirectionHistogramForTimeRange, "direction", "°", 20d, false);
  }

  private ResponseEntity<byte[]> getHistogram(
      String fromString,
      String toString,
      BiFunction<Date, Date, List<DoubleValueCount>> histogramProvider,
      String histogramEntityName,
      String histogramEntityUnits,
      double bucketSize,
      boolean extraBucketForZero)
  {
    Date from = dateTimeService.parse(fromString);
    Date to = dateTimeService.parse(toString);
    List<DoubleValueCount> speedHistogram = histogramProvider.apply(from, to);
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    int bucketCount = 0;
    double bucketValue = 0d;
    for (int i = 0; i < speedHistogram.size(); i++)
    {
      long count = speedHistogram.get(i).getCount();
      double value = speedHistogram.get(i).getValue();
      if (value == 0 && extraBucketForZero)
      {
        dataset.addValue((Number) count, 0, value);
      }
      else
      {
        if (bucketCount == 0 || value / bucketSize + 0.05 < Math.ceil(bucketValue / bucketSize + 0.05d))
        {
          bucketCount += count;
          bucketValue = value;
        }
        else
        {
          bucketValue =  (Math.ceil(bucketValue / bucketSize + 0.05d) - 0.5d) * bucketSize;
          dataset.addValue((Number) bucketCount, 0, bucketValue);
          bucketCount = 0;
          bucketValue = 0d;
        }
      }
    }
    JFreeChart histogram = ChartFactory.createBarChart(
        histogramEntityName + " histogram",
        histogramEntityName + " [" + histogramEntityUnits + "]",
        "count",
        dataset);
    histogram.removeLegend();
    return chartService.createReponseEntityFromChart(histogram);
  }
}
