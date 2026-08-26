# Annotations SPARQL Property

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)          | OpenSILEX version       | Comment                                                           |
|------------|--------------------|-------------------------|-------------------------------------------------------------------|
| 18/09/2025 | yvan.roux@inrae.fr | 1.4.9 Explosive Emerald | Document creation                                                 |
| 31/12/2025 | yvan.roux@inrae.fr | 1.4.9 Explosive Emerald | new @IgnoreUpdateIfNull behaviour : changed technical explanation |
| 26/08/2026 | ARGO               | -                       | Complete documentation for all annotations                        |

---

## Vue d'ensemble

Les annotations SPARQL permettent de mapper les champs des classes Java (`SPARQLResourceModel`) vers les propriétés RDF. Elles sont analysées par `SPARQLClassAnalyzer` pour générer les requêtes SPARQL et configurer le comportement du mapping.

---

## @SPARQLResource

**Cible :** Type (classe)

Déclare une classe comme ressource RDF mappable.

```java
@SPARQLResource(
    ontology = vocabulary.class,
    resource = "Germplasm",
    uriGenerator = DefaultURIGenerator.class,
    graph = "",
    prefix = "",
    ignoreValidation = false,
    allowBlankNode = false,
    handleCustomProperties = false
)
public class Germplasm extends SPARQLNamedResourceModel { ... }
```

| Attribut | Type | Défaut | Description |
|----------|------|--------|-------------|
| `ontology` | `Class<?>` | Requis | Classe du vocabulaire OWL (ex: `vocabulary.class`) |
| `resource` | `String` | Requis | Nom de la ressource OWL (ex: `"Germplasm"`) |
| `uriGenerator` | `Class<? extends URIGenerator>` | `DefaultURIGenerator.class` | Classe de génération d'URI |
| `graph` | `String` | `""` | Nom du graphe (vide = graphe par défaut) |
| `prefix` | `String` | `""` | Préfixe URI personnalisé |
| `ignoreValidation` | `boolean` | `false` | Désactive la validation OWL pour cette classe |
| `allowBlankNode` | `boolean` | `false` | Autorise les nodes sans URI (blank nodes) |
| `handleCustomProperties` | `boolean` | `false` | Gère les propriétés personnalisées non déclarées |

---

## @SPARQLProperty

**Cible :** Champ (field)

Mappe un champ Java vers une propriété RDF.

```java
@SPARQLProperty(
    ontology = vocabulary.class,
    property = "hasParent",
    required = false,
    inverse = false,
    ignoreUpdateIfNull = false,
    cascadeDelete = false,
    autoUpdate = false,
    useDefaultGraph = true
)
protected List<Germplasm> parents;
```

| Attribut | Type | Défaut | Description |
|----------|------|--------|-------------|
| `ontology` | `Class<?>` | Requis | Classe du vocabulaire OWL |
| `property` | `String` | Requis | Nom de la propriété RDF |
| `required` | `boolean` | `false` | La propriété est obligatoire |
| `inverse` | `boolean` | `false` | Propriété inverse (réciproque) |
| `ignoreUpdateIfNull` | `boolean` | `false` | Ignore les valeurs null lors de la mise à jour |
| `cascadeDelete` | `boolean` | `false` | Supprime en cascade les ressources liées |
| `autoUpdate` | `boolean` | `false` | Met à jour automatiquement les ressources liées |
| `useDefaultGraph` | `boolean` | `true` | Stocke l'objet dans le graphe par défaut |

### @Required

**Alias de :** `@SPARQLProperty(required = true)`

Marque une propriété comme obligatoire. La validation OWL vérifiera que la propriété est toujours présente.

```java
@SPARQLProperty(
    ontology = vocabulary.class,
    property = "hasCode",
    required = true
)
protected String code;
```

**Comportement :**
- Vérifié lors de la création et de la mise à jour
- Génère une erreur `MissingRequiredValue` si absent

