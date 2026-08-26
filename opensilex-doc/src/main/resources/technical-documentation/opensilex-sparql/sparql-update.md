# Mise à jour des données SPARQL

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)          | OpenSILEX version       | Comment           |
|------------|--------------------|-------------------------|-------------------|
| 18/09/2025 | yvan.roux@inrae.fr | 1.4.9 Explosive Emerald | Document creation |
| 26/08/2026 | ARGO               | -                       | Complete documentation with examples and improvements |

---

## 1. Vue d'ensemble

Ce document décrit le mécanisme de mise à jour des données dans OpenSILEX via SPARQL.

**SPARQLService** est la classe principale d'interaction avec le store RDF.
**SPARQLResourceModel** est la classe de base de tous les modèles.
**SPARQLClassQueryBuilder** génère les requêtes SPARQL.

Dans OpenSILEX, la mise à jour est réalisée par **suppression des anciennes données + insertion des nouvelles**. Lors de la suppression et de la création d'une `SPARQLResourceModel`, il est possible d'ignorer certains triplets (champs) pour préserver des valeurs pendant la mise à jour.

---

## 2. Mécanisme de mise à jour

### 2.1. Flux général

```
SPARQLService.update(oldModel, newModel)
    │
    ├─→ 1. Suppression sélective (DELETE)
    │     └─ SPARQLClassQueryBuilder.getDeleteBuilderForUpdateCases()
    │         ├─ Ignore les champs @IgnoreUpdateIfNull si null
    │         ├─ Ignore publisher et creationDate
    │         └─ Supprime uniquement les champs modifiés
    │
    ├─→ 2. Insertion des nouvelles valeurs (INSERT)
    │     └─ SPARQLClassQueryBuilder.addCreateBuilder()
    │         ├─ Ignore publisher et creationDate
    │         └─ Insère tous les champs non null
    │
    ├─→ 3. Mise à jour des champs @AutoUpdate (récursif)
    │     ├─ SPARQLService.loadOnlyOldNeededInstances()
    │     └─ SPARQLService.updateAutoUpdateFields()
    │
    └─→ 4. Mise à jour des métadonnées
          └─ lastUpdateDate = now()
```

### 2.2. Exemple concret

```java
// Modèle avant
Experiment oldExp = new Experiment();
oldExp.setCode("EXP-001");
oldExp.setDescription(new SPARQLLabel("Old Description", "fr"));
oldExp.setStartDate(OffsetDateTime.now());

// Modèle après
Experiment newExp = new Experiment();
newExp.setCode("EXP-001");
newExp.setDescription(new SPARQLLabel("New Description", "fr"));
newExp.setStartDate(OffsetDateTime.now());

// Mise à jour
sparqlService.update(oldExp, newExp);

// Requêtes générées :
// DELETE { <exp-001> rdfs:label ?oldDesc }
// INSERT { <exp-001> rdfs:label "New Description"^^xsd:string }
```

---

## 3. Propriétés SPARQL

### 3.1. @IgnoreUpdateIfNull

Prserve les valeurs existantes lors de la mise à jour si la nouvelle valeur est null.

```java
@SPARQLProperty(
    ontology = vocabulary.class,
    property = "hasDescription",
    ignoreUpdateIfNull = true
)
protected SPARQLLabel description;

// newExp.setDescription(null);
// → La description existante est préservée
```

Pour plus de détails, voir [sparql-property-annotation.md](sparql-property-annotation.md).

### 3.2. @AutoUpdate

Met à jour automatiquement les ressources liées.

```java
@SPARQLProperty(
    ontology = vocabulary.class,
    property = "hasFactorLevel",
    autoUpdate = true
)
protected List<FactorLevel> factorLevels;

// newExp.setFactorLevels(newLevels);
// → Les FactorLevel sont aussi mis à jour (label, description, ...)
```

Pour plus de détails, voir [sparql-property-annotation.md](sparql-property-annotation.md).

---

## 4. Cas spécifique des métadonnées

Les champs de métadonnées `publisher` (`dc:publisher`) et `creationDate` (`dc:issued`) ne doivent **JAMAIS** être mis à jour.

### 4.1. Mécanisme de protection

Ces deux champs sont ajoutés manuellement à la liste d'ignorance :

```java
// Dans SPARQLService#deleteForUpdate:
ignoreList.add("publisher");
ignoreList.add("creationDate");

// Dans SPARQLService#create:
ignoreList.add("publisher");
ignoreList.add("creationDate");
```

**Résultat :**
- La requête DELETE exclut ces champs → ils ne sont jamais supprimés
- La requête INSERT exclut ces champs → ils ne sont jamais réinsérés

### 4.2. Autres métadonnées

| Champ | Propriété RDF | Comportement |
|-------|---------------|--------------|
| `publisher` | `dc:publisher` | Jamais modifié |
| `publicationDate` | `dc:issued` | Jamais modifié |
| `lastUpdateDate` | `dc:modified` | Mis à jour à chaque modification |

