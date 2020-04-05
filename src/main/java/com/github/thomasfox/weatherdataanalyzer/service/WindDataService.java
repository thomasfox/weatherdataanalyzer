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
}
