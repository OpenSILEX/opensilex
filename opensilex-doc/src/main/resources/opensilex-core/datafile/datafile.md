# Description

## Gestion multi-source des fichiers

## Optimisation de la gestion des miniatures

## Amélioration de la sécurité des données

- Calcul d'un checksum
- Calcul de la taille

# Schema

# FileSource

# FileRecord

# FileStorageService

- Plusieurs type de connection pour un seul service/prefix (ex: datafile, document)
- À la lecture, le chemin depuis la base de métadonnées (mongodb) est résolu selon la configuration courante d'une instance

> Après

- Stockage de la connection au sein de chaque file (datafile, document) + stockage du chemin absolu
- Résolution avec la configuration courante : 
  - Plusieurs id/prefix datafile -> datafile/s3-meso-1, datafile/irods-phenome-1
  - Initialisation
    - Renseigner chaque configuration/credentials dans la config
    - Dans le cas d'une perte des credentials, les informations sur le fichiers restent valides (pas de perte de tracabilité/identification)

> Écriture

- Utilisation du default fs ou du système de stockage associé au service