package com.github.thomasfox.weatherdataanalyzer.service.repository;

import java.util.Date;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.github.thomasfox.weatherdataanalyzer.service.model.Wind;

public interface WindRepository extends CrudRepository<Wind, Integer>
{
  @Query(value = "SELECT min(time) FROM Wind")
  public Date getMinTime();

  @Query(value = "SELECT max(time) FROM Wind")
  public Date getMaxTime();

}
