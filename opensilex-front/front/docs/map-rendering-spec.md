# Spécification du Rendu Cartographique

## Vue d'ensemble

Le rendu cartographique utilise **OpenLayers** pour afficher des features géographiques sur une carte.
Les features proviennent de trois sources : objets scientifiques, zones (Areas) et appareils (Devices).

```
MapView.vue
├── MapRenderer.vue          ← Orchestre les composants
│   ├── OlMap.vue            ← Initialise la carte OpenLayers
│   ├── OlLayers.vue         ← Gère les couches de rendu
│   │   ├── ScientificObjectLayer.vue
│   │   ├── AreaLayer.vue
│   │   ├── DeviceLayer.vue
│   │   └── FilterLayer.vue
│   ├── OlOverlays.vue       ← Marqueurs et popups
│   └── OlInteractions.vue   ← Clics, sélection, dessin
├── MapToolbar.vue           ← Boutons de contrôle
├── SelectedFeaturesTable.vue← Tableau des features sélectionnées
└── FeatureDetailsDrawer.vue ← Panneau de détails
```

---

## Architecture des Couches

### Structure de données Feature

Chaque feature suit cette interface TypeScript :

```typescript
interface Feature {
  properties: {
    uri?: string           // Identifiant unique
    name?: string          // Nom affiché
    type?: string          // Type sémantique (ex: "WeatherStation")
    nature?: string        // "Structural", "Temporal", "Device"
    creation_date?: string
    destruction_date?: string
    rdf_type_name?: string
  }
  geometry?: {
    type?: string          // "Point", "Polygon", "LineString"
    coordinates?: unknown  // Coordonnées GeoJSON
  }
}
```

### Pipeline de Rendu

```
Données Vue (Feature[])
    │
    ▼
OlLayers.vue (répartiteur)
    │
    ├── watch sur props.features ──► addFeatures()
    │
    ▼
ScientificObjectLayer / AreaLayer / DeviceLayer
    │
    ├── GeoJSON format ──► readGeometry()
    │   (conversion EPSG:4326 → EPSG:3857)
    │
    ├── Création ol/Feature
    │
    ├── Ajout au VectorSource
    │
    ▼
VectorLayer (OpenLayers)
    │
    └── Rendu par le moteur OpenLayers
```

### Z-Index des Couches

| Z-Index | Couche              | Ordre de rendu |
|---------|---------------------|----------------|
| 0       | AreaLayer           | Arrière        |
| 1       | ScientificObjectLayer (cluster) | Milieu |
| 2       | ScientificObjectLayer (non-point) | Milieu |
| 3       | DeviceLayer         | Avant          |

---

## Géométries et Clustering

### Séparation Point / Non-Point

**Contrainte OpenLayers** : Le `Cluster` source ne gère que les géométries `Point`.

```
ScientificObjectLayer
├── vectorSource (Points) ──► Cluster (distance: 25px)
│   └── Style: cercle + compteur
│
└── nonPointSource (Polygones/Lignes)
    └── Style: rempli + contour
```

### Conversion de Projection

Toutes les géométries sont converties de **EPSG:4326** (WGS84, lat/lon) vers **EPSG:3857** (Web Mercator) :

```typescript
const olGeometry = geoJSONFormat.readGeometry(feature.geometry, {
  dataProjection: 'EPSG:4326',
  featureProjection: 'EPSG:3857',
})
```

---

## Gestion des Événements

### Flux de Clic

```
Clic utilisateur
    │
    ▼
OlInteractions.vue (onMapClick)
    │
    ├── getFeaturesAtPixel(pixel)
    │
    ├── Feature trouvée ?
    │   ├── Oui ──► Extraction des propriétés
    │   │             (uri, name, type, nature, dates)
    │   │
    │   ├── Cluster ?
    │   │   ├── Oui ──► Zoom sur le cluster
    │   │   │           Émission du 1er feature
    │   │   └── Non ──► Sélection unique
    │   │
    │   └── Émission 'select' vers MapRenderer
    │
    └── Aucune feature
        └── Désélection (si pas en mode édition)
```

### Réaction en Cascade

```
OlInteractions.vue
    │ @select
    ▼
MapRenderer.vue
    │ @select ──► onFeatureSelected()
    │             selectedFeatures.value.push(feature)
    │
    ▼
MapView.vue
    │ @select
    │
    ├── SelectedFeaturesTable.vue (affichage)
    ├── FeatureDetailsDrawer.vue (détails)
    └── EventPanelDrawer.vue (timeline)
```

---

## Cycle de Vie des Données

### Chargement Initial

```
1. MapView onMounted()
    │
    ├── API: searchScientificObjectsWithGeometryListByUris()
    │
    ├── transformScientificObjectToFeature()
    │   (extraction du GeoJSON depuis so.location?.geojson)
    │
    └── scientificObjects.value = [...]
```