---

## 5. Transactions

Les opérations de mise à jour peuvent être groupées dans une transaction :

```java
sparqlService.startTransaction();
try {
    sparqlService.update(exp1, newExp1);
    sparqlService.update(exp2, newExp2);
    sparqlService.commit();
} catch (Exception e) {
    sparqlService.rollback();
    throw e;
}
```

**Niveaux de transaction :**

```java
private int transactionLevel = 0;

@Override
public void startTransaction() {
    if (transactionLevel == 0) {
        // Début de la première transaction
        connection.startTransaction();
    }
    transactionLevel++;
}

@Override
public void commit() {
    transactionLevel--;
    if (transactionLevel == 0) {
        // Fin de la dernière transaction
        connection.commit();
    }
}
```

---

## 6. Requêtes générées

### 6.1. DELETE sélectif

```sparql
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX vocabulary: <http://www.opensilex.org/vocabulary/>

DELETE {
    GRAPH <http://opensilex.dev/set/experiment> {
        <http://opensilex.dev/id/exp-001> rdfs:label ?oldLabel .
        <http://opensilex.dev/id/exp-001> vocabulary:hasCode ?oldCode .
    }
}
WHERE {
    GRAPH <http://opensilex.dev/set/experiment> {
        <http://opensilex.dev/id/exp-001> rdfs:label ?oldLabel .
        <http://opensilex.dev/id/exp-001> vocabulary:hasCode ?oldCode .
    }
}
```

### 6.2. INSERT

```sparql
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX vocabulary: <http://www.opensilex.org/vocabulary/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

INSERT {
    GRAPH <http://opensilex.dev/set/experiment> {
        <http://opensilex.dev/id/exp-001>
            rdfs:label "New Label" ;
            vocabulary:hasCode "NEW-CODE" ;
            dc:modified "2024-01-15T10:30:00Z"^^xsd:dateTime .
    }
}
WHERE {}
```

---

## 7. Cas spéciaux

### 7.1. Changement de type

```java
// Changement de type d'une ressource
oldExp.setType(oldType);
newExp.setType(newType);

// → L'ancien type est supprimé et le nouveau est ajouté
DELETE { <exp-001> a vocabulary:OldType }
INSERT { <exp-001> a vocabulary:NewType }
```

### 7.2. Modification de liste

```java
// Ajout/Suppression d'éléments dans une liste
oldExp.setFacilities([f1, f2, f3]);
newExp.setFacilities([f2, f3, f4]);

// → f1 supprimé, f4 ajouté, f2 et f3 préservés
```

### 7.3. Relation un-à-un

```java
// Pour les propriétés un-à-un, l'ancienne valeur est remplacée
oldExp.setFacility(f1);
newExp.setFacility(f2);

// → Ancien triplet supprimé, nouveau triplet ajouté
```

---

## 8. Améliorations possibles

### 8.1. Annotation @IgnoreUpdateAlways

Une amélioration proposée serait d'ajouter une annotation dédiée `@IgnoreUpdateAlways` pour les champs qui ne doivent jamais être modifiés (comme `publisher` et `creationDate`). Cela permettrait d'ajouter facilement d'autres champs avec le même comportement à l'avenir.

```java
// Proposition
@SPARQLProperty(
    ontology = DCTerms.class,
    property = "publisher",
    ignoreUpdateAlways = true  // Nouvelle annotation
)
protected URI publisher;
```

### 8.2. Optimisation des @AutoUpdate

Actuellement, chaque mise à jour avec des champs `@AutoUpdate` force le chargement de toutes les instances concernées pour obtenir les anciennes valeurs. Cette approche est très coûteuse lors de mises à jour en masse.

**Pistes d'optimisation :**
- Mise en cache des anciennes valeurs
- Chargement différé des instances @AutoUpdate
- Batch des requêtes de chargement

### 8.3. Merging intelligent

Au lieu de DELETE + INSERT, envisager des opérations de merging :
```sparql
# Proposition : MERGE (si supporté par le store)
MERGE {
    GRAPH <...> {
        <uri> ?p ?o .
    }
}
WHERE {
    GRAPH <...> {
        <uri> ?p ?o .
        FILTER(?p NOT IN (...))
    }
}
```

---

## 9. Bonnes pratiques

| Pratique | Raison |
|----------|--------|
| Utiliser `@IgnoreUpdateIfNull` pour les mises à jour partielles | Évite de charger le modèle complet |
| Éviter `@AutoUpdate` sur les relations circulaires | Prévenir les boucles infinies |
| Utiliser les transactions pour les mises à jour multiples | Atomicité garantie |
| Vérifier les différences avant mise à jour | Réduire le nombre de triplets modifiés |
| Préférer `update()` à `delete()` + `create()` | Préserve les métadonnées |