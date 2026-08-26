# Architecture du module opensilex-sparql

**Document history**

| Date | Editor(s) | OpenSILEX version | Comment |
|------|-----------|-------------------|---------|
| 26/08/2026 | ARGO | - | Document creation - Architecture overview |

---

## 1. Rôle du module

Le module **opensilex-sparql** est la couche d'accès aux données RDF/SPARQL d'OpenSILEX. Il fournit un ORM de type **Object-RDF** qui permet de mapper des classes Java (`SPARQLResourceModel`) sur des triplets RDF stockés dans un store SPARQL (RDF4J).

Ce module est un composant central d'OpenSILEX utilisé par tous les modules qui ont besoin de persister des données sémantiques.

---

## 2. Architecture globale

```
┌─────────────────────────────────────────────────────────────────────┐
│                         OpenSILEX Application                       │
│  (APIs, Frontend, CLI, CSV Import, ...)                             │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        SPARQLService                                │
│  Point d'entrée principal : CRUD, requêtes, transactions            │
│  - create(), update(), delete(), load(), search()                   │
│  - executeSelectQuery(), executeUpdateQuery(), ...                  │
└─────────────────────────────────────────────────────────────────────┘
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
┌─────────────────┐ ┌──────────────┐ ┌──────────────────────┐
│  SPARQLClass    │ │ SPARQLClass  │ │ SPARQLProxy          │
│  ObjectMapper   │ │ QueryBuilder │ │ (lazy-loading)       │
│                 │ │              │ │                      │
│ - Mapping ORM   │ │ - Génération │ │ - ByteBuddy          │
│ - Désérialisation│ │   requêtes   │ │ - Chargement à la    │
│ - Génération URI│ │ - SELECT/    │ │   demande            │
│                 │ │   INSERT/    │ │                      │
│                 │ │   DELETE     │ │                      │
└─────────────────┘ └──────────────┘ └──────────────────────┘
          │              │              │
          ▼              ▼              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     SPARQLClassAnalyzer                             │
│  Analyse réflexive des classes : propriétés, getters, setters       │
└─────────────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   RDF4JConnection                                   │
│  Connexion au store RDF4J (HTTP ou LMDB)                            │
│  - executeAskQuery(), executeDescribeQuery()                        │
│  - executeSelectQuery(), executeUpdateQuery()                       │
└─────────────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    RDF4J Repository (Triple Store)                  │
│  Stockage physique des triplets RDF (named graphs)                  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. Composants principaux

### 3.1. SPARQLService

Classe principale (~2783 lignes) qui centralise toutes les opérations. C'est la porte d'entrée unique pour interagir avec le store RDF.

**Architecture interne :**

```
SPARQLService
    │
    ├─ SPARQLConnection connection  (interface vers RDF4J)
    ├─ Cache<URI, Boolean> generatedUriCache  (Caffeine, 30s, 10000 entrées)
    ├─ String defaultLang  (langue par défaut)
    └─ int transactionLevel  (gestion des transactions imbriquées)
```

**Opérations CRUD :**

| Catégorie | Méthodes |
|-----------|----------|
| **CRUD** | `create()`, `update()`, `delete()`, `load()`, `loadList()` |
| **Requêtes** | `executeSelectQuery()`, `executeAskQuery()`, `executeConstructQuery()`, `executeDescribeQuery()` |
| **Transactions** | `startTransaction()`, `commitTransaction()`, `rollbackTransaction()` |
| **URI** | `generateUniqueURI()`, `getGeneratedURI()` |
| **Graphes** | `describe()`, `getGraphStatement()`, `clearGraph()`, `renameGraph()` |
| **Ontologie** | `loadOntology()`, `enableSHACL()`, `disableSHACL()` |

**Exemple d'utilisation — Création :**

```java
// 1. Création d'une seule ressource
Experiment exp = new Experiment();
exp.setCode("EXP-001");
exp.setName(new SPARQLLabel("Mon Expérience", "fr"));
exp.setStartDate(OffsetDateTime.now());

sparqlService.create(exp);
// → Génère : INSERT { <uri> a vocabulary:Experiment ; rdfs:label "Mon Expérience" ; ... }

// 2. Création en lot avec vérification d'existence
List<Germplasm> germplasms = List.of(germ1, germ2, germ3);
sparqlService.create(germplasms);

// 3. Création avec transaction
sparqlService.startTransaction();
try {
    sparqlService.create(exp1);
    sparqlService.create(exp2);
    sparqlService.commitTransaction();
} catch (Exception e) {
    sparqlService.rollbackTransaction();
    throw e;
}

