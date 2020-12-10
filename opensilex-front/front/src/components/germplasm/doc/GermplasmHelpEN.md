# How to add germplasm ? 

## Introduction
Here you can insert every germplasm you use in your experimentations.
You have to insert them before inserting your scientific objects.

Here are presented all the germplasm types you can add, from the more general to the most precised:
- Species
- Variety
- Accession (or clone)
- PlantMaterialLot (The physical part of the accession which has been used. This can be a seed lot or a vitroplants lot for example.

Depending on the selected germplasm type, the insertion table will change.

## Rules : 
**General** :
- The name is mandatory for every germplasm type
- If the URI is not given, then the system will automatically generate a URI. This URI is the unique identifier in the database.
- All other columns are optional excepted Species URI,variety URI and accession URI.
- To add several synonyms or subtaxa, use | as separator. 
- You can add additional information by clicking on the button *Add Column*. (this new attributes are used for information but won't be used as search filter for example.)

**Species insertion**  
The species defined by Agrovoc ontology have been automatically inserted inside the system database with their translations. For this reason, the species can't be updated for now because this would erase the translations. 
If a species is missing, you can add it by using this functionnality but it won't be translated.

**Variety insertion** 
- The species URI is mandatory and the species must exist in the database. If it doesn't exist, you have to insert it.

**Accession insertion** 
- It is mandatory to fill the variety URI or Species URI.
- If the variety is given but not the species, then the system will automatically look for the species linked to this variety and fill it for the accession.
- If you fill the species URI and the variety URI, then the system will check that the variety belongs to the species.

**Lot insertion (Seedlot or other)** 
- It is mandatory to fill the accession URI, variety URI or Species URI.
- If the accession URI is given but not the variety, neither the species, then the system will automatically look for the species and the variety linked to this accession and fill them for the lot.
- If you fill species URI, variety URI and accession URI, then the system will check that the accession belongs to the variety and the species.

## Scenario example : 
I want to insert plants of maize on my experiment. I know the variety and the seedlot of each plant.
1. I check that the species maize exists and I retrieve its URI.
2. I insert all the varieties by giving the maize species URI.
3. I retrieve the varieties URI and I insert all the seedlots with the rights variety URIs.
4. I retrieve the seedlots URIs and use them to create my scientific objects.
