package com.github.thomasfox.weatherdataanalyzer.service.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class Wind
{
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
    return (speed) / 10d;
  }
}
