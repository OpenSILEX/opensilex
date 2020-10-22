# Comment ajouter des ressources génétiques ?  

## Introduction
Vous pouvez rentrer ici toutes les ressources génétiques qui sont utilisées dans vos expérimentations.
Vous devez insérer les ressources génétiques avant d'insérer vos objets scientifiques.

Voici les différents types de ressources génétiques que vous pouvez insérer, de la moins fine à la plus fine : 
- Espèce
- Variété
- Accession (ou clone)
- PlantMaterialLot (la partie physique de l'accession qui a été utilisée. Cela peut-être un lot de semence, ou un lot de vitroplants par exemple.)

Selon le type de ressource sélectionné, le tableau d'insertion sera différent.

## Règles : 
**Globales** :
- Le nom est obligatoire pour tout type de ressource
- Si l'URI n'est pas renseigné, alors il sera généré automatiquement. C'est l'URI qui sert d'identifiant unique dans la base.
- Tous les autres champs exceptés URI de l'espèce, URI de la variété, URI de l'accession sont facultatifs
- Pour ajouter plusieurs synonymes, utilisez | comme séparateur
- Vous pouvez ajouter des attributs en cliquant sur le bouton *Ajouter Colonne*. (Ces attributs sont là à titre d'information, ils ne pourront pas être utilisés pour filtrer par exemple.)

**Création d'espèce**  
Les espèces issues de l'ontologie Agrovoc ont été automatiquement entrées dans la base avec leurs différentes traduction. Pour cette raison, la mise à jour d'une espèce via l'interface n'est pas possible afin d'éviter que leurs traductions soient écrasées.
Si une espèce est manquante, vous pouvez l'ajouter en utilisant cette fonctionnalité mais celle-ci ne sera alors pas traduite.

**Création de variété** 
- l'URI de l'espèce est obligatoire et doit exister dans la base. Si celle-ci n'existe pas, il faudra alors créer cette espèce.

**Création d'accession** 
- Il est obligatoire de renseigner l'URI de la variété ou l'URI de l'espèce.
- Si l'URI de la variété est renseigné mais pas l'espèce, le système ira automatiquement rechercher l'espèce liée à cette variété pour la renseigner sur l'accession.
- Si vous renseignez à la fois l'URI de l'espèce et de la variété, alors le système vérifiera que la variété appartient bien à cette espèce.

**Création de lot (lot de semence ou autre)** 
- Il est obligatoire de renseigner l'URI de l'accession, l'URI de la variété ou l'URI de l'espèce.
- Si l'URI de l'accession est renseigné mais ni la variété, ni l'espèce, alors le système ira automatiquement rechercher l'espèce et la variété liées à cette accession pour les renseigner sur le lot inséré.
- Si vous renseignez à la fois l'URI de l'espèce, de la variété et de l'accession, alors le système vérifiera que l'accession appartient bien à la variété et l'espèce renseignées.

## Exemple de scénario : 
Je souhaite insérer des plantes de maïs sur lesquels j'ai les informations de variété et de lot de semences.
1. Je vérifie que l'espèce maïs existe et je récupère son URI.
2. Je crée les différentes variétés en renseignant l'URI de l'espèce.
3. Je récupère les URI des variétés et je crée les différents lots en renseignant les URI des variétés.
4. Je récupère les URI des lots de semence insérés et je les utilise pour créer mes objets scientifiques. 
