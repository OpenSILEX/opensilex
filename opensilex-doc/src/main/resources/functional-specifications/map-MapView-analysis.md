# Analyse du composant MapView.vue — Migration Vue 3

> Source : tag `1.5.3` — `opensilex-front/front/src/components/geometry/MapView.vue`
> Taille : 2690 lignes
> Framework : Vue 2 + TypeScript + vue-property-decorator

---

## 1. Bibliothèques utilisées

| Bibliothèque | Version | Rôle | Compatibilité Vue 3 |
|---|---|---|---|
| **vuelayers** | 0.11.35 | Bindings Vue 2 pour OpenLayers | ❌ Non compatible |
| **OpenLayers** (`ol`) | — | Moteur cartographique | ✅ Compatible |
| **bootstrap-vue** | 2.21.2 | `b-modal`, `b-sidebar`, `b-tabs`, `b-button`, `b-alert`, `b-button-group` | ❌ Non compatible |
| **jqx-rangeselector** | 12.1.2 | Sélecteur de plage de dates | ❌ Remplacer |
| **@turf/turf** | 5.1.6 | Analyse géospatiale (unkinkPolygon, polygon) | ✅ Compatible (v6+) |
| **jspdf** | 2.3.1 | Export PDF | ✅ Compatible |
| **file-saver** | 2.0.5 | Téléchargement de fichiers | ✅ Compatible |
| **vuex** | — | State management | ✅ (Pinia recommandé) |
| **vue-i18n** | v7 | i18n (en/fr) | ✅ (v9+ Composition API) |
| **vue-property-decorator** | — | Decorators `@Component`, `@Ref`, `@Watch` | ❌ Remplacer par Composition API |

---

## 2. Fonctionnalités identifiées

### 2.1 Rendu carte OpenLayers

| # | Fonctionnalité | Description |
|---|---|---|
| 1 | **Tuile de base** | OSM (OpenStreetMap) |
| 2 | **Couches vectorielles** | Scientific Objects, Areas, Devices, Filters |
| 3 | **Clustering** | Regroupement des points proches avec compteur |
| 4 | **Overlays** | Tooltip hover (nom/type) + popup détails au clic |
| 5 | **Contrôles** | Zoom, ScaleLine, rotation |
| 6 | **Projections** | EPSG:3857 / EPSG:4326 |

### 2.2 Interactions carte

| # | Fonctionnalité | Description |
|---|---|---|
| 7 | **Drag-box selection** | Sélection multiple par rectangle (Shift+Alt+Click) |
| 8 | **Cluster zoom** | Clic sur cluster → zoom sur les éléments |
| 9 | **Hover tooltip** | Affichage nom/type au survol |
| 10 | **Mode édition** | Dessin de polygon interactif |

### 2.3 Map Panel Sidebar (gauche)

| # | Fonctionnalité | Description |
|---|---|---|
| 11 | **Arbre Scientific Objects** | Arborescence par type RDF, checkbox visibilité |
| 12 | **Arbre Areas** | Structural (vert) + Temporal (rouge), checkbox visibilité |
| 13 | **Arbre Devices** | Arborescence par type, checkbox visibilité |
| 14 | **Filtres géométriques** | Création, couleur stroke/fill, suppression |
| 15 | **Tabs** | 4 onglets (SO, Areas, Devices, Filters) |

### 2.4 Event Panel Sidebar (droite)

| # | Fonctionnalité | Description |
|---|---|---|
| 16 | **Timeline** | Affichage des zones temporaires (tri chronologique) |
| 17 | **Sélection timeline** | Clic sur un événement → sélection sur la carte |

### 2.5 Date Range Selector

| # | Fonctionnalité | Description |
|---|---|---|
| 18 | **Sélecteur de plage** | JqxRangeSelector pour filtrer par période |
| 19 | **Min/Max dates** | Bornées par les dates de l'expérience |
| 20 | **Formatage dynamique** | Ticks adaptatifs (year/month/day/hour) |

