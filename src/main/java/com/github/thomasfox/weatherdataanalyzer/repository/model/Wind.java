package com.github.thomasfox.weatherdataanalyzer.repository.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.github.thomasfox.weatherdataanalyzer.service.model.TimeData;

import lombok.Data;

@Data
@Entity
public class Wind
{
  /**
   * The factor converting the database wind speed number into knots.
   */
  public static final double WIND_SPEED_IN_KNOTS_DATABASE_FACTOR = 0.1d;

  @Id
  private int id;

  /** measuring point. */
  private Date time;

  /** direction where the wind comes from in degrees. */
  private int direction;

  /** wind speed in 1/10 knots. */
  private int speed;

  /** wind speed gusts in measuring interval in 1/10 knots. */
  private int gusts;

  /** 0 for not averaged, everything else for averaged. */
  private int averaged;

  public double getSpeedInKnots()
  {
    return (speed) * WIND_SPEED_IN_KNOTS_DATABASE_FACTOR;
  }

  public TimeData getSpeedTimeData()
  {
    return new  TimeData(getTime().getTime(), getSpeedInKnots());
  }
}
