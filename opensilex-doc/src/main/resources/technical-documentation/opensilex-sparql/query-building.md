# Génération de requêtes SPARQL

**Document history**

| Date | Editor(s) | OpenSILEX version | Comment |
|------|-----------|-------------------|---------|
| 26/08/2026 | ARGO | - | Document creation |

---

## 1. Vue d'ensemble

Les requêtes SPARQL sont générées dynamiquement par `SPARQLClassQueryBuilder` (~1242 lignes) en se basant sur la structure des classes modèles et leurs annotations. Le générateur utilise **Jena QueryBuilder** pour construire les requêtes de manière programmatique.

---

## 2. Architecture du générateur

```
SPARQLClassObjectMapper
    │
    ├─→ SPARQLClassAnalyzer (analyse réflexive de la classe)
    │       ├─ dataProperties (propriétés de type simple)
    │       ├─ objectProperties (propriétés vers d'autres modèles)
    │       ├─ dataPropertiesLists (listes de types simples)
    │       ├─ objectPropertiesLists (listes de modèles)
    │       └─ managedProperties (toutes les propriétés gérées)
    │
    └─→ SPARQLClassQueryBuilder (génération de requêtes)
            ├─ getSelectBuilder()
            ├─ getDeleteBuilder()
            ├─ getDeleteBuilderForUpdateCases()
            └─ addCreateBuilder()
```

---

## 3. Requêtes SELECT

### 3.1. Structure de base

```sparql
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX dc: <http://purl.org/dc/terms/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT DISTINCT ?uri
WHERE {
    GRAPH <http://opensilex.dev/set/experiment> {
        ?uri a vocabulary:Experiment .
        ?uri rdfs:label ?label .
        FILTER(lang(?label) = "fr")
    }
}
```

### 3.2. Gestion des propriétés

Chaque propriété du modèle génère un motif de triple dans la clause WHERE :

```java
// Pour une propriété data simple
selectBuilder.addTriple("?uri", propertyNode, "?propertyVar");

// Pour une propriété objet
selectBuilder.addTriple("?uri", propertyNode, "?objectVar");

// Pour une propriété avec label
selectBuilder.addTriple("?uri", propertyNode, "?labelVar");
selectBuilder.addFilter("langMatches(lang(?labelVar), \"" + lang + "\")");
```

### 3.3. Filtres personnalisés

Le framework supporte des handlers WHERE personnalisés par champ :

```java
Map<String, WhereHandler> customHandlerByFields = new HashMap<>();
customHandlerByFields.put("status", (sb, var) -> {
    sb.addFilter("?status = <http://vocab/status/active>");
});
```

### 3.4. Pagination et tri

```java
// Tri
selectBuilder.addOrderBy(field, Order.ASCENDING);

// Pagination (via wrapper SPARQLService)
// Limit + Offset appliqués au niveau du service
```

---

## 4. Requêtes INSERT (Création)

### 4.1. Structure de base

```sparql
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX dc: <http://purl.org/dc/terms/>

INSERT {
    GRAPH <http://opensilex.dev/set/experiment> {
        <http://opensilex.dev/id/exp-001>
            a vocabulary:Experiment ;
            rdfs:label "My Experiment" ;
            vocabulary:hasFacility <http://opensilex.dev/facility/001> .
    }
}
WHERE {}
```

### 4.2. Champs ignorés

Certains champs ne sont jamais insérés :

| Champ | Raison |
|-------|--------|
| `publisher` | Défini automatiquement, jamais modifié |
| `publicationDate` | Défini automatiquement, jamais modifié |

Ces champs sont ajoutés à la liste d'ignorance dans `SPARQLService#create()`.

---

## 5. Requêtes DELETE (Suppression/Mise à jour)

### 5.1. Suppression complète

```sparql
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

DELETE {
    GRAPH <http://opensilex.dev/set/experiment> {
        <http://opensilex.dev/id/exp-001> ?p ?o .
    }
}
WHERE {
    GRAPH <http://opensilex.dev/set/experiment> {
        <http://opensilex.dev/id/exp-001> ?p ?o .
    }
}
```

### 5.2. Suppression sélective (mise à jour)

Pour les mises à jour, seuls les champs modifiés sont supprimés :