### 2.6 Table des éléments sélectionnés

| # | Fonctionnalité | Description |
|---|---|---|
| 21 | **Colonnes** | name, type, actions |
| 22 | **Actions** | Détails, édition, suppression |
| 23 | **Liens URI** | Navigation vers les pages de détails |
| 24 | **Permissions** | Actions conditionnelles selon les droits |

### 2.7 Modals

| # | Modal | Description |
|---|---|---|
| 25 | **Print map** | Export PNG / PDF de la carte |
| 26 | **Area form** | Création/édition de zones |
| 27 | **Scientific Object form** | Édition d'un objet scientifique |
| 28 | **Device form** | Édition d'un dispositif |
| 29 | **Filter form** | Création/modification de filtres géométriques |
| 30 | **Export shape** | Export Shapefile / GeoJSON |
| 31 | **Chart** | Visualisation graphique des données |

### 2.8 Export

| # | Format | Description |
|---|---|---|
| 32 | **PNG** | Capture d'écran de la carte |
| 33 | **PDF** | Carte en format PDF |
| 34 | **Shapefile** | Export géospatial via API backend |
| 35 | **GeoJSON** | Export géospatial via API backend |

### 2.9 Légende

| # | Fonctionnalité | Description |
|---|---|---|
| 36 | **Légende statique** | Scientific Object (bleu), Structural Area (vert), Temporal Area (rouge) |

---

## 3. Structure du composant

```
MapView.vue (2690 lignes)
│
├── Template
│   ├── Modals (7 modals)
│   ├── Toolbar (boutons d'action)
│   ├── Map (OpenLayers + vuelayers)
│   ├── Event Panel Sidebar (droite)
│   ├── Map Panel Sidebar (gauche)
│   ├── Legend
│   ├── Date Range Slider
│   └── Selected Features Table
│
├── Script (TypeScript + Class Components)
│   ├── Imports (vuelayers, ol, bootstrap-vue, jqx, turf, jspdf, file-saver)
│   ├── Data (expérience, features, couches, état UI)
│   ├── Map Methods (multiSelect, defineCenter, zoomRestriction, clustering)
│   ├── Filters Methods (creation, color, display)
│   ├── SO Methods (recovery, update, details)
│   ├── Areas Methods (recovery, creation, memorize)
│   ├── Devices Methods (recovery, details)
│   ├── Save/Export Methods (PNG, PDF, Shape)
│   ├── Table Methods (URI, credentials, delete, edit)
│   ├── Map Panel Methods (visibility, tree view)
│   ├── Date Range Methods (config, format, change)
│   └── Event Panel Methods (timeline selection)
│
├── Styles (SCSS)
│   ├── JqxWidgets CSS imports
│   ├── Sidebar styling
│   ├── Map styling
│   └── Animations
│
└── i18n (en/fr)
    ├── MapView labels
    ├── Area labels
    ├── Filter labels
    ├── ScientificObjects labels
    └── Device labels
```

---

## 4. Méthodes principales (résumé)

| Méthode | Rôle | Lignes |
|---|---|---|
| `created()` | Initialisation services, récupération données | ~40 |
| `retrievesNameOfType()` | Chargement labels types RDF | ~30 |
| `mapCreated()` / `multiSelect()` | Initialisation interactions carte | ~60 |
| `defineCenter()` | Centrage carte sur les features | ~15 |
| `zoomRestriction()` | Gestion zoom (affichage conditionnel) | ~80 |
| `recoveryScientificObjects()` | Récupération SO géolocalisés | ~50 |
| `areasRecovery()` | Récupération areas dans la vue | ~60 |
| `devicesRecovery()` | Récupération devices dans la vue | ~50 |
| `makeClusterStyleFunc()` | Style des clusters | ~25 |
| `manageCluster()` | Zoom sur cluster | ~20 |
| `exportMap()` / `downloadFeatures()` | Export géospatial | ~60 |
| `savePNG()` / `savePDF()` | Export image | ~30 |
| `deleteItem()` | Suppression feature | ~50 |
| `edit()` | Édition feature | ~30 |
| `setVisibility()` / `updateVisibility()` | Toggle visibilité couches | ~40 |
| `onChangeDateRange()` | Changement plage dates | ~10 |
| `configDateRange()` | Configuration min/max dates | ~25 |
| `selectFeaturesFromTimeline()` | Sélection depuis timeline | ~15 |

