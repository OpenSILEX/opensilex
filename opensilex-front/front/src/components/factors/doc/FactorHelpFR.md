# Vocabulaire pour parler des facteurs

Auteur: Jean-Eudes Hollebecq - Opensilex

## Plan d'experiences

Avant-propos: Il est important de consulter un statisticien lors de la mise en place du plan d'expérience et la définition des variables et facteurs expérimentaux. Une analyse conduite sur un plan bien conçu sera bien plus facile et performante.

1. plan complet :  
   Tous les niveaux de tous les facteurs sont représentés.  
   Ex: Facteur1 à 2 niveaux et facteur2 à 5 niveaux; 2x5 = 10 combinaisons.

2. plan factoriel = idem
3. plan non croisé :  
   contraire d'un plan croisé, certaines combinaisons ne sont pas observées
4. plan équilibré :  
   même nombre d'individus dans chaque classe/ chaque cellule du plan
5. plan déséquilibré :  
   nombre d'individus différents dans chaque classe

L'outil utilisé pour répondre aux questions statistiques posées comme : "est-ce que l'effet du traitement X est significative sur la production de grain?", est l'ANalyse de VAriance (ANOVA). C'est l'outil le plus communément utilisé pour répondre aux questions statistiques.

### Conséquence en anova

Différents types de somme des carrés sont possibles pour tester les hypothèses. On retrouve le TYPE I, II, III et IV dans la majorité des logiciels statistiques (R, SAS, ...).

Les TYPES I et III sont considérés standards et sont équivalents dans une expérience équilibrée et complète.

Dans un plan non complet ou déséquilibré :

- Anova de TYPE I sera différente selon l'ordre d'apparition des facteurs.

- Anova de TYPE III, qui qui correspondent à la variabilité expliquée par l’apport d’un effet sachant que tous les autres effets sont déjà présents dans le modèle.

les TYPES II et IV sont plutôt utilisés dans le cas de facteurs hiérarchiques (II) et contenant des valeurs manquantes (IV). Meme si dans cette dernière situation il est préférable de procéder à une imputation des valeurs manquantes dédiée.

## Facteurs explicatifs

1. Facteur à effet fixes :  
   Tous les niveaux du facteur sont représentatifs de la variabilité du facteur. Ce sont les niveaux d'intérêt

2. Facteur à effets aléatoires :  
   Les niveaux du facteur ne sont pas représentatifs de l'ensemble des niveaux possible. Il s'agit d'un échantillon tiré au hasard parmi les différents niveaux possibles.
3. Facteurs hiérarchiques  
   l'interprétation d'un facteur (subordonné) dépends de la valeur prise par un autre facteur. La mesure du facteur (subordonné), se fait conditionellement aux valeurs prises par un second facteur.

```r
Factors can either be fixed or random.
A factor is fixed when the levels under study are the only levels of interest.
A factor is random when the levels under study are a random sample from a larger population and the goal of the study is to make a statement regarding the larger population.
```

Les facteurs aléatoires permettent également l'analyse de la structure de variance/covariance du facteur qui permet une meilleure compréhension.

Remarque:  
Selon l'objectif l'expérience le même facteur sera fixe ou aléatoire.
Ex : Une expérience qui vise à déterminer qui est le meilleur boulanger va considérer un effet "boulanger" (chaque participant) comme fixe.  
Tandis que si on cherche à savoir si l'effet du boulanger (en général) est important dans la qualité du pain, on considère l'effet "boulanger" aléatoire, car on ne peut pas tester tous les boulangers existants.

Lors de modèle mixte avec tous les effets et interactions possibles, on retire les interactions d'ordre le plus élevé (AnB < AnBnC) en premier pour aboutir à un modèle interprétable.
