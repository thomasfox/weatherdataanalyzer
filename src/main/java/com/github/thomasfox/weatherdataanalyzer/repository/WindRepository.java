package com.github.thomasfox.weatherdataanalyzer.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.github.thomasfox.weatherdataanalyzer.repository.model.Wind;

public interface WindRepository extends CrudRepository<Wind, Integer>
{
  @Query(value = "SELECT min(time) FROM Wind")
  Date getMinTime();

  @Query(value = "SELECT max(time) FROM Wind")
  Date getMaxTime();

  @Query(value = "SELECT avg(speed) FROM Wind WHERE time >= :start AND time < :end")
  Double getAverageSpeed(@Param("start") Date start, @Param("end") Date end);

  @Query(value = "SELECT avg(direction) FROM Wind WHERE time >= :start AND time < :end")
  Double getAverageDirection(@Param("start") Date start, @Param("end") Date end);

  List<Wind> findByTimeGreaterThanAndTimeLessThanEqual(Date start, Date end);
}