// 4. Création sans date de publication (pour update)
sparqlService.createForUpdate(instances, graph, parent);
```

**Exemple d'utilisation — Lecture :**

```java
// 1. Lecture par URI
Germplasm germ = sparqlService.loadByURI(Germplasm.class, URI.create("http://opensilex.dev/id/germ-001"), "fr");

// 2. Lecture avec filtre personnalisé
Experiment exp = sparqlService.loadByURI(
    Experiment.class,
    URI.create("http://opensilex.dev/id/exp-001"),
    "fr",
    (select) -> select.addFilter("?startDate > ?minDate")
);

// 3. Lecture par liste d'URI
List<Germplasm> germplasms = sparqlService.getListByURIs(
    Germplasm.class,
    List.of(uri1, uri2, uri3),
    "fr"
);

// 4. Recherche avec pagination
ListWithPagination<Experiment> results = sparqlService.searchList(
    Experiment.class,
    "maize",  // pattern de recherche
    "fr",
    0,        // offset
    20,       // limit
    null      // orderBy
);
```

**Exemple d'utilisation — Mise à jour :**

```java
// 1. Mise à jour simple
Experiment oldExp = sparqlService.loadByURI(Experiment.class, uri, "fr");
oldExp.setName(new SPARQLLabel("Nouveau Nom", "fr"));
sparqlService.update(oldExp);

// 2. Mise à jour avec champs @AutoUpdate
// → Les ressources liées sont aussi mises à jour

// 3. Mise à jour avec champs @IgnoreUpdateIfNull
// → Les champs null ne sont pas supprimés
```

**Exemple d'utilisation — Suppression :**

```java
// 1. Suppression par URI
sparqlService.delete(Experiment.class, URI.create("http://opensilex.dev/id/exp-001"));

// 2. Suppression en lot
List<URI> uris = List.of(uri1, uri2, uri3);
sparqlService.delete(Experiment.class, uris);

// 3. Suppression avec cascade (@CascadeDelete)
// → Les ressources liées sont aussi supprimées
```

**Exemple d'utilisation — Requêtes SPARQL brutes :**

```java
// 1. Requête SELECT
SelectBuilder select = new SelectBuilder();
select.addSelectVariable("?uri");
select.addWhere("?uri a vocabulary:Experiment");
List<SPARQLResult> results = sparqlService.executeSelectQuery(select);

// 2. Requête ASK
AskBuilder ask = new AskBuilder();
ask.addWhere("?uri a vocabulary:Germplasm");
boolean exists = sparqlService.executeAskQuery(ask);

// 3. Requête CONSTRUCT
ConstructBuilder construct = new ConstructBuilder();
construct.addWhere("?s ?p ?o");
List<SPARQLStatement> statements = sparqlService.executeConstructQuery(construct);

// 4. Requête UPDATE
UpdateBuilder update = new UpdateBuilder();
update.addInsertTriple("?uri", RDFS.label, "\"New Label\"");
sparqlService.executeUpdateQuery(update);
```

**Transactions imbriquées :**

```java
// Le système gère les transactions imbriquées via un compteur
sparqlService.startTransaction();  // transactionLevel = 1
sparqlService.startTransaction();  // transactionLevel = 2
// ... opérations ...
sparqlService.commitTransaction(); // transactionLevel = 1 (pas de commit)
sparqlService.commitTransaction(); // transactionLevel = 0 (commit réel)
```

### 3.2. SPARQLClassObjectMapper

Moteur de mapping reflexif (~729 lignes) qui transforme les classes Java en requêtes SPARQL et vice-versa.

**Architecture interne :**

```
SPARQLClassObjectMapper<T>
    │
    ├─ Class<T> objectClass
    ├─ SPARQLClassAnalyzer classAnalyzer  (analyse des champs)
    ├─ SPARQLClassQueryBuilder classQueryBuilder  (génération de requêtes)
    ├─ Constructor<T> constructor  (instanciation)
    ├─ URI baseGraphURI  (graphe par défaut)
    └─ URI generationPrefixURI  (préfixe pour URI)
```

**Méthodes principales :**

| Méthode | Rôle |
|---------|------|
| `init()` | Initialise l'analyzer et le query builder |
| `getSelectBuilder()` | Génère la requête SELECT pour la classe |
| `addCreateBuilder()` | Ajoute les triplets INSERT au UpdateBuilder |
| `createInstance()` | Crée une instance à partir d'un URI |
| `createInstanceList()` | Crée une liste d'instances à partir d'URIs |
| `deserialize()` | Désérialise un SPARQLResult en objet Java |

**Exemple de désérialisation :**

```java
// Dans SPARQLClassObjectMapper.createInstance(Node graph, SPARQLResult result, ...)

