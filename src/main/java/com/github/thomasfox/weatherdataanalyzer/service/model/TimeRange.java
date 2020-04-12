package com.github.thomasfox.weatherdataanalyzer.service.model;

import java.util.Date;

import lombok.Data;

@Data
public class TimeRange
{
  private long start;

  private long end;

  public TimeRange()
  {
  }

  public TimeRange(long start, long end)
  {
    if (end < start)
    {
      throw new IllegalArgumentException(
          "end " + new Date(end) + " must be larger than or equal to start " + new Date(start));
    }
    this.start = start;
    this.end = end;

  }

  public TimeRange(Date start, Date end)
  {
    if (end.getTime() < start.getTime())
    {
      throw new IllegalArgumentException("end " + end  + " must be after or equal to start " + start);
    }
    this.start = start.getTime();
    this.end = end.getTime();
  }

  public long getDurationMillis()
  {
    return end - start;
  }

  public Date getStartAsDate()
  {
    return new Date(start);
  }

  public Date getEndAsDate()
  {
    return new Date(end);
  }
}
