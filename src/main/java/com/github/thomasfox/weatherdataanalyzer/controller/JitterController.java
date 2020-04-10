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

import com.github.thomasfox.weatherdataanalyzer.repository.model.Wind;
import com.github.thomasfox.weatherdataanalyzer.service.ChartService;
import com.github.thomasfox.weatherdataanalyzer.service.DateTimeService;
import com.github.thomasfox.weatherdataanalyzer.service.WindDataService;
import com.github.thomasfox.weatherdataanalyzer.service.model.TimeData;
import com.github.thomasfox.weatherdataanalyzer.service.model.TimeRangeWithData;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class JitterController
{
  private final WindDataService windDataService;

  private final DateTimeService dateTimeService;

  private final ChartService chartService;

  @RequestMapping(value = "/jitter/histogram", produces="image/png")
  public ResponseEntity<byte[]> jitterHistogram(
      @RequestParam("from") String fromString,
      @RequestParam("to") String toString,
      @RequestParam("cutoff") int cutoff)

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
        if (bin < cutoff)
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
}
