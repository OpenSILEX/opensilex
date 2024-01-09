# Refacto DATA - brouillon

## Points à travailler

- Use cases utilisateurs
    - Entrée
        - quelles DATA les utilisateurs veulent rentrer (nature) / déclaration
        - comment sont formatées les DATA (fichiers) ?
        - quelles contraintes à valider ?
        - quelles performances attendues ?
    - Gestion
        - Besoin de supprimer des DATA ?
        - Quels moyens d'identification des DATA ? (ex. par moment où on les a rentrées)
        - Besoin de modification de DATA ?
    - Sortie : quels types d'interrogation des données ?
- Use cases FAIR
    - Comment partager les DATA (sous forme de dataset par exemple)
    - Comment représenter les DATA pour les rendre FAIR (représentation RDF, vocabulaire)
- Solution technique
    - Quelle représentation RDF ?
    - Quel moyen de stockage répond à nos contraintes ?
    - Quelle représentation, dans ce moyen de stockage, répond aux natures identifiées en entrée ?

## Définitions

- **DATA** ou **observation** (_data_) : une observation expérimentale, une mesure qui peut être réalisée par un humain
  ou un
  outil
  par exemple.
- **Valeur**
- **Type de donnée**
    - **Numérique** (_numerical_) : une valeur numérique, par exemple décimale ou entière. Exemple : 28,03.
    - **Temporel** (_temporal_) : une valeur temporelle. Il peut s'agir d'un **moment** ou d'une **durée**.
        - **Moment** (_moment_) : une zone identifiée dans le temps. Il peut s'agir d'un **instant** ou bien d'une
          **période**.
            - **Instant** (_instant_) : un point identifié dans le temps. Il peut y avoir une précision plus ou moins fine
              (jour, minute, nanoseconde, etc.), et un référentiel absolu ou non (une date ne correspond pas exactement au
              même moment selon la région du monde). Exemple : le 22 janvier 2013 à 16h34.
            - **Période** (_period_) : un intervalle entre deux **instants**. Exemple : entre le 3 et le 7 mars 2018.
        - **Durée** (_duration_) : une quantité de temps. Exemple : 2 jours et 10 heures.

## 1. Use cases utilisateurs

TODO faire un schéma / une représentation hiérarchique / un tableur ??

### 1.1. Déclaration des DATA

J'entends par déclaration la structuration d'une observation selon des concepts existant dans OpenSilex. La
déclaration est un travail conceptuel, nécessaire pour, mais différent de l'écriture, abordé plus bas.

- `w_data` est notre cas d'utilisation central : "Je veux déclarer une observation".

