package com.github.thomasfox.weatherdataanalyzer.repository.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntValueCount
{
  private int value;

  private long count;
}
