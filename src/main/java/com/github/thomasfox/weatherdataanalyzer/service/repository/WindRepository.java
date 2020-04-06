package com.github.thomasfox.weatherdataanalyzer.service.repository;

import java.util.Date;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.github.thomasfox.weatherdataanalyzer.service.model.Wind;

public interface WindRepository extends CrudRepository<Wind, Integer>
{
  @Query(value = "SELECT min(time) FROM Wind")
  public Date getMinTime();

  @Query(value = "SELECT max(time) FROM Wind")
  public Date getMaxTime();

  @Query(value = "SELECT avg(speed) FROM Wind WHERE time >= :start AND time < :end")
  public float getAverageSpeed(@Param("start") Date start, @Param("end") Date end);
}
