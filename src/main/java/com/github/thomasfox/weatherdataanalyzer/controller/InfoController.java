package com.github.thomasfox.weatherdataanalyzer.controller;

import java.util.Date;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.thomasfox.weatherdataanalyzer.service.WindDataService;

import lombok.AllArgsConstructor;
import lombok.Data;

@RestController
public class InfoController
{
  private final WindDataService windDataService;

  public InfoController(WindDataService loader)
  {
    this.windDataService = loader;
  }

  @RequestMapping("/info")
  public Info info()
  {
    Info result = new Info(
        windDataService.count(),
        windDataService.getMinTime(),
        windDataService.getMaxTime());
    return result;
  }

  @Data
  @AllArgsConstructor
  private static class Info
  {
    private long count;

    private Date minTime;

    private Date maxTime;
  }
}
