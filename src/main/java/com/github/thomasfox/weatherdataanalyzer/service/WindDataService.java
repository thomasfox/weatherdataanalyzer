package com.github.thomasfox.weatherdataanalyzer.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Component;

import com.github.thomasfox.weatherdataanalyzer.service.model.Wind;
import com.github.thomasfox.weatherdataanalyzer.service.repository.WindRepository;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class WindDataService
{
  /**
   * The factor converting the database wind speed number into knots.
   */
  private static final float WIND_SPEED_IN_KNOTS_DATABASE_FACTOR = 0.1f;

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
   * @return the average speed in knots
   */
  public float getAverageSpeed(Date start, Date end)
  {
    return windRepository.getAverageSpeed(start, end) * WIND_SPEED_IN_KNOTS_DATABASE_FACTOR;
  }
}
