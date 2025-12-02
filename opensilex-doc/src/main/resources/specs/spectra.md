# Specifications : Spectra


|Date      |Author          |Developer(s)|Version OpenSilex|Comment|
|----------|----------------|------------|-----------------|
|2024-11-27|Emilie Fernandez|EF          |                 |

## Needs

Component developed in order to store, analyze, and update spectral data and models in a sustainable way.

## Solution

{Describe the solution we chose in OpenSILEX. You can explain why this solution was chosen, which
other solutions were considered and why they were not kept.}

### Business logic

Import data files in DX format with special processing, TSV format, and CSV format
Export in DX and TSV formats interpretable by chemometrics software
Link a sample to multiple data files and one or more biochemical values
Graphical visualization of a raw data file in DX format

## Technical specifications

### Detailed explanations

#### Back-end

`DXFileConverter`: Graphical visualization of DX file
`DXFileParser`: Split DX file into as many files as samples found
`DXMetadataParser`: Convert dx metadata into json (same for CSV)
`DXToTSVConverter`: Export DX files into TSV format interpretable by chemometrics software (same for CSV)
`DataLogic.java`: Add prov_used column on CSV data import
`Ontology`: Add spectra datafile type
`DataFilesAPI.java`:
    - Add file size limit for import
    - Edit getPicturesThumbnails service for graphical visualization of spectral data files
    - `uploadAndParseDX`: new service for inserting DX data files
    - `uploadAndParseSpectraCSV`: new service for inserting CSV data files
    - `exportDXToTSV`: new service for exporting spectral data files
    - Deleting locally stored files

#### Front-end

`DataFilesList.vue`: Add item selection and import, export and delete button
`ExportDataFileModal.vue`: Export datafiles modal
`DataFileForm.vue`: Form
`GenerateDataTemplateFrom.vue`: Edit data template CSV for add prov_used column

### Tests



### Environment

#### Dependency

`jfreechart` library for graphical visualization of spectral data files

