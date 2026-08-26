# Validation des données

**Document history**

| Date | Editor(s) | OpenSILEX version | Comment |
|------|-----------|-------------------|---------|
| 26/08/2026 | ARGO | - | Document creation |

---

## 1. Vue d'ensemble

Le module opensilex-sparql implémente deux niveaux de validation des données RDF :

1. **Validation OWL** : basée sur le schéma (domaine, range, cardinalités, restrictions)
2. **Validation SHACL** : basée sur les formes (règles de conformité avancées)

---

## 2. Validation OWL

### 2.1. OwlRestrictionValidator

Classe abstraite (~433 lignes) qui valide les relations RDF contre le schéma OWL.

```java
public abstract class OwlRestrictionValidator<T extends ValidationContext> {
    
    protected final SPARQLService sparql;
    protected final OntologyStore ontologyStore;
    protected int nbError;
    protected int nbErrorLimit;
    
    protected Map<String, Map<String, List<T>>> validationByTypesAndValues;
}
```

### 2.2. Méthodes de validation

| Méthode | Rôle |
|---------|------|
| `validateDataTypePropertyValue()` | Valide une propriété de type données (xsd:string, xsd:integer, etc.) |
| `validateObjectPropertyValue()` | Valide une propriété objet (lien vers une autre ressource) |
| `validateModel()` | Valide un modèle complet |
| `batchValidation()` | Validation par lot (existence de N URI) |

### 2.3. Types d'erreurs

| Erreur | Méthode | Description |
|--------|---------|-------------|
| `UnknownPropertyError` | `addUnknownPropertyError()` | Propriété non liée au type |
| `InvalidURIError` | `addInvalidURIError()` | URI inconnue ou mal formée |
| `InvalidDatatypeError` | `addInvalidDatatypeError()` | Type de donnée incorrect |
| `InvalidValueError` | `addInvalidValueError()` | Valeur invalide (dépend du domaine) |
| `MissingRequiredValue` | `addMissingRequiredValue()` | Propriété requise manquante |

### 2.4. Validation du domaine

Vérifie que la propriété peut s'appliquer au type de la ressource :

```java
// Récupère les propriétés liées au type de la ressource
Set<String> properties = ontologyStore.getOwlRestrictionsUris(
    classURI, 
    includeNestedRestrictions = true
);

// Vérifie que la propriété est dans la liste
boolean isValid = properties.contains(propertyURI.toString());
```

### 2.5. Validation du range

Vérifie que la valeur d'une propriété objet est du type attendu :

```java
// Récupère le range de la propriété
ObjectPropertyModel prop = ontologyStore.getObjectProperty(propertyURI, domain, lang);

// Vérifie que le type de la valeur est compatible
boolean isValid = prop.getRange().contains(valueTypeURI);
```

### 2.6. Validation des cardinalités

Vérifie les contraintes de cardinalité OWL :

```java
// Cardinalité minimale
OwlRestrictionModel minCard = restriction.getMinCardinality();
if (count < minCard.getValue()) {
    addMissingRequiredValue(context);
}

// Cardinalité maximale
OwlRestrictionModel maxCard = restriction.getMaxCardinality();
if (count > maxCard.getValue()) {
    addInvalidValueError(context);
}
```

---

## 3. Validation SHACL

### 3.1. Configuration

SHACL est **optionnel** et doit être activé explicitement :

```yaml
ontologies:
    baseURI: http://opensilex.dev/
    enableSHACL: true    # Par défaut: false
```

### 3.2. Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                       SHACL (utils)                         │
│  ~26.4K de code                                               │
│  - Chargement des shapes                                      │
│  - Exécution des règles                                       │
│  - Collecte des violations                                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                  RDF4J SHACL Engine                         │
│  Moteur de validation SHACL intégré                         │
│  - ShaclSail (RDF4J)                                        │
│  - Validation des shapes                                      │
└─────────────────────────────────────────────────────────────┘
```

### 3.3. Cycle de vie

```
1. Module installation
   └─→ SPARQLModule.install()
       └─→ enableSHACL() / disableSHACL()

2. Pendant l'exécution
   └─→ SPARQLService.executeUpdateQuery()
       └─→ Validation SHACL (si activé)
           └─→ SPARQLValidationException si violation

3. Check
   └─→ SPARQLModule.check()
       └─→ Vérification des ontologies
