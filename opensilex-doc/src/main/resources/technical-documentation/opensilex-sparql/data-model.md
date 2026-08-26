# Modèle de données SPARQL

**Document history**

| Date | Editor(s) | OpenSILEX version | Comment |
|------|-----------|-------------------|---------|
| 26/08/2026 | ARGO | - | Document creation |

---

## 1. Vue d'ensemble

Le module opensilex-sparql utilise un modèle de données basé sur le pattern **Resource-Property-Value** (triplet RDF) exposé via des classes Java. Chaque ressource est représentée par une sous-classe de `SPARQLResourceModel`.

---

## 2. Classes de base

### 2.1. SPARQLResourceModel

Classe de base pour toutes les ressources RDF.

```java
@SPARQLResource(
    ontology = OWL2.class,
    resource = "Class",
    ignoreValidation = true
)
public class SPARQLResourceModel implements SPARQLModel {
    
    // Champs obligatoires
    @SPARQLResourceURI()
    protected URI uri;              // Identifiant unique de la ressource
    
    @SPARQLTypeRDF()
    protected URI rdfType;          // Type RDF (classe OWL)
    
    @SPARQLTypeRDFLabel()
    protected SPARQLLabel rdfTypeName; // Nom du type RDF
    
    // Métadonnées DCMI
    @SPARQLProperty(ontology = DCTerms.class, property = "publisher")
    protected URI publisher;
    
    @SPARQLProperty(ontology = DCTerms.class, property = "issued")
    protected OffsetDateTime publicationDate;
    
    @SPARQLProperty(ontology = DCTerms.class, property = "modified")
    protected OffsetDateTime lastUpdateDate;
    
    // Relations
    protected List<SPARQLModelRelation> relations;
}
```

**Champs et métadonnées :**

| Champ | Propriété RDF | Contrainte |
|-------|---------------|------------|
| `uri` | (subject du triplet) | Obligatoire, unique |
| `rdfType` | `rdf:type` | Obligatoire |
| `rdfTypeName` | `rdfs:label` du type | Facultatif |
| `publisher` | `dc:publisher` | Défini à la création |
| `publicationDate` | `dc:issued` | Défini à la création, jamais modifié |
| `lastUpdateDate` | `dc:modified` | Mis à jour à chaque modification |
| `relations` | - | Liste des relations vers d'autres ressources |

**Comportement des métadonnées :**

