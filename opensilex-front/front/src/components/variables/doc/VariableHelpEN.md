# Variable help

Author: Jean-Eudes Hollebecq - Opensilex (23/11/2020)

## Create variables

A variable is a series of observations on some individuals.

Different ways to create different variables are : change the subject of observation (**Entity**), change the **characteristic** observed, 
change the **method** used to observe, and change the **unit** to express the result. 
 
### <span style="color:blue">**Detailed names**</span>

Variables are made of four main fields. Fields are separated by underscores. 
Note that the two first fields define the **trait=Entity_Quality**. Each field is using the CamelCase format.

**`Entity`**: the object that is targeted. You can find for example some entities :

- Canopy
- Row
- Plant
- Stem
- Leaf
- Inflorescence (generic term for all species (includes, ear, spike, panicle, capitulum …) can be specialized into :
    - Male
    - Female
- A suffix may be added to indicate which part of the entity is targeted. By default, the whole entity is sampled.
    - Green
    - Senescent
    - DiseaseSpot
    - Contaminated 
    
**`Characteristic`**: he type of measurements. It can be some physical quantities as well as some observed qualities. 
Here are some characteristic used:
 
- Radiance
- Reflectance
- Transmittance
- SpectralIndex
- CoverFraction
- AreaIndex (Area per ground area : LAI, PAI, GLAI GAI, SAI)
- AreaDensity (area per canopy volume unit)
- Height
- FIPAR
- FAPAR


A suffix is added when necessary to indicate more precisely the measure:
- For `wavelengths`: wavelengths used are added in nm, each wavelength used ending by “nm”. Example: _MCARI570nm730nm850nm_
- For `directions`: angles used are added in degree, each direction used ending by “deg”. Example: _CoverFraction45deg_
- By default, the value is the expectation (mean) over the entity. When it is not the case, a suffix is added at the end. The following suffixes are used:
    - `Med`: when it is the Median
    - `Std`: for Standard Deviation
    - `Flag`: for indicating classes of measurement quality
    
    - <span style="color:red">**NOTA BENE**</span> : 
        <p>
        This is not a computation of the « mean » variable, but another output of the sensor describing 
        the spatial heterogeneity of the entity or some unsuitables conditions leading to possibly false measurement. <br>
        The computed variables over the time series ( maximum of temperature of the day, etc) 
        are meant to stay withing the data analysis scripts and not stored in the database. 
        </p>

**`Method`**: the method used to estimate the variable, i.e. the value of the trait. You can find some existing methods here :

- Calibration for Radiance et Reflectance and RGB.
- BandCombination (for vegetation index using spectral reflectance)
- Height and PointCloud
    - Manual
    - Photogrammetry
    - LiDAR
- AreaIndex
    - DirectMeasurement
    - PhysicalModel (Radiative Transfer Model Inversion)
    - EmpiricalModel
- FIPAR/FAPAR
    - DirectMeasurement
    - ImageSegmentation
    - GeometricalModel (LiDAR)
    - PhysicalModel (Radiative Transfer Model Inversion)
    - EmpiricalModel
- Lodging
    - VisualScore
    - EmpiricalModel (Plant_Height and/or Canopy_VI)
- Number
    - DirectMeasurement
    - ImageProcessing
- ChlorophyllContent/ NitrogenContent/ WaterContent
    - DirectMeasurement (WetChemistry & Spectrometry & SPAD)
    - PhysicalModel (Radiative Transfer Model Inversion)
    - EmpiricalModel
- SurfaceTemperature
    - ThermalImaging
    - Radiothermometer
    - DirectMeasurement (thermocouple)

 <span style="color:red">**NOTA BENE**</span> : Addition of new methods is much welcome to explain your way to proceed 
 and thus have as much details as possible.

**`Unit `**: the unit used to measure the trait. The abbreviations are made to be consistent with the current use. 
Small cap letters are preferred, except when the use is for capital letters: (degK, degC)

- Kilogram (kg)
- Gram (g)
- Meter (m)
- Centimeter (cm)
- Unitless (uless)
- Degree for inclination (deg)
- DegreeCelsius (degC)
- DegreeKelvin (degK)

To combine units, add “per” (abbreviation “p”) <br>
For area, add suffix “2” <br>
For volume, add suffix “3” <br>

### <span style="color:blue">**Short name**</span>

<p>
For conveniance purpose a short name is given as well, by default is only the trait (Entity+characteristic). 
It is used to label some graphs and to fit better in tables headers. <br>
This short name can use abregated words as well, see for example :

_Canopy_InclinationAngle_PhysicalModel_Degree → Can_IncAng_PhyMod_deg
</p>