package com.github.thomasfox.weatherdataanalyzer.controller;

import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.thomasfox.weatherdataanalyzer.service.WindDataService;
import com.github.thomasfox.weatherdataanalyzer.service.model.TimeRange;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class TimeIntervalController
{
  private static final Long TEN_MINUTES_IN_MILLIS = 10L * 60L * 1000L;

  private final WindDataService windDataService;

  @RequestMapping("/times/find")
  public List<TimeRange> findByWindSpeed(
      @RequestParam("from") @DateTimeFormat(iso = ISO.DATE) Date from,
      @RequestParam("to") @DateTimeFormat(iso = ISO.DATE) Date to,
      @RequestParam("speedFrom") Double speedFrom,
      @RequestParam("speedTo") Double speedTo)
  {
    List<TimeRange> timeRangesWithAverageWindSpeedIn = windDataService.getTimeRangesWithAverageWindSpeedAndDirectionIn(
        from,
        to,
        speedFrom,
        speedTo,
        0d,
        360d,
        TEN_MINUTES_IN_MILLIS,
        true);
    return timeRangesWithAverageWindSpeedIn;
  }
}