// 1. Récupération de l'URI
URI uri = uriDeserializer.fromString(result.getStringValue("uri"));
T instance = createInstance(uri);

// 2. Récupération du type RDF
URI realType = new URI(result.getStringValue("rdfType"));
instance.setType(SPARQLDeserializers.formatURI(realType));

// 3. Désérialisation des propriétés data
for (Field field : classAnalyzer.getDataPropertyFields()) {
    Method setter = classAnalyzer.getSetterFromField(field);
    String strValue = result.getStringValue(field.getName());
    if (strValue != null) {
        Object objValue = SPARQLDeserializers.getForClass(field.getType()).fromString(strValue);
        setter.invoke(instance, objValue);
    }
}

// 4. Désérialisation des propriétés objet (avec proxy lazy-loading)
for (Field field : classAnalyzer.getObjectPropertyFields()) {
    Method setter = classAnalyzer.getSetterFromField(field);
    URI objURI = uriDeserializer.fromString(result.getStringValue(field.getName()));
    
    Class<? extends SPARQLResourceModel> fieldType = (Class<? extends SPARQLResourceModel>) field.getType();
    
    // Création d'un proxy pour le chargement différé
    SPARQLProxyResource<?> proxy = new SPARQLProxyResource<>(
        mapperIndex, propertyGraph, objURI, fieldType, lang, useDefaultGraph, service
    );
    setter.invoke(instance, proxy.getInstance());
}
```

**Exemple de génération de requête SELECT :**

```java
// Dans SPARQLClassObjectMapper.getSelectBuilder()
// Pour une classe Experiment avec les champs : code, name, startDate

SelectBuilder select = new SelectBuilder();
select.setDistinct(true);

// Sélection de l'URI
select.addBind(SPARQLDeserializers.nodeURI("?uri"), URI_VAR);

// Sélection des propriétés
select.addTriple("?uri", vocabulary.hasCode, "?code");
select.addTriple("?uri", RDFS.label, "?name");
select.addTriple("?uri", vocabulary.hasStartDate, "?startDate");

// Filtres sur le graphe
select.from(graph);

// Filtre sur le type
select.addWhere("?uri a vocabulary:Experiment");

// Tri
select.addOrderBy("uri", Order.ASCENDING);

// Résultat :
// SELECT DISTINCT ?uri ?code ?name ?startDate
// WHERE {
//     GRAPH <http://opensilex.dev/set/experiment> {
//         ?uri a vocabulary:Experiment ;
//              vocabulary:hasCode ?code ;
//              rdfs:label ?name ;
//              vocabulary:hasStartDate ?startDate .
//     }
// }
```

### 3.3. SPARQLClassQueryBuilder

Générateur de requêtes SPARQL (~1242 lignes) qui construit les requêtes dynamiquement selon la structure des classes.

**Architecture interne :**

```
SPARQLClassQueryBuilder
    │
    ├─ SPARQLClassAnalyzer analyzer  (analyse de la classe)
    └─ SPARQLClassObjectMapperIndex mapperIndex  (index des mappers)
```

**Variables générées :**

| Élément | Variable SPARQL |
|---------|-----------------|
| URI | `uri` |
| Type RDF | `rdfType` |
| Label du type | `rdfTypeName` |
| Propriété objet | `{fieldName}` |
| Nom de l'objet | `_{fieldName}_name` |
| Nom par défaut | `_{fieldName}_name_default` |
| Timestamp | `_{fieldName}__timestamp` |

**Exemple de génération de requête INSERT :**

```java
// Dans SPARQLClassQueryBuilder.addCreateBuilder()
// Pour un Germplasm avec : code="MAIZE001", name="Mon Maïs", parent=<uri-parent>

UpdateBuilder insert = new UpdateBuilder();
insert.addInsertQuad(
    SPARQLDeserializers.nodeURI("http://opensilex.dev/id/germ-001"),
    RDF.type,
    SPARQLDeserializers.nodeURI("http://opensilex.org/vocabulary/Germplasm")
);
insert.addInsertQuad(
    SPARQLDeserializers.nodeURI("http://opensilex.dev/id/germ-001"),
    vocabulary.hasCode,
    SPARQLDeserializers.nodeString("MAIZE001")
);
insert.addInsertQuad(
    SPARQLDeserializers.nodeURI("http://opensilex.dev/id/germ-001"),
    RDFS.label,
    SPARQLDeserializers.nodeString("Mon Maïs")
);
insert.addInsertQuad(
    SPARQLDeserializers.nodeURI("http://opensilex.dev/id/germ-001"),
    vocabulary.hasParent,
    SPARQLDeserializers.nodeURI("http://opensilex.dev/id/germ-002")
);