```

### 3.4. Gestion des erreurs

```java
try {
    sparqlService.enableSHACL();
} catch (SPARQLValidationException ex) {
    LOGGER.warn("Error while enable SHACL validation:");
    LOGGER.warn(ex.getMessage());
} catch (Exception ex) {
    LOGGER.error("Error while initializing SHACL", ex);
}
```

---

## 4. Validation en contexte CSV

### 4.1. CSVValidationModel

Stocke les résultats de validation pour l'import CSV :

```java
public class CSVValidationModel {
    private Map<Integer, List<String>> errorsByLine;  // Ligne → Erreurs
    private int totalErrors;
    private int totalLines;
    private int validLines;
    
    // Accesseurs pour rapport
    public int getTotalErrors() { ... }
    public int getTotalLines() { ... }
    public int getValidLines() { ... }
}
```

### 4.2. CsvCellValidationContext

Contexte de validation pour une cellule individuelle :

```java
public class CsvCellValidationContext {
    private int line;        // Numéro de ligne
    private int column;      // Numéro de colonne
    private String value;    // Valeur de la cellule
    private List<String> errors; // Erreurs détectées
}
```

---

## 5. Validation en contexte API

### 5.1. Validation avant création

```java
// 1. Validation du modèle
OwlRestrictionValidator<ApiValidationContext> validator = ...;
validator.validateModel(classModel, resourceModel, contextSupplier);

// 2. Validation par lot (existence des URI liées)
validator.batchValidation();

// 3. Collecte des erreurs
if (validator.getNbError() > 0) {
    throw new SPARQLValidationException(validator.getErrors());
}

// 4. Création si valide
sparqlService.create(resourceModel);
```

### 5.2. Validation avant mise à jour

```java
// 1. Validation des nouvelles valeurs
validator.validateModel(classModel, newModel, contextSupplier);

// 2. Validation des suppressions
validator.validateDelete(classModel, oldModel, newModel);

// 3. Mise à jour si valide
sparqlService.update(oldModel, newModel);
```

---

## 6. Validation des URI

### 6.1. URIDeserializer

Le deserializer URI (~4.7K) gère la validation des URI :

```java
public class URIDeserializer implements SPARQLDeserializer<URI> {
    
    // Validation du format
    public static boolean validateURI(String uri) {
        try {
            new URI(uri);
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }
    
    // Conversion String → URI
    public URI fromString(String value) {
        return URI.create(value);
    }
    
    // Conversion URI → Node Jena
    public Node getNode(Object value) {
        return NodeFactory.createURI(value.toString());
    }
}
```

### 6.2. Gestion des URI relatives

Les URI relatives sont résolues par rapport au `baseURI` :

```java
// Entrée CSV : "my-germplasm"
// Résolu en : "http://opensilex.dev/id/my-germplasm"
```

---

## 7. Exceptions de validation

| Exception | Raison |
|-----------|--------|
| `SPARQLValidationException` | Échec de validation générale |
| `SPARQLInvalidModelException` | Modèle invalide |
| `SPARQLInvalidClassDefinitionException` | Classe mal définie |
| `SPARQLInvalidURIException` | URI mal formée |
| `SPARQLInvalidUriListException` | Liste d'URI invalide |
| `SPARQLUnknownFieldException` | Champ inconnu |
| `SPARQLMultipleObjectException` | Multiple objets pour propriété singleton |
| `SPARQLNotExistingUriListException` | URI inexistante dans une liste |
| `SPARQLAlreadyExistingUriException` | URI déjà existante |
| `SPARQLAlreadyExistingUriListException` | Liste d'URI déjà existantes |

---

## 8. Bonnes pratiques

### 8.1. Performance

| Recommandation | Raison |
|----------------|--------|
| Utiliser `batchValidation()` pour les vérifications d'existence | Évite N requêtes SPARQL |
| Préférer `OntologyStore` à `OntologyDAO` | Cache RAM vs requêtes SPARQL |
| Limiter le nombre de validations personnalisées | Chaque validation ajoute du temps |

### 8.2. Qualité

| Recommandation | Raison |
|----------------|--------|
| Valider avant de créer | Évite les données invalides |
| Retourner des erreurs détaillées | Aide au débogage |
| Utiliser des messages d'erreur explicites | Compréhension rapide |

### 8.3. SHACL

| Recommandation | Raison |
|----------------|--------|
| Tester SHACL en environnement de dev | SHACL est expérimental |
| Documenter les shapes personnalisées | Maintenance |
| Surveiller les performances | SHACL est coûteux |