---

## 5. État et données gérées

### Données de l'expérience
- `experiment` (URI)
- `experimentData` (nom, dates)
- `typeLabel` (labels RDF i18n)

### Features (couches vectorielles)
- `featuresOS` — Scientific Objects (tableau 2D, groupé par type RDF)
- `featuresArea` — Areas (structural + temporal)
- `featuresDevice` — Devices
- `tabLayer` — Filtres utilisateur
- `temporaryArea` — Zone en cours de dessin
- `temporalAreas` — Événements liés aux zones temporaires

### Sélection
- `selectedFeatures` — Features sélectionnées
- `selectedOS` — URIs des SO sélectionnés
- `soWithLabels` — Labels des SO pour le chart

### UI
- `editingMode` — Mode édition activé
- `displayDateRange` — Slider de dates visible
- `timelineSidebarVisibility` — Panel événements ouvert
- `showInstructionMap` — Instructions visibles
- `isDisabled` — Panel désactivé (zoom < 5)

### Configuration carte
- `mapControls` — Contrôles OpenLayers
- `overlayCoordinate` / `centerMap` — Positions des overlays
- `checkZoom` / `opacityOS` — Contrôle visibilité SO

---

## 6. Alternatives à vuelayers pour Vue 3

### Comparaison fonctionnelle

| Fonctionnalité | vue3-openlayers | @vuemap/vue-openlayers | OpenLayers direct | Leaflet-vue |
|---|---|---|---|---|
| Tuile OSM | ✅ | ✅ | ✅ | ✅ |
| Couches vectorielles | ✅ | ✅ | ✅ | ✅ |
| Clustering | ✅ natif | ✅ natif | ✅ natif | ❌ (plugin) |
| Overlays | ✅ | ✅ | ✅ | ✅ |
| Contrôles (zoom, scale) | ✅ | ✅ | ✅ | ✅ |
| Projections (EPSG:3857/4326) | ✅ | ✅ | ✅ | ✅ |
| Drag-box selection | ✅ | ✅ | ✅ | ✅ (plugin) |
| Cluster zoom | ✅ | ✅ | ✅ | ❌ (plugin) |
| Hover tooltip | ✅ | ✅ | ✅ | ✅ |
| Mode édition (draw polygon) | ✅ | ✅ | ✅ | ✅ |
| Export image (PNG/PDF) | ✅ | ✅ | ✅ | ✅ |
| Style dynamique | ✅ | ✅ | ✅ | ✅ |
| Visibilité couches | ✅ | ✅ | ✅ | ✅ |
| Filtres géométriques | ✅ | ✅ | ✅ | ✅ |

### Recommandation

- **vue3-openlayers** ou **@vuemap/vue-openlayers** : API proche de vuelayers, migration la plus directe
- **OpenLayers direct** : Contrôle total, plus robuste, mais ~600 lignes supplémentaires
- **Leaflet-vue** : Plus léger, mais clustering/drag-box nécessitent des plugins

---

## 7. Mapping Bootstrap-Vue / JqxWidgets → Naive UI

