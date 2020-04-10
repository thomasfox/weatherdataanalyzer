package com.github.thomasfox.weatherdataanalyzer.service.model;

import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TimeRangeWithData
{
  public TimeRangeWithData(Date start, Date end, List<TimeData> data)
  {
    this.range = new TimeRange(start, end);
    this.data = data;
  }

  private TimeRange range;

  private List<TimeData> data;
}