### @Inverse

**Alias de :** `@SPARQLProperty(inverse = true)`

Marque une propriété comme inverse d'une autre propriété. Utilisé pour les relations bidirectionnelles.

```java
// Côté A
@SPARQLProperty(
    ontology = vocabulary.class,
    property = "hasParent",
    inverse = true
)
protected List<Germplasm> parents;

// Côté B
@SPARQLProperty(
    ontology = vocabulary.class,
    property = "hasChild"
)
protected List<Germplasm> children;
```

**Comportement :**
- Lors de la lecture, les deux côtés de la relation sont peuplés
- La propriété inverse est automatiquement déduite

### @IgnoreUpdateIfNull

**Alias de :** `@SPARQLProperty(ignoreUpdateIfNull = true)`

Quand une propriété est null lors d'une mise à jour, les triplets existants ne sont pas supprimés.

**Use case :**
Permet les mises à jour partielles sans supprimer les champs non fournis.

**Technical explanation :**

Le mécanisme initial chargeait le modèle complet avant toute opération pour restaurer les valeurs null. Cette approche a été remplacée par une optimisation : les champs marqués sont ignorés lors de l'opération DELETE.

```java
// Dans SPARQLClassQueryBuilder#getDeleteBuilderForUpdateCases
for each field annotated with @IgnoreUpdateIfNull:
    if new value is null:
        add field to ignore list in getDeleteBuilder()

// Create operation : pas de comportement spécial
// (si new value is null, aucun triplet n'est créé)
```

**Exemple :**

```java
@SPARQLProperty(
    ontology = vocabulary.class,
    property = "hasDescription",
    ignoreUpdateIfNull = true
)
protected SPARQLLabel description;

// Mise à jour avec description = null
// → La description existante est préservée
```

### @AutoUpdate

**Alias de :** `@SPARQLProperty(autoUpdate = true)`

Lors de la mise à jour d'une ressource, les ressources liées via cette propriété sont automatiquement mises à jour (pas seulement l'URI).

> ⚠️ **Warning:** Auto update is recursive. If two resources reference each other with @AutoUpdate fields, this can lead to an infinite loop.

**Use case :**

SPARQLResourceModel a deux types de champs :
- **Data properties** : types simples (String, Integer, etc.)
- **Object properties** : autres SPARQLResourceModel

Quand un champ objet est annoté avec `@AutoUpdate`, la mise à jour de la ressource parente met aussi à jour le contenu des ressources enfants.

**Exemple :**

```java
@SPARQLProperty(
    ontology = vocabulary.class,
    property = "hasFactorLevel",
    autoUpdate = true
)
protected List<FactorLevel> factorLevels;

// Mise à jour du Factor
// → Les FactorLevel associés sont aussi mis à jour (label, description, ...)
```

**Comportement opposé (par défaut) :**

La plupart des champs ne sont **pas** annotés avec `@AutoUpdate`. Par exemple, une Organization a une liste de Facilities, mais mettre à jour l'Organization ne modifie que les URIs des Facilities associées, pas leurs informations (nom, localisation).

**Technical explanation :**

```java
// Dans SPARQLService#update:
1. loadOnlyOldNeededInstances() → fetch instances with @AutoUpdate fields
2. updateAutoUpdateFields() → update all @AutoUpdate fields
3. For each @AutoUpdate field:
   → call SPARQLService#update() recursively
```

**Améliorations possibles :**

Chaque mise à jour avec des champs `@AutoUpdate` force le chargement de toutes les instances concernées, ce qui peut être très coûteux lors de mises à jour en masse.

### @CascadeDelete

**Alias de :** `@SPARQLProperty(cascadeDelete = true)`

Lors de la suppression d'une ressource, les ressources liées via cette propriété sont automatiquement supprimées.

> ⚠️ **Warning:** Cascade delete is recursive. If two resources reference each other with @CascadeDelete fields, this can lead to an infinite loop.

