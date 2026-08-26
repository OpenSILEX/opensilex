# Import CSV

**Document history**

| Date | Editor(s) | OpenSILEX version | Comment |
|------|-----------|-------------------|---------|
| 26/08/2026 | ARGO | - | Document creation |

---

## 1. Vue d'ensemble

Le module opensilex-sparql fournit un système d'import CSV par lots pour charger des données dans le store RDF. L'import combine :
- **Validation syntaxique** : format des cellules, types de données
- **Validation sémantique** : conformité au schéma OWL (domaine, range, cardinalités)
- **Validation SHACL** : règles de forme optionnelles

---

## 2. Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    CsvImporter (interface)                  │
│  - import(File, Publisher)                                  │
│  - validate(File)                                           │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│               AbstractCsvImporter<T>                        │
│  Implémentation abstraite pour les importeurs CSV           │
│  - Lecture par batch                                         │
│  - Validation cellulaire                                     │
│  - Validation sémantique OWL                                 │
│  - Création en masse                                         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              DefaultCsvImporter (exemple)                   │
│  Implémentation concrète pour un type donné                 │
│  - GermplasmCsvImporter                                      │
│  - ExperimentCsvImporter                                     │
│  - FacilityCsvImporter                                       │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Format CSV attendu

### 3.1. Structure de base

Le fichier CSV doit respecter un format spécifique :

```csv
#uri,#type,#label_fr,#label_en,#propriété1,#propriété2
http://opensilex.dev/id/germ-001,germplasm,Mon Germplasm,My Germplasm,code123,species-uri
http://opensilex.dev/id/germ-002,germplasm,Autre Germplasm,Another Germplasm,code456,species-uri-2
```

### 3.2. Colonnes obligatoires

| Index | Nom | Description |
|-------|-----|-------------|
| 0 | `uri` | URI de la ressource (optionnelle, générée si absente) |
| 1 | `type` | Type RDF (classe OWL) |
| 2+ | - | Propriétés du modèle |

### 3.3. En-têtes

Les deux premières lignes sont réservées aux métadonnées :
- Ligne 1 : noms des colonnes
- Ligne 2 : descriptions des colonnes

---

## 4. Configuration

### 4.1. Paramètres

| Paramètre | Valeur par défaut | Description |
|-----------|-------------------|-------------|
| `csvBatchSize` | 4096 | Nombre de lignes traitées par batch |
| `csvMaxErrorNb` | 100 | Nombre maximum d'erreurs avant interruption |

### 4.2. Configuration YAML

```yaml
ontologies:
    baseURI: http://opensilex.dev/
    sparql:
        config:
            serverURI: http://localhost:7200
            repository: opensilex
    csvBatchSize: 2048        # Réduit pour moins de RAM
    csvMaxErrorNb: 50         # Arrêt après 50 erreurs
```

---

## 5. Processus d'import

### 5.1. Flux général

```
1. Ouverture du fichier CSV
2. Lecture de l'en-tête (2 premières lignes)
3. Pour chaque batch de lignes :
   a. Parsing des cellules
   b. Validation syntaxique (types, formats)
   c. Validation sémantique (OWL, SHACL)
   d. Génération des URI si manquantes
   e. Création des ressources RDF
   f. Commit de la transaction
4. Rapport d'import (succès/erreurs)
```

### 5.2. Validation par batch

```
Batch N (lignes 1-4096)
    │
    ├─→ Validation cellulaire
    │     ├─ Format des dates
    │     ├─ Format des URI
    │     ├─ Format des emails
    │     └─ Format des nombres
    │
    ├─→ Validation sémantique
    │     ├─ Domaine des propriétés
    │     ├─ Range des propriétés
    │     ├─ Cardinalités
    │     └─ Existence des URI liées
    │
    ├─→ Validation SHACL (si activé)
    │     └─ Règles de forme
    │
    └─→ Création RDF
          ├─ INSERT des triplets
          └─ Commit transaction
```

