# Aide aux variables

---

## Créer des variables

Une variable est une série d’observations sur des individus.
Il existe beaucoup de sujets à observer (**Entités**), caractéristiques à observer (**Quantités**/**Qualités**),
 façons d’y arriver (**Méthodes**), unités ou échelles pour exprimer ces résultats (**Unités**). 
 Ces quatre composants forment le modèle de variable.
 
### <span style="color:blue">**Nom précis**</span>

Les variables sont constituées de quatre parties, séparées par des tirets-bas ( _ ). 
En particulier, les deux champs forment le **trait=Entité_Charactéristique**. 
Chaque champ est écrit en format CamelCase.

**`Entité`**: l’objet qui est ciblé et observé. On trouve par exemple les entités suivantes :

- Canopée
- Rang
- Plante
- Tige
- Feuille
- Inflorescence (terme générique pour plusieurs espèces (y compris, épis, pointe, panicule, capitule …)) peut-être spécialisé en :
    - Mâle
    - Femelle
- Un suffixe peut être ajouté pour distinguer une partie seulement de l’entité, par défaut, l’entité entière est observée.
    - Vert
    - Sénescent
    - Tache de maladie
    - Contaminé 

**`Caractéristique`**: quel type de mesure. C’est la quantité physique mesurée, ou bien la qualité observée sur l’individu. 
On trouve par exemple les caractéristiques suivantes :    
 
- Radiance
- Reflectance
- Transmittance
- Indice spectral
- Fraction de couverture
- Indice de surface (surface par unité de surface au sol : LAI, PAI, GLAI, GAI, SAI)
- Densité de surface (surface par unité de volume du couvert)
- Hauteur
- FIPAR (Fraction du rayonnement photosynthétiquement actif intercepté)
- FAPAR (Fraction du rayonnement photosynthétiquement actif absorbé)


Un suffixe peut être ajouté pour préciser davantage la mesure : 
- Pour les `longueurs d’ondes` : on utilise les “nm”. Exemple : _MCARI570nm730nm850nm_
- Pour les `directions` : les angles sont exprimés en degrés d’angle, chaque direction termine par “deg”. Exemple : _CoverFraction45deg_
- Par défaut, on mesure une grandeur « moyenne » issue d’un capteur sur l’ensemble de l’entité, si ce n’est pas le cas un suffix est ajouté à la fin. Les suffixes suivants sont utilisés :
    - `Med`: lorsqu'il s'agit d'une médiane
    - `Std`: lorsqu'il s'agit d'un écart type
    - `Flag`: lorsqu'il s'agit d'un indice de confiance sur la qualité de la mesure

    -  <span style="color:red">**REMARQUE**</span> : 
        <p>
        Il ne s’agit pas de variables calculées à partir de la série temporelle (moyenne), 
        mais d’une sortie capteur décrivant une hétérogénéité spatiale de l’entité, 
        ou une insuffisance capteur créant une trop grande incertitude de mesure. <br>
        Ces autres variables (calcul de l’écart-type d’une variable, 
        composition de ses propres variables décrivant des conditions jugées inappropriées à une étude), 
        doivent rester dans les scripts d’analyse de données, et ne pas être entreposées dans la base PHIS.
        </p>
        


**`Méthode`**: la méthode utilisée pour obtenir l’observation de l’entité, on trouve notamment :

- Calibration pour Radiance et Reflectance et RGB.
- Combinaison de bandes (pour indices de végétation issus de spectroscopie)
- Hauteur et nuage de points
    - Mesure manuelle
    - Photogrammétrie
    - LiDAR
- Indice de surface
    - Mesure directe
    - Modèle physique (inversion de modèles de transfert radiatif)
    - Modèle empirique
- FIPAR/FAPAR
    - Mesure directe
    - Segmentation d’images
    - Modèle géométrique (LiDAR)
    - Modèle physique (inversion de modèles de transfert radiatif)
    - Modèle empirique
- ChlorophyllContent/ NitrogenContent/ WaterContent
    - Mesure directe (chimie humide, spectrométrie et SPAD)
    - Modèle physique (inversion de modèles de transfert radiatif)
    - Modèle empirique

 <span style="color:red">**REMARQUE**</span> : Il est chaudement recommandé de créer de nouvelles méthodes 
 pour expliquer votre façon de faire et ainsi d'avoir le plus de détails possibles.

**`Unité`**: l’unité utilisée pour exprimer la mesure. On préférera les lettres minuscules, 
sauf quand il est admis d’utiliser des majuscules (degC, degK, …).
- Kilogramme (kg)
- Gramme (g)
- Mètre (m)
- Centimètre (cm)
- Sans unité (adimensionnel)
- Degré pour l'inclinaison (deg)
- Degré Celsius (°C)
- Kelvin (K)

Pour combiner des unités, ajouter “_per_” (abbreviation “p”) . 

Pour des grandeurs au carré, ajouter le suffixe “2”.

Pour les grandeurs au cube, ajouter suffixe “3”.

### <span style="color:blue">**Nom abrégé**</span>

<p>
Il est aussi recommandé d’utiliser un nom abrégé, qui sera plus adapté lors de la manipulation des données 
(étiquettes de graphes, en-têtes de colonnes dans un tableau).
</p>

<p>
Ce nom abrégé peut contenir uniquement le trait (Entité + caractéristique), 
au risque d’entrer en conflit avec d’autres variables mesurant le même trait, dans d’autres unités, ou méthodes. 
Ce nom abrégé peut aussi abréger les composantes en un nom compréhensible mais raccourci. <br>
Exemple : Canopy_InclinationAngle_PhysicalModel_Degree → Can_IncAng_PhyMod_deg
</p>