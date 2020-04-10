package com.github.thomasfox.weatherdataanalyzer.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.stereotype.Component;

@Component
public class DateTimeService
{
  public Date parse(String toParse)
  {
    if (toParse == null || toParse.trim().isEmpty())
    {
      return null;
    }
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    try
    {
      return dateFormat.parse(toParse);
    }
    catch (ParseException e1)
    {
      dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
      try
      {
        return dateFormat.parse(toParse);
      }
      catch (ParseException e2)
      {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try
        {
          return dateFormat.parse(toParse);
        }
        catch (ParseException e3)
        {
          throw new IllegalArgumentException("Could not parse " + toParse);
        }
      }
    }
  }
}
