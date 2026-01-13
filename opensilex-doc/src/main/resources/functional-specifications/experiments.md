**History**

| Date       | Author           | Developer(s)     | OpenSILEX version | Comment                                  |
|------------|------------------|------------------|-------------------|------------------------------------------|
| 10/07/2023 | Valentin Rigolle | Valentin Rigolle | 1.0.0             | Document creation & available facilities |

---

> WARNING : This document is incomplete ! You can help by expanding it. Make sure you update the history afterward.
>
> Currently covered topics :
>
> - Available facilities
>
> Missing topics :
>
> - Literally everything else about experiments

# Specifications : Experiments

## Needs

Experiments are a central concept in OpenSILEX. An experiment is a context where scientific objects are defined, and
is linked to many kinds of metadata. An experiment can be associated with organizations, facilities, projects,
persons, starts at a certain date and may have an end date.

When your register a scientific object for an experiment, you can associate metadata with it, often linked to the
experiment. One of the metadata is the facility where the scientific object is hosted. It must be an **available
facility** of the experiment.

## Solution

Available facilities are inferred from the organizations and facilities associated with the experiment.

### Business logic

Available facilities of an experiment are the union of the two following sets of facilities :

- Facilities hosting organizations used by the experiment
- Facilities used by the experiment

## Technical specifications

### Definitions

- **Available facility** : a facility, associated in some way to an experiment, that can host scientific objects of this
  experiment.