```sparql
DELETE {
    GRAPH <http://opensilex.dev/set/experiment> {
        <http://opensilex.dev/id/exp-001> vocabulary:hasLabel ?oldLabel .
    }
}
WHERE {
    GRAPH <http://opensilex.dev/set/experiment> {
        <http://opensilex.dev/id/exp-001> vocabulary:hasLabel ?oldLabel .
    }
}
```

### 5.3. Champs ignorés lors de la suppression

| Condition | Champ ignoré |
|-----------|--------------|
| `@IgnoreUpdateIfNull` + nouvelle valeur = null | Champ non supprimé |
| Champ = `publisher` | Toujours ignoré |
| Champ = `publicationDate` | Toujours ignoré |

---

## 6. Requêtes ASK

Vérification d'existence :

```sparql
ASK {
    GRAPH <http://opensilex.dev/set/germplasm> {
        <http://opensilex.dev/id/germ-001> a vocabulary:Germplasm .
    }
}
```

Utilisé pour :
- Vérifier l'existence d'une ressource
- Vérifier les restrictions OWL
- Vérifications SHACL

---

## 6. Requêtes CONSTRUCT

Génération de sous-graphes RDF :

```sparql
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

CONSTRUCT {
    ?s ?p ?o .
}
WHERE {
    GRAPH <http://opensilex.dev/set/experiment> {
        ?s ?p ?o .
    }
}
```

---

## 7. Requêtes DESCRIBE

Description complète d'une ressource :

```sparql
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

DESCRIBE <http://opensilex.dev/id/exp-001>
FROM <http://opensilex.dev/set/experiment>
```

---

## 8. Requêtes UPDATE

Opérations de mise à jour atomiques :

```sparql
PREFIX dc: <http://purl.org/dc/terms/>

DELETE {
    GRAPH <http://opensilex.dev/set/experiment> {
        <http://opensilex.dev/id/exp-001> dc:modified ?oldDate .
    }
}
INSERT {
    GRAPH <http://opensilex.dev/set/experiment> {
        <http://opensilex.dev/id/exp-001> dc:modified "2024-01-15T10:30:00Z"^^xsd:dateTime .
    }
}
WHERE {
    GRAPH <http://opensilex.dev/set/experiment> {
        <http://opensilex.dev/id/exp-001> dc:modified ?oldDate .
    }
}
```

---

## 9. Requêtes spéciales

### 9.1. Recherche par pattern

```sparql
SELECT DISTINCT ?uri
WHERE {
    GRAPH <http://opensilex.dev/set/germplasm> {
        ?uri a vocabulary:Germplasm .
        ?uri rdfs:label ?label .
        FILTER(REGEX(?label, "maize", "i"))
    }
}
```

### 9.2. Recherche multi-classe

```sparql
SELECT DISTINCT ?uri
WHERE {
    GRAPH <http://opensilex.dev/set/variable> {
        ?uri a ?type .
        FILTER(?type IN (
            vocabulary:Species,
            vocabulary:Germplasm
        ))
    }
}
```

### 9.3. Recherche multi-graphe

```sparql
SELECT DISTINCT ?uri
WHERE {
    { GRAPH <http://opensilex.dev/set/experiment> { ?uri a vocabulary:Experiment . } }
    UNION
    { GRAPH <http://opensilex.dev/set/factor> { ?uri a vocabulary:Factor . } }
}
```

---

## 10. Variables générées

Le générateur crée des variables SPARQL avec des noms prédictibles :

| Élément | Nom de variable |
|---------|-----------------|
| URI | `uri` |
| Type | `rdfType` |
| Label du type | `rdfTypeName` |
| Propriété objet | `{fieldName}` |
| Nom de l'objet | `_{fieldName}_name` |
| Nom par défaut de l'objet | `_{fieldName}_name_default` |
| Timestamp de l'objet | `_{fieldName}__timestamp` |

---

## 11. Préfixes SPARQL

Les préfixes sont automatiquement ajoutés à toutes les requêtes :

| Préfixe | Namespace |
|---------|-----------|
| `rdfs` | `http://www.w3.org/2000/01/rdf-schema#` |
| `foaf` | `http://xmlns.com/foaf/0.1/` |
| `dc` | `http://purl.org/dc/terms/` |
| `owl` | `http://www.w3.org/2002/07/owl#` |
| `xsd` | `http://www.w3.org/2001/XMLSchema#` |

Les préfixes personnalisés sont ajoutés via `SPARQLConfig.customPrefixes()`.