// Résultat :
// INSERT {
//     GRAPH <http://opensilex.dev/set/germplasm> {
//         <http://opensilex.dev/id/germ-001>
//             a vocabulary:Germplasm ;
//             vocabulary:hasCode "MAIZE001" ;
//             rdfs:label "Mon Maïs" ;
//             vocabulary:hasParent <http://opensilex.dev/id/germ-002> .
//     }
// }
// WHERE {}
```

**Exemple de génération de requête DELETE sélective :**

```java
// Dans SPARQLClassQueryBuilder.getDeleteBuilderForUpdateCases()
// Pour une mise à jour où seul le nom change

UpdateBuilder delete = new UpdateBuilder();
delete.addDeleteQuad(
    SPARQLDeserializers.nodeURI("http://opensilex.dev/id/germ-001"),
    RDFS.label,
    "?oldLabel"
);

// Résultat :
// DELETE {
//     GRAPH <http://opensilex.dev/set/germplasm> {
//         <http://opensilex.dev/id/germ-001> rdfs:label ?oldLabel .
//     }
// }
// WHERE {
//     GRAPH <http://opensilex.dev/set/germplasm> {
//         <http://opensilex.dev/id/germ-001> rdfs:label ?oldLabel .
//     }
// }
```

### 3.4. SPARQLClassAnalyzer

Analyse réflexive (~814 lignes) d'une classe `SPARQLResourceModel` pour extraire sa structure.

**Architecture interne :**

```
SPARQLClassAnalyzer
    │
    ├─ Map<String, Property> dataProperties  (propriétés de type simple)
    ├─ Map<String, Property> objectProperties  (propriétés vers d'autres modèles)
    ├─ Map<String, Property> dataPropertiesLists  (listes de types simples)
    ├─ Map<String, Property> objectPropertiesLists  (listes de modèles)
    ├─ Map<String, Property> labelProperties  (propriétés de type label)
    ├─ Map<String, Field> fieldsByName  (champs par nom)
    ├─ BiMap<Method, String> fieldsByGetter  (getters par champ)
    ├─ BiMap<Method, String> fieldsBySetter  (setters par champ)
    ├─ List<String> autoUpdateFields  (champs @AutoUpdate)
    ├─ List<String> ignoreUpdateIfNullFields  (champs @IgnoreUpdateIfNull)
    ├─ Map<Class, Set<String>> cascadeDeleteClassesField  (champs @CascadeDelete)
    └─ URIGenerator uriGenerator  (générateur d'URI)
```

**Exemple d'analyse d'une classe :**

```java
// Dans SPARQLClassAnalyzer(SPARQLClassObjectMapperIndex, Germplasm.class)

// 1. Analyse de l'annotation @SPARQLResource
SPARQLResource annotation = ClassUtils.findClassAnnotationRecursivly(
    Germplasm.class, SPARQLResource.class
);
// → ontology = vocabulary.class
// → resource = "Germplasm"
// → rdfTypeURI = http://opensilex.org/vocabulary/Germplasm

// 2. Analyse des champs
ClassUtils.executeOnClassFieldsRecursivly(Germplasm.class, (parentClass, field) -> {
    SPARQLProperty sProperty = field.getAnnotation(SPARQLProperty.class);
    if (sProperty != null) {
        analyzeSPARQLPropertyField(sProperty, field);
    }
});

// Résultat pour Germplasm :
// dataProperties: { code -> vocabulary:hasCode }
// objectProperties: { parent -> vocabulary:hasParent }
// autoUpdateFields: []
// ignoreUpdateIfNullFields: [description]
// cascadeDeleteClassesField: { parent -> Germplasm }
```

**Exemple d'analyse d'un champ :**

```java
// Dans SPARQLClassAnalyzer.analyzeSPARQLPropertyField()
// Pour le champ : @SPARQLProperty(ontology=vocabulary.class, property="hasCode")
//                  protected String code;

SPARQLProperty sProperty = field.getAnnotation(SPARQLProperty.class);
Property property = RDF.createProperty(
    vocabulary.class.getField("hasCode").get(null).getURI()
);

if (field.getType().equals(String.class) || field.getType().equals(Integer.class)) {
    dataProperties.put(field.getName(), property);
} else if (SPARQLResourceModel.class.isAssignableFrom(field.getType())) {
    objectProperties.put(field.getName(), property);
} else if (List.class.isAssignableFrom(field.getType())) {
    Class<?> genericType = getGenericFieldType(field);
    if (SPARQLResourceModel.class.isAssignableFrom(genericType)) {
        objectPropertiesLists.put(field.getName(), property);
    } else {
        dataPropertiesLists.put(field.getName(), property);
    }
}

// Enregistrement des métadonnées
if (sProperty.required()) {
    // Propriété obligatoire
}
if (sProperty.autoUpdate()) {
    autoUpdateFields.add(field.getName());
}
if (sProperty.ignoreUpdateIfNull()) {
    ignoreUpdateIfNullFields.add(field.getName());
}
if (sProperty.cascadeDelete()) {
    cascadeDeleteClassesField.put(field.getType(), field.getName());
}
```

---

## 4. Flux de données détaillés

### 4.1. Création d'une ressource

```
SPARQLService.create(Germplasm germplasm)
    │
    ├─→ 1. prepareInstanceCreation()
    │   ├─ Vérification/génération de l'URI
    │   ├─ Définition du type RDF (vocabulary:Germplasm)
    │   ├─ Définition de publicationDate = now()
    │   └─ Création des ressources imbriquées (nested resources)
    │
    ├─→ 2. SPARQLClassObjectMapper.addCreateBuilder(graph, instance, updateBuilder)
    │   ├─ INSERT du type RDF (rdf:type)
    │   ├─ INSERT des propriétés data (code, label, etc.)
    │   ├─ INSERT des propriétés objet (parent, species, etc.)
    │   └─ INSERT des métadonnées (publisher, publicationDate)
    │       → publisher et publicationDate sont ignorés dans DELETE
    │
    ├─→ 3. SPARQLService.executeUpdateQuery(updateBuilder)
    │   ├─ addPrefixes(updateBuilder)  (ajout des préfixes SPARQL)
    │   └─ connection.executeUpdateQuery(updateBuilder)
    │
    └─→ 4. RDF4JConnection.executeUpdateQuery(query)
        └─ Exécution de la requête INSERT sur le store RDF4J
```

**Exemple concret — Création d'un Germplasm :**

```java
// Code appelant
Germplasm germ = new Germplasm();
germ.setCode("MAIZE001");
germ.setName(new SPARQLLabel("Mon Maïs", "fr"));
germ.setSpecies(URI.create("http://opensilex.org/species/zea-mays"));

sparqlService.create(germ);

// Flux interne :
// 1. prepareInstanceCreation()
//    → germ.setType(URI.create("http://opensilex.org/vocabulary/Germplasm"))
//    → germ.setPublicationDate(OffsetDateTime.now())
//    → germ.setPublisher(currentUser.getUri())
//
// 2. addCreateBuilder()
//    → INSERT { <uri> a vocabulary:Germplasm ;
//                 vocabulary:hasCode "MAIZE001" ;
//                 rdfs:label "Mon Maïs" ;
//                 vocabulary:hasSpecies <...> ;
//                 dc:publisher <...> ;
//                 dc:issued "2024-01-15T10:30:00Z"^^xsd:dateTime . }
//
// 3. executeUpdateQuery()
//    → Ajout des préfixes : PREFIX vocabulary: <http://opensilex.org/vocabulary/>
//    → connection.executeUpdateQuery(query)
```

### 4.2. Lecture d'une ressource

```
SPARQLService.loadByURI(Germplasm.class, URI.create("http://opensilex.dev/id/germ-001"), "fr")
    │
    ├─→ 1. SPARQLClassObjectMapper.getSelectBuilder(graph, lang)
    │   ├─ SELECT ?uri ?code ?name ?species ?startDate
    │   ├─ FROM <http://opensilex.dev/set/germplasm>
    │   └─ WHERE { ?uri a vocabulary:Germplasm ;
    │                vocabulary:hasCode ?code ;
    │                rdfs:label ?name ;
    │                vocabulary:hasSpecies ?species .
    │             FILTER(lang(?name) = "fr") }
    │
    ├─→ 2. SPARQLService.executeSelectQuery(selectBuilder)
    │   ├─ addPrefixes(selectBuilder)
    │   └─ connection.executeSelectQuery(selectBuilder)
    │
    ├─→ 3. SPARQLClassObjectMapper.createInstance(graph, result, lang, service)
    │   ├─ Récupération de l'URI
    │   ├─ Récupération du type RDF
    │   ├─ Désérialisation des propriétés data (code, startDate)
    │   ├─ Création de proxy pour les propriétés objet (species)
    │   └─ Création de proxy pour les labels (name)
    │
    └─→ 4. Retour de l'instance Germplasm
        └─ Les propriétés objet sont chargées à la demande (lazy-loading)
```

**Exemple concret — Lecture avec lazy-loading :**

```java
// Code appelant
Germplasm germ = sparqlService.loadByURI(
    Germplasm.class,
    URI.create("http://opensilex.dev/id/germ-001"),
    "fr"
);

// À ce stade, seul l'URI et les propriétés de base sont chargés
System.out.println(germ.getUri());        // Chargé immédiatement
System.out.println(germ.getCode());       // Chargé immédiatement
System.out.println(germ.getName());       // Chargé immédiatement

// Mais les relations objet sont des proxies
System.out.println(germ.getSpecies());    // Premier appel → requête SPARQL pour charger l'espèce
// → SELECT ?uri ?label WHERE { <uri-species> a vocabulary:Species ; rdfs:label ?label . }

// Appels suivants utilisent le cache
System.out.println(germ.getSpecies());    // Retourne l'instance déjà chargée
```

### 4.3. Mise à jour d'une ressource

```
SPARQLService.update(Germplasm germ)
    │
    ├─→ 1. loadOnlyOldNeededInstances()
    │   ├─ Récupération des instances avec @AutoUpdate fields
    │   └─ Chargement des anciennes valeurs pour comparaison
    │
    ├─→ 2. SPARQLClassObjectMapper.getDeleteBuilderForUpdateCases(oldModel, newModel)
    │   ├─ Comparaison oldModel vs newModel
    │   ├─ Identification des champs modifiés
    │   ├─ Ignorance des champs @IgnoreUpdateIfNull si null
    │   └─ Ignorance de publisher et publicationDate (toujours)
    │   └─ DELETE des triplets modifiés
    │
    ├─→ 3. SPARQLClassObjectMapper.addCreateBuilder(newModel)
    │   ├─ INSERT des nouvelles valeurs
    │   ├─ INSERT de lastUpdateDate = now()
    │   └─ Ignorance de publisher et publicationDate
    │
    ├─→ 4. updateAutoUpdateFields(mapper, oldInstance, instance)
    │   ├─ Identification des champs @AutoUpdate modifiés
    │   ├─ Suppression des anciennes valeurs liées
    │   └─ Récursion sur update() pour chaque instance @AutoUpdate
    │
    └─→ 5. SPARQLService.executeUpdateQuery(deleteBuilder + createBuilder)
```

**Exemple concret — Mise à jour avec @AutoUpdate :**

```java
// Code appelant
Experiment exp = sparqlService.loadByURI(Experiment.class, uri, "fr");
exp.setName(new SPARQLLabel("Nouveau Nom", "fr"));
exp.setFactors(newFactors);  // Liste de Factor avec @AutoUpdate

sparqlService.update(exp);

// Flux interne :
// 1. loadOnlyOldNeededInstances()
//    → Charge les anciens Factor pour comparaison
//
// 2. getDeleteBuilderForUpdateCases()
//    → DELETE { <exp-001> vocabulary:hasName ?oldName }
//    → DELETE { <exp-001> vocabulary:hasFactor ?oldFactor1 }
//    → Ignore dc:publisher et dc:issued
//
// 3. addCreateBuilder()
//    → INSERT { <exp-001> vocabulary:hasName "Nouveau Nom" }
//    → INSERT { <exp-001> vocabulary:hasFactor <new-factor-uri> }
//    → INSERT { <exp-001> dc:modified "2024-01-15T10:30:00Z" }
//
// 4. updateAutoUpdateFields()
//    → Pour chaque Factor dans newFactors :
//       → update(factor)  // Récursion
//       → Met à jour le label, la description, etc.
```

### 4.4. Suppression d'une ressource

```
SPARQLService.delete(Germplasm.class, URI.create("http://opensilex.dev/id/germ-001"))
    │
    ├─→ 1. Vérification des relations @CascadeDelete
    │   └─ Si des ressources dépendent de cette URI, suppression récursive
    │
    ├─→ 2. SPARQLClassObjectMapper.getDeleteBuilder(graph, instance)
    │   ├─ DELETE de tous les triplets du sujet
    │   └─ DELETE de tous les triplets du graphe associé
    │
    └─→ 3. SPARQLService.executeDeleteQuery(deleteBuilder)
```

### 4.5. Recherche avec filtres

```
SPARQLService.searchList(Experiment.class, "maize", "fr", 0, 20, null)
    │
    ├─→ 1. SPARQLClassObjectMapper.getSelectBuilder(graph, lang, filterHandler, customHandler)
    │   ├─ SELECT ?uri ?code ?name
    │   ├─ FROM <http://opensilex.dev/set/experiment>
    │   └─ WHERE { ?uri a vocabulary:Experiment ;
    │                vocabulary:hasCode ?code ;
    │                rdfs:label ?name .
    │             FILTER(REGEX(?name, "maize", "i")) }
    │
    ├─→ 2. SPARQLService.executeSelectQuery(selectBuilder)
    │   └─ connection.executeSelectQuery(selectBuilder)
    │
    ├─→ 3. SPARQLClassObjectMapper.createInstanceList(graph, results, lang, service)
    │   └─ Désérialisation de chaque résultat en Experiment
    │
    └─→ 4. Retour de ListWithPagination<Experiment>
```

---

## 5. Organisation des données

### 5.1. Named Graphs

Chaque concept est stocké dans un named graph séparé :

| Concept | Graph URI |
|---------|-----------|
| Ontologies | `http://www.opensilex.org/vocabulary/<prefixe>` |
| User | `http://<baseURI>/set/user` |
| Experiment | `http://<baseURI>/set/experiment` |
| Germplasm | `http://<baseURI>/set/germplasm` |
| Event | `http://<baseURI>/set/event` |
| ... | ... |

### 5.2. Graphes expérimentaux

Les propriétés spécifiques au contexte expérimental sont stockées dans un graphe dédié par expérience :
- `rdf:type` et `rdfs:label` sont dupliqués dans le graphe global et le graphe expérimental
- Les autres propriétés ne sont stockées que dans le graphe expérimental

---

## 6. Couche de validation

```
┌─────────────────────────────────────────────────────────────┐
│                    OwlRestrictionValidator                  │
│  Validation basée sur le schéma OWL                         │
│  - Domaine/Range des propriétés                             │
│  - Cardinalités                                             │
│  - Restrictions OWL                                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       SHACL                                 │
│  Validation Shape Constraint (optionnel)                    │
│  - Vérification de formes RDF                               │
│  - Règles de conformité avancées                            │
└─────────────────────────────────────────────────────────────┘
```

---

## 7. Extension du module

### 7.1. SPARQLExtension

Interface permettant aux modules OpenSILEX d'ajouter des ontologies personnalisées.

**Implémentation typique dans un module OpenSILEX :**

```java
public class MyModule extends OpenSilexModule implements SPARQLExtension {
    
    @Override
    public void installOntologies(SPARQLService sparql, boolean reset) throws Exception {
        // Chargement des ontologies personnalisées dans le store
        InputStream ontologyStream = getClass().getClassLoader()
            .getResourceAsStream("my-ontology.owl");
        sparql.loadOntology(
            URI.create("http://my.module.org/vocabulary"),
            ontologyStream,
            Lang.OWLXML
        );
    }
    
    @Override
    public void checkOntologies(SPARQLService sparql) {
        // Vérification que les ontologies sont bien installées
        AskBuilder ask = new AskBuilder();
        ask.addWhere(
            "?uri a <http://my.module.org/vocabulary/MyClass>"
        );
        boolean exists = sparql.executeAskQuery(ask);
        if (!exists) {
            throw new SPARQLValidationException("Ontology not installed");
        }
    }
    
    @Override
    public void inMemoryInitialization() {
        // Initialisation en mémoire pour les stores LMDB
    }
}
```

### 7.2. SPARQLDeserializer

Interface pour ajouter des deserializers de types personnalisés. Inscription via SPI (`ServiceLoader`).

**Implémentation typique — Deserializer d'URI :**

```java
public class URIDeserializer implements SPARQLDeserializer<URI> {
    
    @Override
    public URI fromString(String value) throws Exception {
        return URI.create(value);
    }
    
    @Override
    public Node getNode(Object value) throws Exception {
        return NodeFactory.createURI(value.toString());
    }
    
    @Override
    public boolean validate(String value) {
        try {
            new URI(value);
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }
    
    @Override
    public XSDDatatype getDataType() {
        return XSDDatatype.XSDanyURI;
    }
}
```

**Deserializers existants :**

| Deserializer | Type Java | Type RDF |
|--------------|-----------|----------|
| `StringDeserializer` | `String` | `xsd:string` |
| `IntegerDeserializer` | `Integer` | `xsd:integer` |
| `DoubleDeserializer` | `Double` | `xsd:double` |
| `BooleanDeserializer` | `Boolean` | `xsd:boolean` |
| `DateDeserializer` | `Date` | `xsd:date` |
| `DateTimeDeserializer` | `OffsetDateTime` | `xsd:dateTime` |
| `URIDeserializer` | `URI` | `rdf:resource` |
| `EmailDeserializer` | `String` | `xsd:string` (format email) |

**Inscription via ServiceLoader :**

```
META-INF/services/org.opensilex.sparql.deserializer.SPARQLDeserializer
# Contenu : org.opensilex.sparql.deserializer.MyCustomDeserializer
```

### 7.3. SPARQLModule personnalisé

Création d'un module OpenSILEX qui utilise le store SPARQL :

```java
public class MySPARQLModule extends OpenSilexModule {
    
    @Override
    public Class<?> getConfigClass() {
        return MySPARQLConfig.class;
    }
    
    @Override
    public String getConfigId() {
        return "my-module";
    }
    
    @Override
    public void install(boolean reset) throws Exception {
        SPARQLServiceFactory factory = getConfig(SPARQLConfig.class).sparql();
        if (reset) {
            factory.deleteRepository();
        }
        factory.createRepository();
    }
    
    @Override
    public void startup() throws Exception {
        SPARQLService sparql = getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
        
        // Chargement initial des données
        sparql.create(initialData);
        
        sparql.dispose(sparql);
    }
}
```

---

## 8. Dépendances

| Dépendance | Rôle |
|------------|------|
| `opensilex-main` | Core OpenSILEX (services, configuration, modules) |
| `jgrapht-core` | Graphes pour le traversal d'ancêtres (OntologyStore) |
| `jena-arq` | Génération de requêtes SPARQL |
| `rdf4j` | Connexion au store RDF |
| `caffeine` | Cache de génération d'URI |
| `bytebuddy` | Création de proxies dynamiques |
| `univocity-parsers` | Parsing CSV |

---

## 9. Proxy lazy-loading

Le module utilise **ByteBuddy** pour créer des proxies dynamiques qui chargent les données à la demande.

**Architecture du proxy :**

```
SPARQLResourceModel germplasm
    │
    ├─ uri : chargé immédiatement
    ├─ name : chargé immédiatement
    ├─ species : SPARQLProxyResource<Species>
    │   └─ getUri() → premier appel → requête SPARQL → chargement
    │   └─ getUri() → appels suivants → cache
    └─ parents : SPARQLProxyList<Germplasm>
        └─ get(0) → premier appel → requête SPARQL → chargement
        └─ get(0) → appels suivants → cache
```

**Exemple concret de proxy :**

```java
// Dans SPARQLProxyResource.java
abstract class SPARQLProxy<T> implements InvocationHandler {
    
    protected T instance;
    protected boolean loaded = false;
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        loadIfNeeded();  // Chargement à la demande
        return method.invoke(instance, args);
    }
    
    protected T loadIfNeeded() throws Exception {
        if (!loaded) {
            instance = loadData();  // Requête SPARQL
            loaded = true;
        }
        return instance;
    }
    
    protected abstract T loadData() throws Exception;
}
```

**Création du proxy dans SPARQLClassObjectMapper :**

```java
// Dans createInstance(Node graph, SPARQLResult result, ...)
for (Field field : classAnalyzer.getObjectPropertyFields()) {
    URI objURI = uriDeserializer.fromString(result.getStringValue(field.getName()));
    Class<? extends SPARQLResourceModel> fieldType = (Class<? extends SPARQLResourceModel>) field.getType();
    
    // Création du proxy
    SPARQLProxyResource<?> proxy = new SPARQLProxyResource<>(
        mapperIndex, propertyGraph, objURI, fieldType, lang, useDefaultGraph, service
    );
    
    // Invocation du setter avec le proxy (pas l'instance réelle)
    setter.invoke(instance, proxy.getInstance());
}
```

**Résultat pour l'utilisateur :**

```java
Germplasm germ = sparqlService.loadByURI(Germplasm.class, uri, "fr");

// Seule l'URI est chargée immédiatement
System.out.println(germ.getUri());  // Chargé immédiatement

// Les relations objet sont des proxies
System.out.println(germ.getSpecies().getUri());  // Premier appel → requête SPARQL
// → SELECT ?uri ?label WHERE { <uri-species> a vocabulary:Species ; rdfs:label ?label . }

// Appels suivants utilisent le cache
System.out.println(germ.getSpecies().getName());  // Retourne l'instance déjà chargée
```

## 10. Points d'attention

| Point | Impact | Statut |
|-------|--------|--------|
| Taille de SPARQLService (~2783 lignes) | Maintenance difficile | À découper |
| Récursivité @AutoUpdate/@CascadeDelete | Risque de boucle infinie | Documenté, pas protégé |
| NoOntologyStore incomplet | Tests potentiellement faussés | TODO code |
| SHACL expérimental | Fonctionnalité non stable | Configurable |
| SPARQLClassQueryBuilder volumineux | Complexité de maintenance | À refactoriser |