### Synchronisation avec les Couches

**Problème de race condition** : Les données peuvent arriver avant que la couche ne soit initialisée.

**Solution** : Double-watch dans `OlLayers.vue`

```typescript
// Watch 1 : Données changent
watch([() => props.scientificObjects, () => props.mapInstance], ...)

// Watch 2 : Couche devient ready
watch([() => scientificObjectLayerReady.value, () => props.mapInstance], ...)
```

---

## Contraintes Techniques

### OpenLayers

- **Cluster** : uniquement `Point` geometries
- **Projection** : EPSG:3857 pour le rendu, EPSG:4326 pour les données
- **Performance** : clustering recommandé au-delà de ~100 points

### Vue.js

- **Réactivité** : les mutations de `ref` déclenchent les watches
- **nextTick** : nécessaire après mutation avant d'appeler `addFeatures()`
- **pointer-events: none** : sur les conteneurs de couches pour ne pas bloquer les interactions

### Naive UI

- **NDataTable** : rendu de colonnes via `render(row)` avec `h()` (fonctionnelle, pas JSX)
- **NDrawer** : placement "right" pour les panneaux latéraux

---

## Difficultés Rencontrées

### 1. Données Non-Affichées (Race Condition)

**Symptôme** : Les features chargées avant l'initialisation de la couche ne s'affichent pas.

**Cause** : Le watch sur `props.features` attend `layerReady === true`, mais si les données arrivent avant, le watch ne se re-déclenche pas.

**Solution** : Ajouter un watch sur `layerReady` qui appelle `addFeatures()` si des données existent déjà.

### 2. Propriétés des Features Clusterisées

**Symptôme** : Clic sur un cluster ne retourne pas les propriétés complètes.

**Cause** : Le cluster est une feature OpenLayers qui ne contient pas directement les propriétés.

**Solution** : Récupérer les propriétés depuis le premier feature enfant du cluster :
```typescript
const clusterFeature = clusterFeatures[0]
const uri = clusterFeature.values_?.uri || clusterFeature.get('uri') || clusterFeature.get('properties')?.uri
```

### 3. Géométries Non-Standard

**Symptôme** : Certaines features ne s'affichent pas.

**Cause** : `transformScientificObjectToFeature()` retourne `null` si `geometry` est absent.

**Solution** : Vérifier la présence de `geometry` avant de créer la feature.

---

## Erreurs à Éviter

### ❌ Ne jamais modifier directement `props.features`

Les props sont en lecture seule. Toujours passer par `addFeatures()` dans le composant couche.

### ❌ Ne pas oublier `nextTick()`

Après avoir modifié `selectedFeatures.value`, attendre `nextTick()` avant d'accéder au DOM ou aux refs.

### ❌ Ne pas confondre les projections

- **Données API** : EPSG:4326 (lat/lon)
- **Rendu OpenLayers** : EPSG:3857 (Web Mercator)
- **Toujours utiliser** `dataProjection` et `featureProjection` dans `readGeometry()`

### ❌ Ne pas mixer Point et Polygon dans un Cluster

Le clustering d'OpenLayers plante avec des géométries mixtes. Toujours séparer.

### ❌ Ne pas omettre la désélection

Un clic sur le vide doit vider la sélection (sauf en mode édition).

---

## Bonnes Pratiques

### 1. Séparation des Responsabilités

| Composant | Rôle |
|-----------|------|
| `MapView` | Orchestration, état global |
| `MapRenderer` | Coordination des sous-composants |
| `OlMap` | Initialisation OpenLayers |
| `OlLayers` | Répartition des features vers les couches |
| `*Layer` | Rendu spécifique à un type |
| `OlInteractions` | Gestion des événements |

### 2. Exposition des Méthodes via `defineExpose`

```typescript
defineExpose({
  addFeatures,
  clearFeatures,
  setVisibility,
})
```

Permet l'accès depuis les composants parents via `ref`.

### 3. Gestion d'Erreurs

```typescript
try {
  const response = await service.searchScientificObjectsWithGeometryListByUris(uri)
  // ...
}
catch (error) {
  opensilex!.errorHandler(error)
}
```

---

## Glossaire

| Terme | Définition |
|-------|-----------|
| **Feature** | Entité géographique avec propriétés et géométrie |
| **Layer** | Couche de rendu OpenLayers (VectorLayer) |
| **Source** | Conteneur de features (VectorSource) |
| **Cluster** | Regroupement de points proches en un seul marqueur |
| **Projection** | Système de coordonnées (EPSG:4326, EPSG:3857) |
| **Z-Index** | Ordre d'empilement des couches |
| **Ref** | Référence réactive Vue.js vers un composant enfant |