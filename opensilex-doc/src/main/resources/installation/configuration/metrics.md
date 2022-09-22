---
- file-system.md
- OpenSILEX
- Copyright © INRAE 2022
- Creation date: 14 February, 2022
- Contact: arnaud.charleroy@inrae.fr
---

# Service `metrics` - metrics 

The "metrics" service is used to present the amount of data available within an experiment or periodically in an instance of OpenSILEX on concepts (Data by variable, Germplasm (Plant material), Devices, Scientific Object). .

## Feature

Thanks to the configuration file, it is possible to define the retrieval of this information over a period with a unit of time which makes it possible to define the interval of production of metrics for the system and within the framework of the experiments.

Module-core configuration file for metrics key

```yaml
# ------------------------------------------------------------------------------
# Configuration for module: CoreModule (CoreConfig)
core:
  # Metrics options (MetricsConfig)
  metrics:
    # Activate access metrics (boolean)
    enableMetrics: true
    # Metrics configs about system (SystemMetricsConfig)
    system:
      # First metrics for any time depending on is time unit (int)
      timeBeforeFirstMetric: 1
      # Delay between whole system metrics (combined with corresponding TimeUnit) (int)
      delayBetweenMetrics: 30
      # Default metrics units : DAYS, HOURS, MINUTES, SECONDS are authorized (String)
      metricsTimeUnit: DAYS
    # Metrics configs about experiments (ExperimentsMetricsConfig)
    experiments:
      # First metrics for any time depending on is time unit (int)
      timeBeforeFirstMetric: 1
      # Delay between whole system metrics (combined with corresponding TimeUnit) (int)
      delayBetweenMetrics: 7
      # Default metrics units : DAYS, HOURS, MINUTES, SECONDS are authorized (String)
      metricsTimeUnit: DAYS
```

Example, if the time unit is in days for the system or the experiments, the first measurement will be made on the first day.
The rest will be done according to the duration of the interval defined for the type of information as well as the duration of time defined.

The calculated metrics are data count by variable, scientific object count by type, device count by type, plant material by type.
The operation of the computation is based on a ScheduledExecutorService which is launched periodically after the initialization of the application. The ScheduleMetrics class implements ApplicationEventListener which allows to track the launch event and the termination of the application.
