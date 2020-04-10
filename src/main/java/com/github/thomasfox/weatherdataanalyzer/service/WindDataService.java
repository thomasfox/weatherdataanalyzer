package com.github.thomasfox.weatherdataanalyzer.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.github.thomasfox.weatherdataanalyzer.repository.WindRepository;
import com.github.thomasfox.weatherdataanalyzer.repository.model.Wind;
import com.github.thomasfox.weatherdataanalyzer.service.model.TimeData;
import com.github.thomasfox.weatherdataanalyzer.service.model.TimeRange;
import com.github.thomasfox.weatherdataanalyzer.service.model.TimeRangeWithData;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class WindDataService
{
  private final WindRepository windRepository;

  public List<Wind> getAll()
  {
    List<Wind> result = new ArrayList<>();
    windRepository.findAll().forEach(result::add);
    return result;
  }

  public long count()
  {
    return windRepository.count();
  }

  public Date getMinTime()
  {
    return windRepository.getMinTime();
  }

  public Date getMaxTime()
  {
    return windRepository.getMaxTime();
  }

  /**
   * Returns the average wind speed in knots.
   * The average is over data points, not over time interval
   * (so if sample times are irregular, the average may be wrong).
   *
   * @param start start time of the averaging interval
   * @param end end time of the averaging interval, must be larger than the start time.
   *
   * @return the average speed in knots, or null if no data points are within time range
   */
  public Double getAverageSpeed(Date start, Date end)
  {
    Double rawAverageSpeed = windRepository.getAverageSpeed(start, end);
    if (rawAverageSpeed == null)
    {
      return null;
    }
    return rawAverageSpeed * Wind.WIND_SPEED_IN_KNOTS_DATABASE_FACTOR;
  }

  /**
   * Returns the average wind direction in degrees.
   * The average is over data points, not over time interval
   * (so if sample times are irregular, the average may be wrong).
   *
   * @param start start time of the averaging interval
   * @param end end time of the averaging interval, must be larger than the start time.
   *
   * @return the average direction in degrees, or null if no data points are within time range
   */
  public Double getAverageDirection(Date start, Date end)
  {
    return windRepository.getAverageDirection(start, end);
  }

  public TimeRangeWithData getDataForTimeRange(Date start, Date end, Function<Wind, TimeData> windConverter)
  {
    List<Wind> dataPoints = windRepository.findByTimeGreaterThanAndTimeLessThanEqual(start, end);
    List<TimeData> resultData = new ArrayList<>();
    for (Wind wind : dataPoints)
    {
      resultData.add(windConverter.apply(wind));
    }
    return new TimeRangeWithData(start, end, resultData);
  }

  public List<TimeRangeWithData> getWithSpeedAndDirectionIn(
      Date start,
      Date end,
      double lowerSpeedBoundary,
      double upperSpeedBoundary,
      double lowerDirectionBoundary,
      double upperDirectionBoundary,
      long averageMillis,
      Function<Wind, TimeData> windConverter)
  {
    List<TimeRange> timeRanges = getTimeRangesWithAverageWindSpeedAndDirectionIn(
        start,
        end,
        lowerSpeedBoundary,
        upperSpeedBoundary,
        lowerDirectionBoundary,
        upperDirectionBoundary,
        averageMillis,
        false);
    Map<TimeRange, List<Wind>> windIntervals = new LinkedHashMap<>();
    for (TimeRange timeRange: timeRanges)
    {
      windIntervals.put(timeRange, windRepository.findByTimeGreaterThanAndTimeLessThanEqual(
          new Date(timeRange.getStart()),
          new Date(timeRange.getEnd())));
    }
    List<TimeRangeWithData> result = new ArrayList<>();
    for (Map.Entry<TimeRange, List<Wind>> singleWindInterval : windIntervals.entrySet())
    {
      List<TimeData> singleResultInterval = new ArrayList<>();
      for (Wind wind : singleWindInterval.getValue())
      {
        singleResultInterval.add(windConverter.apply(wind));
      }
      result.add(new TimeRangeWithData(singleWindInterval.getKey(), singleResultInterval));
    }
    return result;
  }

  public List<TimeRange> getTimeRangesWithAverageWindSpeedAndDirectionIn(
      Date start,
      Date end,
      double lowerSpeedBoundary,
      double upperSpeedBoundary,
      double lowerDirectionBoundary,
      double upperDirectionBoundary,
      long averageMillis,
      boolean collapseAdjoiningIntervals)
  {
    if (averageMillis <=0 )
    {
      throw new IllegalArgumentException("averageMillis must be larger than 0");
    }
    if (lowerSpeedBoundary >= upperSpeedBoundary )
    {
      throw new IllegalArgumentException("upperSpeedBound must be larger than lowerSpeedBound");
    }
    if (lowerDirectionBoundary >= upperDirectionBoundary )
    {
      throw new IllegalArgumentException("upperDirectionBoundary must be larger than lowerDirectionBoundary");
    }
    if ((end.getTime() - start.getTime()) / averageMillis > 10000)
    {
      throw new IllegalArgumentException(
          "too many intervals: " + ((end.getTime() - start.getTime()) / averageMillis));
    }

    List<TimeRange> result = new ArrayList<>();
    long intervalStartMillis = start.getTime();
    long lastAddedIntervalEndMillis = 0L;
    while (intervalStartMillis < end.getTime())
    {
      Double averageSpeed = getAverageSpeed(
          new Date(intervalStartMillis),
          new Date(intervalStartMillis + averageMillis));
      Double averageDirection = getAverageDirection(
          new Date(intervalStartMillis),
          new Date(intervalStartMillis + averageMillis));
      if (averageSpeed != null && averageDirection != null
          && averageSpeed >= lowerSpeedBoundary && averageSpeed < upperSpeedBoundary
          && averageDirection >= lowerDirectionBoundary && averageDirection < upperDirectionBoundary)
      {
        if (lastAddedIntervalEndMillis == intervalStartMillis && collapseAdjoiningIntervals)
        {
          result.get(result.size() - 1).setEnd(intervalStartMillis + averageMillis);
        }
        else
        {
          result.add(new TimeRange(intervalStartMillis, intervalStartMillis + averageMillis));
        }
        lastAddedIntervalEndMillis = intervalStartMillis + averageMillis;
      }
      intervalStartMillis += averageMillis;
    }
    return result;
  }
}
