package com.github.thomasfox.weatherdataanalyzer.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeData
{
  /** Timestamp in milliseconds */
  private long timestamp;

  private double value;
}
