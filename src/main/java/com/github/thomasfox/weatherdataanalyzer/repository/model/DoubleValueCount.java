package com.github.thomasfox.weatherdataanalyzer.repository.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoubleValueCount
{
  private double value;

  private long count;
}
