package com.github.thomasfox.weatherdataanalyzer.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.thomasfox.weatherdataanalyzer.repository.model.DoubleValueCount;
import com.github.thomasfox.weatherdataanalyzer.repository.model.Wind;
import com.github.thomasfox.weatherdataanalyzer.service.ChartService;
import com.github.thomasfox.weatherdataanalyzer.service.DateTimeService;
import com.github.thomasfox.weatherdataanalyzer.service.WindDataService;
import com.github.thomasfox.weatherdataanalyzer.service.model.TimeData;
import com.github.thomasfox.weatherdataanalyzer.service.model.TimeRange;
import com.github.thomasfox.weatherdataanalyzer.service.model.TimeRangeWithData;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class JitterController
{
  private final WindDataService windDataService;

  private final DateTimeService dateTimeService;

  private final ChartService chartService;

  @RequestMapping(value = "/wind/jitter/histogram", produces="image/png")
  public ResponseEntity<byte[]> jitterHistogram(
      @RequestParam("from") String fromString,
      @RequestParam("to") String toString,
      @RequestParam(value = "histogramStart", required = false) Integer histogramStart,
      @RequestParam(value = "histogramCutoff", required = false) Integer histogramCutoff)
  {
    Date from = dateTimeService.parse(fromString);
    Date to = dateTimeService.parse(toString);

    TimeRangeWithData speedPoints = windDataService.getDataForTimeRange(from, to, Wind::getSpeedTimeData);

    List<Integer> counts = new ArrayList<>();
    Long lastX = null;
    for (TimeData point : speedPoints.getData())
    {
      long x = point.getTimestamp();
      if (lastX != null)
      {
        int bin = (int) ((x - lastX)/1000);
        if ((histogramStart == null || bin >= histogramStart)
            && (histogramCutoff == null || bin <= histogramCutoff))
        {
          makeSureListHasAtLeastSize(counts, bin + 1);
          counts.set(bin, counts.get(bin) + 1);
        }
      }
      lastX = x;
    }
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    for (int i = 0; i < counts.size(); i++)
    {
      dataset.addValue((Number) counts.get(i), 0, i);
    }
    JFreeChart histogram = ChartFactory.createBarChart("Jitter", "time", "count", dataset);
    return chartService.createReponseEntityFromChart(histogram);
  }

  private void makeSureListHasAtLeastSize(List<Integer> list, int minimumSize)
  {
    while(list.size() < minimumSize)
    {
      list.add(0);
    }
  }

  @RequestMapping(value = "/wind/timedistance/findLargerThan")
  public List<TimeRange> jitterHistogram(
      @RequestParam("from") String fromString,
      @RequestParam("to") String toString,
      @RequestParam(value = "threshold", required = false) Integer threshold)
  {
    Date from = dateTimeService.parse(fromString);
    Date to = dateTimeService.parse(toString);

    TimeRangeWithData speedPoints = windDataService.getDataForTimeRange(from, to, Wind::getSpeedTimeData);

    List<TimeRange> result = new ArrayList<>();
    Long lastX = null;
    for (TimeData point : speedPoints.getData())
    {
      long x = point.getTimestamp();
      if (lastX != null)
      {
        int bin = (int) ((x - lastX)/1000);
        if (bin >= threshold)
        {
          result.add(new TimeRange(new Date(lastX) ,new Date(x)));
        }
      }
      lastX = x;
    }
    return result;
  }


  @RequestMapping(value = "/wind/badData")
  public List<TimeRange> badDataCandidates(
      @RequestParam("from") String fromString,
      @RequestParam("to") String toString)
  {
    Date from = dateTimeService.parse(fromString);
    Date to = dateTimeService.parse(toString);
    List<TimeRange> result = new ArrayList<>();
    Date intervalStart = new Date(from.getTime());
    Date intervalEnd = new Date(intervalStart.getTime() + 5000 * 60l);
    while (intervalStart.before(to))
    {
      List<DoubleValueCount> speedHistogram = windDataService.getSpeedHistogramForTimeRange(from, to);
      if (speedHistogram.size() == 0 || (speedHistogram.size() == 1 && speedHistogram.get(0).getValue() >= 2d))
      {
        result.add(new TimeRange(intervalStart, intervalEnd));
      }
      intervalStart = intervalEnd;
      intervalEnd = new Date(intervalStart.getTime() + 5000 * 60l);
    }
    return result;
  }

  @RequestMapping(value = "/wind/count")
  public Long count(
      @RequestParam("from") String fromString,
      @RequestParam("to") String toString)
  {
    Date from = dateTimeService.parse(fromString);
    Date to = dateTimeService.parse(toString);
    long result = windDataService.count(from, to);
    return result;
  }

}