- `publisher` et `publicationDate` sont **protégés contre la mise à jour** (ajoutés à la liste d'ignorance lors des suppressions)
- `lastUpdateDate` est mis à jour automatiquement à chaque modification
- `publisher` est défini à partir de l'utilisateur courant

### 2.2. SPARQLNamedResourceModel

Extension de `SPARQLResourceModel` ajoutant un nom textuel.

```java
public class SPARQLNamedResourceModel extends SPARQLResourceModel {
    
    @SPARQLProperty(ontology = RDFS.class, property = "label")
    protected SPARQLLabel name;
    
    @SPARQLProperty(ontology = RDFS.class, property = "comment")
    protected SPARQLLabel description;
}
```

### 2.3. SPARQLModelRelation

Représente une relation entre deux ressources.

```java
public class SPARQLModelRelation {
    private Property property;       // Propriété RDF
    private URI value;               // URI de la valeur
    private InstantModel timestamp;  // Horodatage de la relation
}
```

---

## 3. Modèles de structure

### 3.1. SPARQLLabel

Valeur textuelle avec langue.

```java
public class SPARQLLabel {
    private String value;    // Valeur textuelle
    private String lang;     // Code langue (ex: "fr", "en")
}
```

### 3.2. SPARQLTreeListModel<T>

Arborescence de modèles pour représenter les hiérarchies.

```java
public class SPARQLTreeListModel<T> {
    private T value;
    private List<SPARQLTreeListModel<T>> children;
}
```

### 3.3. SPARQLDagModel

Représentation en graphe orienté acyclique (DAG) pour les relations complexes.

```java
public class SPARQLDagModel {
    private Map<String, List<ResourceDagDTO>> children;
}
```

### 3.4. SPARQLPartialTreeListModel

Version partielle de l'arborescence (pour le chargement différé).

### 3.5. TranslatedModel

Modèle avec support de traduction.

### 3.6. VocabularyModel

Modèle pour les éléments de vocabulaire.

---

## 4. Modèles temporels

Package `org.opensilex.sparql.model.time`

### 4.1. Time

```java
public class Time {
    private OffsetDateTime dateTime;
}
```

### 4.2. InstantModel

```java
public class InstantModel {
    private OffsetDateTime dateTimeStamp;
}
```

---

## 5. Mapping RDF → Java

### 5.1. Principes

Chaque champ d'une sous-classe de `SPARQLResourceModel` est mappé vers une ou plusieurs propriétés RDF via les annotations :

```
Champ Java ──@SPARQLProperty(ontology=X, property="y")──▶ Propriété RDF
```

### 5.2. Types supportés

| Type Java | Type RDF | Deserializer |
|-----------|----------|--------------|
| `String` | `xsd:string` | `StringDeserializer` |
| `Integer` | `xsd:integer` | `IntegerDeserializer` |
| `Long` | `xsd:long` | `LongDeserializer` |
| `Double` | `xsd:double` | `DoubleDeserializer` |
| `Float` | `xsd:float` | `FloatDeserializer` |
| `Boolean` | `xsd:boolean` | `BooleanDeserializer` |
| `Byte` | `xsd:byte` | `ByteDeserializer` |
| `Short` | `xsd:short` | `ShortDeserializer` |
| `Char` | `xsd:string` | `CharDeserializer` |
| `BigInteger` | `xsd:integer` | `BigIntegerDeserializer` |
| `OffsetDateTime` | `xsd:dateTime` | `DateTimeDeserializer` |
| `Date` | `xsd:date` | `DateDeserializer` |
| `URI` | `rdf:resource` | `URIDeserializer` |
| `Email` | `xsd:string` | `EmailDeserializer` |
| `SPARQLLabel` | `rdfs:label` | (spécial) |
| `SPARQLResourceModel` | `rdf:type` | (spécial) |

### 5.3. Collections

Les collections de types simples sont supportées :

```java
@SPARQLProperty(ontology = X.class, property = "y")
protected List<String> myValues;
```

Les collections d'objets complexes (listes de modèles) sont également supportées :

```java
@SPARQLProperty(ontology = X.class, property = "z")
protected List<Germplasm> parents;
```

---

## 6. Proxy lazy-loading

Le module utilise **ByteBuddy** pour créer des proxies dynamiques qui chargent les données à la demande.

```java
// Création du proxy
SPARQLProxy<Germplasm> proxy = new SPARQLProxy<>(mapperIndex, graph, Germplasm.class, lang, service);
Germplasm germplasm = proxy.getInstance();

// Premier appel : chargement des données depuis le store RDF
germplasm.getUri(); // Chargement différé
```

**Mécanisme :**

1. `getInstance()` crée une sous-classe dynamique de type
2. Toutes les méthodes sont interceptées par `SPARQLProxy.invoke()`
3. Au premier appel, `loadIfNeeded()` est exécuté :
   - Requête SPARQL pour charger les données
   - Désérialisation en objet Java
4. Les appels suivants utilisent l'instance déjà chargée

---

## 7. Génération d'URI

Les URI sont générées automatiquement via des `URIGenerator` :

```java
@SPARQLResource(
    ontology = X.class,
    resource = "MyClass",
    uriGenerator = MyURIGenerator.class
)
public class MyClass extends SPARQLResourceModel { ... }
```

**Configuration :**

| Configuration | Rôle |
|---------------|------|
| `baseURI` | URI de base de la plateforme |
| `baseURIAlias` | Alias pour l'URI de base |
| `generationBaseURI` | URI de base pour la génération (défaut : baseURI) |
| `generationBaseURIAlias` | Alias alternatif pour la génération |

**Exemple :**

```yaml
ontologies:
    baseURI: http://www.opensilex.org/
    baseURIAlias: opensilex
    generationBaseURI: http://id.opensilex.org/
    generationBaseURIAlias: id
```

→ URI générée : `http://id.opensilex.org/mon-objet-123`

---

## 8. Exemple complet

```java
@SPARQLResource(
    ontology = vocabulary.class,
    resource = "Germplasm"
)
public class Germplasm extends SPARQLNamedResourceModel {

    @SPARQLProperty(
        ontology = vocabulary.class,
        property = "hasParent",
        cascadeDelete = true,
        autoUpdate = false
    )
    protected List<Germplasm> parents;

    @SPARQLProperty(
        ontology = vocabulary.class,
        property = "hasCode"
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
        property = "hasSpecies"
    )
    protected URI species;
}
```

**Triplets générés :**

```turtle
@prefix vocab: <http://www.opensilex.org/vocabulary/> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .

<http://id.opensilex.org/germplasm-001>
    a vocab:Germplasm ;
    rdfs:label "My Germplasm" ;
    vocab:hasCode "MAIZE001" ;
    vocab:hasParent <http://id.opensilex.org/germplasm-002> ;
    vocab:hasSpecies <http://id.opensilex.org/species-zea-mays> ;
    dc:publisher <http://www.opensilex.org/set/user/agent-1> ;
    dc:issued "2024-01-15T10:30:00Z"^^xsd:dateTime ;
    dc:modified "2024-01-15T10:30:00Z"^^xsd:dateTime .
```