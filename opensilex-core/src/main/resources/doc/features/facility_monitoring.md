
# [Facilities] Visualisation de données environnementales

Auteur: Brice Maussang

Date: 30/05/2023

Developpeureuse(s): Brice Maussang

Versions: Opensilex 1.0.0-rc+7


# Table des matières
1. [Besoin](#besoin)
2. [Solution retenue](#solution-retenue)
3. [Spécification techniques](#specifications)
   1. [Définitions clés](#definitions)
   2. 

## I. Besoin

Un utilisateur doit être en mesure de visualiser rapidement "l'état" d’une infrastructure
(ex: serre, chambre de culture, …) en observant des données environnementales rattachées 
à celle-ci (ex: température, humidité, …).

## II. Solution retenue

Ajouter un onglet "Supervision" dans la page d’une infrastructure (ouvert par défaut)
qui affiche les différentes données environnementales.

Possibilité d’associer un ou plusieurs groupes de variables à une infrastructure dans 
le formulaire de création et de modification.
Un bandeau présent en haut de page met à disposition un sélecteur de groupe de variables 
(parmi ceux associés à l’infrastructure).

Regrouper les données par variables sous la forme de petits encadrés.
Chacun représente une variable avec la dernière donnée (médiane des médianes) 
calculé sur la dernière semaine.
S’il n’y a pas de données sur cette période, affiche la date de la dernière donnée 
enregistrée dans le système.

Pouvoir visualiser le graphique complet contenant les séries de données calculées
ainsi que l’ensemble des séries de données. Le graphique s’affiche en cliquant sur la tuile, 
seules les séries de données calculées sont visibles.
Un bouton de paramètre offre la possibilité de changer la période des données à afficher
et de sélectionner si oui on non afficher toutes les séries de données.

## III. Spécification techniques

### A) Définitions clés

**Variable environnementale**:

Est considérée comme une variable environnementale toute variable ayant des données rattachées
qui pointent vers une infrastructure (via le champ "target").

**Série de données**:

Une série de données est un ensemble de données possédant la même variable ainsi que la même provenance.

### B) Explications détaillées

#### 1. API

##### a) Architecture générale

##### b) Web service