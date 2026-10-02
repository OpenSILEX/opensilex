# Vocabulaire pour parler des facteurs

Auteur: Jean-Eudes Hollebecq - Opensilex

## Plans d'expériences

__Avant-propos:__ Il est important de consulter un statisticien lors de la mise en place du plan d'expérience et de la définition des variables et facteurs expérimentaux. Une analyse conduite sur un plan bien conçu sera bien plus facile et performante.

1. plan complet :    
 Tous les niveaux de tous les facteurs sont représentés.
 Ex: Facteur1 à 2 niveaux et facteur2 à 5 niveaux; 2x5 = 10 combinaisons.
2. plan factoriel = idem    
3. plan non-croisé:    
 Contraire d'un plan complet, certaines combinaisons ne sont pas observées
4. plan équilibré :    
 Même nombre d'individus dans chaque classe/cellule du plan
5. plan déséquilibré :    
 Nombre d'individus différents dans chaque classe


L'outil utilisé pour répondre aux questions statistiques posées comme : "est-ce que l'effet du traitement X est significatif sur la production de grain?", est l'ANalyse de VAriance (ANOVA). C'est l'outil le plus communément utilisé pour répondre aux questions statistiques.


## Conséquences en ANOVA

Différents types de somme des carrés sont possibles pour tester les hypothèses. On retrouve les TYPES I, II, III et IV dans la majorité des logiciels statistiques (R, SAS, ...).
Les TYPES I et III sont considérés standards et sont équivalents dans une expérience équilibrée et complète.   
   
Dans un plan non complet ou déséquilibré :
- L'ANOVA de TYPE I sera différente selon l'ordre d'apparition des facteurs.
- L’ANOVA de TYPE III, correspond à la variabilité expliquée par l’apport d’un effet sachant que tous les autres effets sont déjà présents dans le modèle.   
   
Les TYPES II et IV sont plutôt utilisés dans le cas de facteurs hiérarchiques (II) et contenant des valeurs manquantes (IV). Même si dans cette dernière situation il est préférable de procéder à une imputation des valeurs manquantes dédiées.


## Facteurs explicatifs

1. Facteurs à effets fixes :    
 Tous les niveaux du facteur sont représentatifs de la variabilité du facteur. Ce sont les niveaux d'intérêt
2. Facteurs à effets aléatoires :      
 Les niveaux du facteur ne sont pas représentatifs de l'ensemble des niveaux possibles. Il s'agit d'un échantillon tiré au hasard parmi les différents niveaux possibles.
3. Facteurs hiérarchiques :    
 L'interprétation d'un facteur (subordonné) dépend de la valeur prise par un autre facteur. La mesure du facteur (subordonné), se fait conditionnellement aux valeurs prises par un second facteur.    
    
Un facteur peut être soit fixe, soit aléatoire.
Un facteur est fixe lorsque les niveaux étudiés sont les seuls niveaux d'intérêts.
Un facteur est aléatoire lorsque les niveaux étudiés sont un échantillon aléatoire d’une population plus large et que le but de l’étude est de déduire des informations sur cette population globale.
Les facteurs aléatoires permettent également l'analyse de la structure de variance/covariance du facteur qui permet une meilleure compréhension.

__Remarque:__
Selon l'objectif de l'expérience, le même facteur pourra être fixe ou aléatoire. 
Ex : Une expérience qui vise à déterminer qui est le meilleur boulanger va considérer un effet "boulanger" (chaque participant) comme fixe. Tandis que si l’objectif est de savoir si l'effet du boulanger (en général) est important dans la qualité du pain, on considère l'effet "boulanger" aléatoire, car on ne peut pas tester tous les boulangers existants.

Dans le cas de modèles mixtes, avec tous les effets et interactions possibles, on retire les interactions d'ordre le plus élevé (AnB < AnBnC) en premier pour aboutir à un modèle interprétable.