---

## 6. Validation

### 6.1. Validation cellulaire

Chaque cellule est validée par un `SPARQLDeserializer` :

| Type | Validation |
|------|------------|
| `String` | Toujours valide |
| `URI` | Format URI valide, namespace optionnel |
| `Date` | Format ISO 8601 |
| `DateTime` | Format ISO 8601 avec timezone |
| `Integer` | Nombre entier |
| `Double` | Nombre décimal |
| `Boolean` | "true"/"false" |
| `Email` | Format email valide |

### 6.2. Validation sémantique

Utilise `OwlRestrictionValidator` pour vérifier :

```java
// Vérification du domaine
propertyModel.getDomain().contains(classModel);

// Vérification du range
propertyModel.getRange().contains(typeURI);

// Vérification de la cardinalité
restriction.getMinCardinality() <= count;
restriction.getMaxCardinality() >= count;

// Vérification de l'existence
ontologyStore.classExist(typeURI, ancestorURI);
```

### 6.3. Validation personnalisée

Le framework supporte des validations personnalisées :

```java
public class MyCsvImporter extends AbstractCsvImporter<MyClass> {
    
    @Override
    protected void init() {
        super.init();
        addCustomValidation(new CustomCsvValidation<MyClass>() {
            @Override
            public void validate(String property, String value, 
                               MyClass model, CsvCellValidationContext ctx) {
                // Validation personnalisée
                if (value.startsWith("INVALID_")) {
                    ctx.addError("Value must not start with 'INVALID_'");
                }
            }
        });
    }
}
```

---

## 7. Gestion des erreurs

### 7.1. Types d'erreurs

| Type | Description |
|------|-------------|
| `SPARQLInvalidUriException` | URI mal formée |
| `SPARQLInvalidModelException` | Modèle invalide |
| `SPARQLValidationException` | Échec de validation |
| `SPARQLUnknownFieldException` | Champ inconnu |
| `SPARQLQueryException` | Erreur requête SPARQL |

### 7.2. Rapport d'erreurs

```java
// CSVValidationModel stocke les erreurs par ligne
public class CSVValidationModel {
    private Map<Integer, List<String>> errorsByLine;  // Ligne → Erreurs
    private int totalErrors;
    private int totalLines;
    private int validLines;
}
```

### 7.3. Comportement en cas d'erreur

| Situation | Comportement |
|-----------|--------------|
| Erreur < csvMaxErrorNb | Continuer l'import |
| Erreur ≥ csvMaxErrorNb | Arrêter l'import |
| Erreur dans un batch | Batch ignoré, suite traitée |

---

## 8. Export CSV

Le module fournit également des outils d'export CSV via le package `csv/export`.

### 8.1. Export de ressources

```java
// Export d'une liste de ressources vers CSV
CsvExporter.export(List<SPARQLResourceModel>, File output);
```

### 8.2. Format de sortie

Les colonnes de sortie correspondent aux colonnes d'entrée :
- URI
- Type
- Propriétés

---

## 9. Bonnes pratiques

### 9.1. Performance

| Recommandation | Impact |
|----------------|--------|
| Ajuster `csvBatchSize` selon la RAM disponible | Moins d'I/O avec des batches plus grands |
| Pré-générer les URI | Évite la génération à la volée |
| Désactiver SHACL pour les gros imports | SHACL est coûteux en calcul |

### 9.2. Qualité des données

| Recommandation | Raison |
|----------------|--------|
| Valider le CSV avant l'import | Détecte les erreurs tôt |
| Utiliser des URI stables | Permet les réimports |
| Vérifier les rapports d'erreurs | Identification des problèmes |

### 9.3. Sécurité

| Recommandation | Raison |
|----------------|--------|
| Limiter la taille des fichiers CSV | Évite les attaques par déni de service |
| Valider les URI d'entrée | Évite les injections RDF |
| Vérifier les namespaces | Respecte la sémantique du vocabulaire |