**Use case :**

Supprimer un parent supprime automatiquement les enfants.

```java
@SPARQLProperty(
    ontology = vocabulary.class,
    property = "hasParent",
    cascadeDelete = true
)
protected List<Germplasm> parents;
```

**Comportement :**

```
Delete Germplasm A
    → Delete all parents of A (cascade)
        → Delete all parents of parents (recursive)
```

---

## Autres annotations

### @SPARQLResourceURI

**Cible :** Champ `URI`

Marque le champ contenant l'URI de la ressource.

```java
@SPARQLResourceURI()
protected URI uri;
```

### @SPARQLTypeRDF

**Cible :** Champ `URI`

Marque le champ contenant le type RDF de la ressource.

```java
@SPARQLTypeRDF()
protected URI rdfType;
```

### @SPARQLTypeRDFLabel

**Cible :** Champ `SPARQLLabel`

Marque le champ contenant le label du type RDF.

```java
@SPARQLTypeRDFLabel()
protected SPARQLLabel rdfTypeName;
```

### @SPARQLIgnore

**Cible :** Champ

Ignore un champ lors du mapping RDF.

```java
@SPARQLIgnore()
protected transient String internalField;
```

### @SPARQLManualLoading

**Cible :** Champ

Indique que le champ doit être chargé manuellement (pas de chargement automatique).

```java
@SPARQLManualLoading()
protected List<SPARQLModelRelation> relations;
```

---

## Exemple complet

```java
@SPARQLResource(
    ontology = vocabulary.class,
    resource = "Experiment",
    uriGenerator = ClassURIGenerator.class
)
public class Experiment extends SPARQLNamedResourceModel {

    @SPARQLProperty(
        ontology = vocabulary.class,
        property = "hasCode",
        required = true
    )
    protected String code;

    @SPARQLProperty(
        ontology = vocabulary.class,
        property = "hasDescription",
        ignoreUpdateIfNull = true
    )
    protected SPARQLLabel description;

    @SPARQLProperty(
        ontology = vocabulary.class,
        property = "hasFacility"
    )
    protected List<Facility> facilities;

    @SPARQLProperty(
        ontology = vocabulary.class,
        property = "hasFactor",
        autoUpdate = true
    )
    protected List<Factor> factors;

    @SPARQLProperty(
        ontology = vocabulary.class,
        property = "hasParentExperiment",
        cascadeDelete = true
    )
    protected List<Experiment> parentExperiments;

    @SPARQLProperty(
        ontology = vocabulary.class,
        property = "hasStartDate"
    )
    protected OffsetDateTime startDate;

    @SPARQLIgnore()
    protected transient String internalId;
}
```

---

## Tableau récapitulatif

| Annotation | Cible | Rôle principal |
|------------|-------|----------------|
| `@SPARQLResource` | Classe | Déclare une ressource RDF |
| `@SPARQLProperty` | Champ | Mappe un champ vers une propriété RDF |
| `@SPARQLResourceURI` | Champ URI | Identifiant de la ressource |
| `@SPARQLTypeRDF` | Champ URI | Type RDF |
| `@SPARQLTypeRDFLabel` | Champ Label | Label du type RDF |
| `@SPARQLIgnore` | Champ | Ignore le champ |
| `@SPARQLManualLoading` | Champ | Chargement manuel |

### Attributs de @SPARQLProperty

| Attribut | Effet |
|----------|-------|
| `required = true` | Propriété obligatoire |
| `inverse = true` | Propriété inverse (relation bidirectionnelle) |
| `ignoreUpdateIfNull = true` | Préserve les valeurs existantes lors de la mise à jour si null |
| `autoUpdate = true` | Met à jour récursivement les ressources liées |
| `cascadeDelete = true` | Supprime en cascade les ressources liées |
| `useDefaultGraph = true/false` | Contrôle le graphe de stockage |