On peut découper les DATA en 5 éléments, desquels on déduit les cas d'utilisation. À noter que 4 des cas d'utilisations
sont des inclusions de `w_data` (donc ces éléments sont requis pour définir une observation), mais la déclaration de la
cible est une extension (la cible n'est donc pas obligatoire dans une observation).

- **Valeur** (notée $Val$) de la mesure
    - `w_data_value` : "Je veux déclarer la valeur de mon observation", inclus par `w_data`.
- **Variable** (notée $Var$) mesurée
    - `w_data_variable` : "Je veux déclarer la variable mesurée par mon observation", inclus par `w_data`.
- **Cible** (notée $T$) de l'observation
    - `w_data_target` : "Je veux déclarer la cible de mon observation", étend `w_data`.
- **Moment** (noté $M$) de la mesure
    - `w_data_moment` : "Je veux déclarer le moment où mon observation a été réalisée", inclus par `w_data`.
- **Provenance** (notée $P$) de l'observation
    - `w_data_provenance` : "Je veux déclarer la provenance de mon observation", inclus par `w_data`.

À noter que `w_data_value` et `w_data_variable` incluent un cas d'utilisation commun :

- `w_data_datatype` : "Je veux déclarer le type de donnée de mon observation".

Déclarer une DATA $D$ revient donc à déclarer un tuple $D = (Val, Var, T, M, P)$, où seul $T$ peut être non définie.

Chaque cas d'utilisation peut être spécialisé en différents cas spécifiques.

- `w_data_value` est spécialisé par :
    - `w_data_value_one_d` : "Je veux déclarer une valeur à une dimension"
    - `w_data_value_multi_d` : "Je veux déclarer une valeur multi-dimensionnelle"
- `w_data_variable` est spécialisé par :
    - `w_data_variable_one_d` : "Je veux déclarer une variable à une dimension"
    - `w_data_variable_multi_d` : "Je veux déclarer une variable multi-dimensionnelle"
- `w_data_datatype` est spécialisé par :
    - `w_data_datatype_numerical` : "Je veux déclarer une observation de type numérique"
    - `w_data_datatype_resource` : "Je veux déclarer une observation liée à une autre ressource, un autre objet par
      exemple"
    - `w_data_datatype_temporal` : "Je veux déclarer une observation de type temporel"
    - `w_data_datatype_qual` : "Je veux déclarer une observation de type qualitatif". Ce cas est spécialisé par :
        - `w_data_datatype_qual_ord` : "Je veux déclarer une observation de type qualitatif ordonné"
        - `w_data_datatype_qual_non_ord` : "Je veux déclarer une observation de type qualitatif non ordonné"
- `w_data_target` est spécialisé par :
    - `w_data_target_single` : "Je veux déclarer une cible unique"
    - `w_data_target_set` : "Je veux déclarer un ensemble de cibles liées"
    - `w_data_target_multiple` : "Je veux déclarer plusieurs cibles indépendantes"
    - `w_data_target_type_object` : "Je veux déclarer un objet scientifique en tant que cible"
    - `w_data_target_type_device` : "Je veux déclarer un appareil en tant que cible"
    - `w_data_target_type_facility` : "Je veux déclarer une installation scientifique en tant que cible"
- `w_data_moment` est spécialisé par :
    - `w_data_instant` : "Je veux déclarer l'instant où mon observation a été réalisée"
    - `w_data_period` : "Je veux déclarer la période où mon observation a été réalisée"

Le cas des provenances est un peu plus complexe.

- `w_data_provenance` est liée aux cas suivants :
    - `w_data_provenance_agent` est inclus par `w_data_provenance` : "Je veux déclarer un agent". Il est spécialisé par :
        - `w_data_provenance_agent_single` : "Je veux déclarer un unique agent"
        - `w_data_provenance_agent_multiple` : "Je veux déclarer plusieurs agents"
        - `w_data_provenance_agent_operator` : "Je veux déclarer un opérateur"
        - `w_data_provenance_agent_device` : "Je veux déclarer un capteur"
    - `w_data_provenance_experiment` étend `w_data_provenance` : "Je veux déclarer l'expérimentation dans le cadre de
      laquelle s'est déroulée l'observation"
    - `w_data_provenance_activity` étend `w_data_provenance` : "Je veux déclarer l'activité qui a permis d'obtenir
      l'observation"

On peut également rajouter le cas d'utilisation de l'annotation.

- `w_data_annotation` étend `w_data` : "Je veux annoter l'observation"

### 1.2. Interrogation

J'entends par interrogation les possibilités de trouver des observations selon certains critères. C'est une notion
conceptuelle qu'on peut voir comme une forme d'identification. L'interrogation est nécessaire pour, mais différente
de la lecture décrite plus bas.

> Les cas d'utilisation suivants sont organisés en hiérarchie de spécialisations. La hiérarchie de la liste à point
suivante reflète la hiérarchie des spécialisations, de telle sorte que le premier point représente le cas le plus
générique.

> Attention, quelques relations ne sont pas des spécialisations, celles-ci sont explicitées entre parenthèses.

- `r_data` est le cas d'utilisation principal : "Je veux trouver des observations"
    - `r_data_value` : "Je veux trouver des observations par leur valeur"
        - `r_data_value_eq` : "Je veux trouver des observations dont la valeur sont égales à un certain paramètre"
        - `r_data_value_diff` : "Je veux trouver des observations dont la valeur est différente d'un certain
          paramètre"
        - `r_data_value_order` : "Je veux trouver des observations dont la valeur respecte une certaine relation
          d'ordre par rapport à un certain paramètre" (exemple : valeurs supérieures à 0). À noter que cela n'est valide
          que pour les types de données ordonnés.
        - `r_data_value_in` : "Je veux trouver des observations dont la valeur est comprise dans un certain
          ensemble"
        - `r_data_multi_d_one` : "Je veux trouver des observations par la valeur d'une de leur dimension"
        - `r_data_value_simple_comp` : "Je veux trouver des observations dont la valeur respecte une unique
          comparaison"
        - `r_data_value_complex_comp` : "Je veux trouver des observations dont la valeur respecte un ensemble de
          comparaisons"
    - `r_data_variable` : "Je veux trouver des observations par leur variable"
        - `r_data_variable_eq` : "Je veux trouver des observations mesurant une variable en particulier"
        - `r_data_variable_in` : "Je veux trouver des observations mesurant une variable parmi un certain ensemble"
    - `r_data_target` : "Je veux trouver des observations par leur cible"
        - `r_data_target_eq` : "Je veux trouver des observations caractérisant une certaine cible". Ce cas d'utilisation
          inclut :
            - `r_data_target_eq_multiple` (inclusion) : "Je veux trouver des observations caractérisant plusieurs cibles
              indépendantes, dont au moins une est une certaine cible"
        - `r_data_target_in` : "Je veux trouver des observations caractérisant une cible parmi un certain ensemble". Ce
          cas d'utilisation inclut :
            - `r_data_target_in_multiple` (inclusion) : "Je veux trouver des observations caractérisant plusieurs cibles
              indépendantes, dont au moins une est incluse dans un certain ensemble"
        - `r_data_target_set_eq` : "Je veux trouver des observations caractérisant un certain ensemble de cibles liées"
        - `r_data_target_set_any` : "Je veux trouver des observations caractérisant un ensemble de cibles liées comprenant
          une certaine cible"
        - `r_data_target_type` : "Je veux trouver des observations caractérisant une cible d'un certain type"
    - `r_data_moment` : "Je veux trouver des observations par le moment de leur mesure"
        - `r_data_moment_order` : "Je veux trouver des observations dont le moment de la mesure respecte une certaine
          relation d'ordre avec un certain instant"
        - `r_data_moment_in` : "Je veux trouver des observations dont le moment de la mesure est compris dans une
          certaine période"
    - `r_data_provenance` : "Je veux trouver des observations par leur provenance"
        - `r_data_provenance_experiment` : "Je veux trouver des observations par l'expérimentation liée"
        - `r_data_provenance_agent` : "Je veux trouver des observations par l'agent ayant effectué la mesure"
            - `r_data_provenance_operator` : "Je veux trouver des observations par l'opérateur ayant effectué la mesure"
            - `r_data_provenance_device` : "Je veux trouver des observations par le capteur ayant effectué la mesure"
                - `r_data_provenance_device_type` : "Je veux trouver des observations par le type de capteur ayant effectué
                  la mesure"
            - `r_data_provenance_agent_eq` : "Je veux trouver des observations dont la mesure a été effectuée par un
              certain agent"
            - `r_data_provenance_agent_in` : "Je veux trouver des observations dont la mesure a été effectuée par un agent
              compris dans un certain ensemble"
    - `r_data_metadata` : "Je veux trouver des observations par leurs métadonnées"
        - `r_data_metadata_publisher` : "Je veux trouver des observations par l'utilisateur les ayant déclarées"
        - `r_data_metadata_publication_date` : "Je veux trouver des observations par la date de leur déclaration"
        - `r_data_metadata_batch` : "Je veux trouver des observations par batch d'observations insérées au même moment"
          (exemple : j'ai inséré tout un fichier CSV de données et je me rends compte d'une erreur ; j'aimerais pouvoir
          retrouver toutes les données insérées via ce fichier pour les vérifier)

### 1.4. Lecture et écriture

> Les cas d'utilisation suivants sont organisés en hiérarchie de spécialisations. La hiérarchie de la liste à point
suivante reflète la hiérarchie des spécialisations, de telle sorte que le premier point représente le cas le plus
générique.

> Attention, quelques relations ne sont pas des spécialisations, celles-ci sont explicitées entre parenthèses.

- `io_data` : "Je veux lire ou écrire des observations"
    - `i_data` : "Je veux écrire des observations"
        - `w_data` (inclusion)
    - `o_data` : "Je veux lire des observations"
        - `r_data` (inclusion)
        - `o_data_visualization` : "Je veux visualiser mes observations"
            - `o_data_visualization_graph` : "Je veux visualiser mes observations sous forme de graphiques"
            - `o_data_visualization_table` : "Je veux visualiser mes observations sous forme de table"
        - `o_data_fair` : "Je veux exporter mes données sous un format FAIR"
    - `io_data_ws` : "Je veux lire ou écrire des observations en faisant appel à des webservices"
    - `io_data_gui` : "Je veux lire ou écrire des observations en passant par une interface graphique"
    - `io_data_format_uniform` (inclusion) : "Je veux écrire des observations en utilisant un format identique à celui
      avec lequel j'exporte des observations"
    - `io_data_format_compatibility` (inclusion) : "Je veux lire ou écrire des observations en passant par un
      format compatible avec mes autres outils"
        - `io_data_format_compatibility_script` : "Je veux lire ou écrire des observations en passant par un
          format facilement utilisable par des scripts"
    - `io_data_format_template` (inclusion) : "Je veux obtenir un gabarit me permettant de déclarer mes observations
      en vue de les écrire"

### 1.3. Gestion

> Les cas d'utilisation suivants sont organisés en hiérarchie de spécialisations. La hiérarchie de la liste à point
suivante reflète la hiérarchie des spécialisations, de telle sorte que le premier point représente le cas le plus
générique.

- `m_data` : "Je veux gérer des observations déjà écrites"
    - `m_data_deletion` : "Je veux supprimer des observations déjà écrites"
    - `m_data_modification` : "Je veux mettre à jour des observations déjà écrites". Je ne suis pas sûr que ce cas
      d'utilisation soit souhaitable.

### 1.5. Non fonctionnel

TODO : validation, performance, sécurité etc.

- `nf_data` : "Je veux que la gestion de mes observations soit ergonomique, performante et sécurisée"
    - `nf_data_sec` (inclusion) : "Je veux que la gestion de mes observations soit sécurisée"
    - `nf_data_perf` (inclusion) : "Je veux que la gestion de mes observations soit performante"
    - `nf_data_ergo` (inclusion) : "Je veux que la gestion de mes observations soit ergonomique"

