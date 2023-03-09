---
- dashboard.md
- OpenSILEX
- Copyright © INRAE 2023
- Creation date: 06 March, 2023
- Contact: sebastien.prado@inrae.fr
---

# Allow metrics and choose their refresh period

The "metrics" service is used to present the amount of data available periodically in an instance of OpenSILEX on concepts (Data, Experiment, Scientific Object)...

Thanks to the configuration file, it is possible to define the system metrics retrieval over a user-defined time period.

Module-core configuration file for metrics key:

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

Example, if the time unit is in days for the system, the first measurement will be made on the first day.
The following measurements will be done according to the defined `delayBetweenMetrics` associated with `metricsTimeUnit`.

The calculated metrics are data, scientific object and experiment.

# Variable choice for data visualization

The visualization component displays data associated to a variable, over a specific time period.

Graphic configuration for variable key :

```yaml
# ------------------------------------------------------------------------------
# Configuration for module: CoreModule (CoreConfig)
front:
  dashboard:
    graph1:
      # Variable URI to use for the visualization
      variable: "dev:id/variable/air_temperature_thermometer_degreecelsius"
```

Example, if the selected variable URI is : `dev:/id/variable/air_temperature_thermometer_degreecelsius`, the graphic will display the data of all devices measuring this variable.
It is then possible with the interface to filter the devices displayed and the periods chosen.
