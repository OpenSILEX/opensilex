# Spécifications : Métadonnées

**Document history**

| Date | Editor(s) | OpenSILEX version | Comment |
|------|-----------|-------------------|---------|
| 2023-07-13 | Hamza Ikiou | 1.0.1 (develop) | Document original |
| 26/08/2026 | ARGO | - | Mise à jour avec détails techniques |

---

## 1. Besoin

Tous les éléments d'OpenSILEX sont différents et ont des usages variés. Cependant, ils partagent des attributs génériques communs :

- **Le publisher** : personne ayant mis la ressource sur OpenSILEX
- **La date de publication** : date à laquelle la ressource a été mise sur OpenSILEX
- **La date de dernière modification** : date de la dernière modification

Ces informations sont affichées sur la page de détail de tous les éléments d'OpenSILEX sous la forme :
> "Published at {_publication_date_}, by {_publisher_}, modified at {_last_updated_date_}"

---

## 2. Solution

Les modèles génériques sont mis à jour en remplaçant le `creator` par un `publisher` et en ajoutant la date de publication et la date de dernière modification.

Les méthodes génériques de création et de mise à jour sont mises à jour pour définir automatiquement ces deux dates en temps réel.

Tous les modèles sont définis avec l'utilisateur courant comme publisher.

---

## 3. Définitions

| Terme | Description | Référence |
|-------|-------------|-----------|
| **Publisher** | La personne qui a mis la ressource sur OpenSILEX | [DC:Publisher](https://www.dublincore.org/specifications/dublin-core/dcmi-terms/terms/publisher/) |
| **Publication date** | La date à laquelle la ressource a été mise sur OpenSILEX (différent de la date de création) | [DC:Issued](https://www.dublincore.org/specifications/dublin-core/dcmi-terms/terms/issued/) |
| **Date of the last update** | La date à laquelle la ressource a été modifiée pour la dernière fois | [DC:Modified](https://www.dublincore.org/specifications/dublin-core/dcmi-terms/terms/modified/) |

---

## 4. Spécifications techniques

### 4.1. API

#### SPARQLResourceModel

Ajout des trois attributs sur `SPARQLResourceModel` et `MongoModel` :

```java
@SPARQLProperty(ontology = DCTerms.class, property = "publisher")
protected URI publisher;

@SPARQLProperty(ontology = DCTerms.class, property = "issued")
protected OffsetDateTime publicationDate;

@SPARQLProperty(ontology = DCTerms.class, property = "modified")
protected OffsetDateTime lastUpdateDate;
```

#### Comportement des dates

| Opération | publicationDate | lastUpdateDate |
|-----------|-----------------|----------------|
| `create()` | Défini (now) | Non défini |
| `update()` | Conservé (inchangé) | Défini (now) |

#### Types de données

| Modèle | Type des dates |
|--------|----------------|
| `MongoModel` | `Instant` |
| `SPARQLResourceModel` | `OffsetDateTime` |

#### Publisher

- Stocké comme URI dans les deux cas
- Défini à partir de l'utilisateur courant
- Dans les DTO, stocké comme `UserGetDTO`

#### Classes concernées

Toutes les classes héritant de `SPARQLResourceModel` ou `MongoModel` bénéficient de ce mécanisme.

**Exceptions :** Certaines classes (ScientificObject, Device, ...) n'utilisent pas les méthodes génériques de `SPARQLService`. Dans ces cas, les attributs sont définis directement dans leurs classes DAO et API.

### 4.2. MongoDBService

Les méthodes `create()` et `update()` de `MongoDBService` ont été mises à jour pour définir automatiquement les dates.

### 4.3. SPARQLService

La méthode `create()` de `SPARQLService` a été mise à jour pour définir la date de publication.

### 4.4. SPARQLClassObjectMapper

La méthode `updateInstanceFromOldValues()` a été mise à jour pour définir la date de dernière modification.

### 4.5. Front-end

Un nouveau composant `MetadataView` a été ajouté :
- Affiche les trois attributs comme `Prop()`
- Construit la phrase "Published at {_publication_date_}, by {_publisher_}, modified at {_last_updated_date_}"
- Affiché sur toutes les pages de détail d'OpenSILEX
- Affiché uniquement si le publisher n'est pas undefined et non null

---

## 5. Tests

Les métadonnées sont testées dans les classes `SPARQLMetadataTest` et `MongoMetadataTest`. Ces classes ont le même contenu, l'une vérifie les données MongoDB et l'autre les données RDF.

### 5.1. Scénarios de test

| Test | publisher | publicationDate | lastUpdateDate |
|------|-----------|-----------------|----------------|
| `create` | Défini | Défini | Non défini |
| `updateAfterCreate` | Identique | Identique | Défini |
| `updateAfterUpdate` | Identique | Identique | Différent |

### 5.2. Assertions

```java
// create : Le modèle doit avoir un publisher et une date de publication,
//          et ne doit pas avoir de date de dernière modification
assertNotNull(model.getPublisher());
assertNotNull(model.getPublicationDate());
assertNull(model.getLastUpdateDate());

// updateAfterCreate : Le modèle doit avoir la même date de publication
//                     et une date de dernière modification
assertEquals(oldModel.getPublicationDate(), model.getPublicationDate());
assertNotNull(model.getLastUpdateDate());

// updateAfterUpdate : Le modèle doit avoir la même date de publication
//                     et une date de dernière modification différente
assertEquals(oldModel.getPublicationDate(), model.getPublicationDate());
assertNotEquals(oldModel.getLastUpdateDate(), model.getLastUpdateDate());
```

---

## 6. Schéma RDF

```turtle
@prefix dc: <http://purl.org/dc/terms/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

<http://opensilex.dev/id/germplasm-001>
    dc:publisher <http://opensilex.dev/set/user/agent-1> ;
    dc:issued "2024-01-15T10:30:00+01:00"^^xsd:dateTime ;
    dc:modified "2024-01-15T10:30:00+01:00"^^xsd:dateTime .
```

---

## 7. Protection contre la mise à jour

Les champs `publisher` et `publicationDate` sont protégés contre la mise à jour en étant ajoutés à la liste d'ignorance lors des opérations de suppression (voir [sparql-update.md](sparql-update.md)).

```java
// Dans SPARQLService#deleteForUpdate:
ignoreList.add("publisher");
ignoreList.add("publicationDate");

// Dans SPARQLService#create:
ignoreList.add("publisher");
ignoreList.add("publicationDate");
```