| Bootstrap-Vue / Jqx | Naive UI équivalent | Usage dans MapView |
|---|---|---|
| `b-modal` | `NModal` | Modals (print, area, export, chart) |
| `b-sidebar` (left/right) | `NDrawer` + `NDrawerContent` | Map Panel (gauche), Event Panel (droite) |
| `b-tabs` | `NTabs` | Onglets du Map Panel |
| `b-button` | `NButton` | Tous les boutons de la toolbar |
| `b-button-group` | `NButtonGroup` | Actions table |
| `b-alert` | `NAlert` | Instructions map |
| `b-form` | `NForm` + `NFormItem` | Formulaires |
| `b-table` | `NDataTable` | Table sélection |
| Checkbox | `NCheckbox` | Visibilité couches |
| Input color | `NColorPicker` | Couleur filtres |
| Tree view | `NTree` | Arborescence SO, Areas, Devices |
| Collapse | `NCollapse` + `NCollapseItem` | Sections pliables |
| Space | `NSpace` | Espacement |
| Tag | `NTag` | Badges compteur |
| Dropdown | `NDropdown` | Menus contextuels |
| Layout | `NLayout` + `NLayoutSider` | Structure globale |
| Date range picker | `NDatePicker` (`type="daterange"`) | Remplacement JqxRangeSelector |
| Timeline | `NTimeline` | Remplacement composant Timeline perso |

---

## 8. Proposition de split pour Vue 3

```
MapView.vue                           # Orchestrateur (~150 lignes)
│
├── MapRenderer.vue                    # Rendu OpenLayers
│   ├── OlMap.vue                     # Instance OpenLayers + OSM
│   ├── OlLayers.vue                  # Gestion couches vectorielles
│   │   ├── ScientificObjectLayer.vue # SO + clustering
│   │   ├── AreaLayer.vue             # Structural + Temporal
│   │   ├── DeviceLayer.vue           # Points devices
│   │   └── FilterLayer.vue           # Filtres user
│   ├── OlOverlays.vue                # Tooltip + popup détails
│   └── OlInteractions.vue            # DragBox, Select, Draw
│
├── MapToolbar.vue                    # Barre d'outils
│   └── MapToolbarButton.vue          # Bouton générique
│
├── MapPanelDrawer.vue                # NDrawer gauche
│   ├── MapPanelTabs.vue              # NTabs
│   │   ├── SOTreePanel.vue
│   │   ├── AreaTreePanel.vue
│   │   ├── DeviceTreePanel.vue
│   │   └── FilterPanel.vue
│   └── CreateFilterButton.vue
│
├── EventPanelDrawer.vue              # NDrawer droit
│   └── EventTimeline.vue
│
├── DateRangePicker.vue               # NDatePicker daterange
├── SelectedFeaturesTable.vue         # NDataTable
├── MapLegend.vue                     # Légende
├── MapEditor.vue                     # Mode édition
├── MapExportModal.vue                # NModal export
└── ChartModal.vue                    # NModal chart
```

---

## 9. Estimation de la charge

| Composant | Lignes estimées | Complexité |
|---|---|---|
| `MapView.vue` (orchestrateur) | ~150 | Faible |
| `MapRenderer.vue` + sous-composants | ~800 | **Élevée** (OpenLayers) |
| `MapPanelDrawer.vue` + sous-composants | ~400 | Moyenne |
| `EventPanelDrawer.vue` | ~200 | Faible |
| `MapToolbar.vue` | ~150 | Faible |
| `SelectedFeaturesTable.vue` | ~200 | Faible |
| `composables/` | ~500 | Moyenne |
| **Total** | **~2400** | |

---

## 10. Points de vigilance

1. **vuelayers** → Pas de version Vue 3. Wrapper OpenLayers à réécrire (~600-800 lignes)
2. **JqxRangeSelector** → Remplacer par `NDatePicker` ou `NSlider`
3. **Bootstrap-Vue** → Migrer vers Naive UI (déjà dans le projet)
4. **Class components** → Convertir en Composition API (`<script setup>`)
5. **`$bvModal`** → Remplacer par `NModal`
6. **`$refs`** → Utiliser `ref()` de Vue 3
7. **vuex** → Migrer vers Pinia
8. **vue-i18n v7** → Migrer vers v9+ (Composition API)
9. **@turf/turf v5** → Migrer vers v6+
10. **`beforeDestroy`** → Remplacer par `onBeforeUnmount`
11. **`$emit`** → `defineEmits()`
12. **`this.$store`** → Pinia stores