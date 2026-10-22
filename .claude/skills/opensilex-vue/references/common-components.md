# Briques partagées de l'hôte (`opensilex-front/front/src/components/common/`)

Inventaire du 2026-10-09. En cas de doute, le fichier fait foi : `graft skeleton <fichier>`.

**Trois règles :**

1. **Réutiliser** ces briques plutôt que d'en écrire une nouvelle.
2. **Ne pas les modifier** sans demande explicite : d'autres listes et vues déjà migrées en dépendent. Si un défaut gêne, le signaler (section « Défauts connus ») et le contourner côté appelant.
3. **Remplacer Bootstrap par Naive UI** vaut pour le balisage que **tu écris**. Plusieurs briques sont encore en Bootstrap à l'intérieur (`Button` et ses dérivés, `Card`, `Modal`) : on les utilise quand même, puisque c'est la brique commune.

Chaque `.vue` de `components/` est enregistré globalement sous `opensilex-<NomDuFichier>`. Dans le code neuf, on l'importe explicitement (`import X from '@/components/common/…/X.vue'`) : c'est la tendance des fichiers récents, et c'est la seule façon de typer une ref de gabarit avec `InstanceType<typeof X>`.

## Libellé et validation : qui enveloppe quoi

C'est la source d'erreur la plus fréquente, d'où ce tableau.

| Composant | Rend son libellé ? | Rend son `n-form-item` ? | Usage dans un `n-form` |
|---|---|---|---|
| `FormField` | oui (div + `*` rouge, aide en tooltip) | **non**, pas de validation | c'est le wrapper des champs ci-dessous |
| `InputForm`, `TextAreaForm`, `DateForm`, `DateTimeForm`, `CheckboxForm`, `TagInputForm`, `IconForm`, `GeometryForm`, `FileInputForm`, `UriForm`, `TypeForm` | oui, via la prop `label` | non | `<n-form-item path="champ">` **sans `label`**, le champ porte `label` |
| `FormSelector` et **tout sélecteur bâti dessus** | oui | **oui** (`path`, `:show-label="false"`) | **ne pas envelopper** : passer `path` et `label` au sélecteur |
| `DateRangePickerForm`, `AddressForm`, `CSVSelectorInputForm` | oui | oui | ne pas envelopper |

Les deux erreurs à éviter :

- un `n-form-item` avec `label` autour d'un champ qui a lui aussi un `label`, ce qui affiche deux libellés ;
- un `n-form-item` autour d'un sélecteur bâti sur `FormSelector`, ce qui donne deux libellés et deux messages de validation.

Les props `rules` et `vid` qu'on voit passées à certains champs sont des restes Vue 2 : `FormField` ne les déclare pas, elles n'ont aucun effet. Les règles se déclarent sur le `n-form` (`:rules`) et s'accrochent par `path`.

**Sélecteurs bâtis sur `FormSelector`** (ne pas envelopper) : `AgentTypeSelector`, `CharacteristicSelector`, `CriteriaOperatorSelector`, `DatafileProvenanceSelector`, `DeviceSelector`, `EntitySelector`, `ExperimentSelector`, `FacilityPropertySelector`, `FacilitySelector`, `FactorLevelSelector`, `FundingSelector`, `GermplasmSelector`, `GroupSelector`, `GroupVariablesSelector`, `InterestEntitySelector`, `MethodSelector`, `OrganizationSelector`, `PersonSelector`, `ProjectSelector`, `ProvenanceSelector`, `ScientificObjectParentPropertySelector`, `ScientificObjectTypeSelector`, `SiteSelector`, `SpeciesSelector`, `UnitSelector`, `VariableDataTypeSelector`, `VariableSelector`, `VariableTimeIntervalSelector`. Pour vérifier un sélecteur absent de cette liste, cherche `<FormSelector` dans son gabarit.

## Listes et filtres

### `TableAsyncView` (`views/TableAsyncView.vue`)

Table paginée côté serveur, basée sur `n-data-table` en mode `remote`. C'est **la** table des pages de liste.

- **props** : `fields: TableField[]` (obligatoire) ; `searchMethod` (obligatoire), appelée avec `{ orderBy: string[], currentPage: number /* base 0 */, pageSize: number }` et qui doit renvoyer `Promise<HttpResponse<OpenSilexResponse<T[]>>>` ; `defaultSortBy` (défaut `'name'`) ; `defaultSortDesc` ; `fieldKeyToSortableModelLabelMap` (clé de colonne → nom du champ triable côté API) ; `isSelectable` ; `selectMode: 'multi' | 'single'` ; `maximumSelectedRows` ; `labelNumberOfSelectedRow` ; `iconNumberOfSelectedRow` ; `defaultPageSize` (20) ; `showCount` (true) ; `showHeaderCount` (true).
- **`TableField`** (`views/TableField.ts`) : `{ key: string; label?: string /* clé i18n */; sortable?: boolean; resizable?: boolean; isSelect?: boolean; naiveProps?: object }`.
- **slots** : `cell(<key>)` avec la portée `{ data: { item, index } }`, qui est la forme héritée de Bootstrap-Vue ; `row-details` (ligne dépliée, si `item._showDetails`) ; `export` ; `selectableTableButtons` (`{ numberOfSelectedRows }`).
- **émet** : `select(item)`, `unselect(item)`, `row-selected(count)`, `refreshed`, `selectall(items)`.
- **expose** : `refresh()`, `getSelected()`, `resetSelection()`, `toggleOnlySelected()`, `setOnlySelected(b)`, `setPage(n)`, `getCurrentPage()`, `getPageSize()`, `getTotalRow()`, `getPaginationInfo()`, `checkSelectedItems(uri)`, `onItemSelected(item)`, `onItemUnselected(item)`.
- La clé de ligne est `row.uri`. Lancer une recherche, c'est appeler `tableRef.value?.refresh()`.
- **Frontière faiblement typée** : `searchMethod: Function` et les payloads `select` / `unselect` sont typés `NamedResourceDTO`. Côté appelant, typer `searchMethod` précisément (paramètres et retour), et typer les gestionnaires avec le vrai DTO de ligne.

### `SearchFiltersSidebar` (`filters/SearchFiltersSidebar.vue`)

Bouton loupe et panneau latéral de filtres (`n-layout-sider`), à placer **dans un `<n-layout has-sider>`**.

- **v-model** : `v-model:filtersCollapsed` (`boolean`).
- **props** : `activeFiltersCount: number` (obligatoire ; le parent le calcule depuis son objet de filtres), `searchButtonLabelTranslationKey` (défaut `searchfilter.label`).
- **émet** : `refresh` (bouton Rechercher ou touche Entrée), `reset` (bouton Effacer).
- **slot par défaut** : les champs de filtre. Le slot est déjà dans un `n-form` (`label-placement="top"`, `size="small"`), donc on y met des `n-form-item` ou des sélecteurs bâtis sur `FormSelector`, sans les envelopper.
- Les boutons Rechercher et Effacer sont fournis : ne pas en ajouter d'autres.

### `StringFilter` (`filters/StringFilter.vue`)

Champ texte de filtre avec bouton d'effacement : `v-model:filter`, `placeholder` (clé i18n, obligatoire), `label?`, `type` (`'text'`), `min` / `max` (pour `number`). Il émet `update`, `handlingEnterKey`. Il est encore en Bootstrap à l'intérieur (`input-group`). Dans un filtre neuf, un `n-input` dans un `n-form-item` convient aussi.

### `TableView` (`views/TableView.vue`)

Table **côté client** (données déjà chargées) : `items`, `fields: TableField[]`, `withPagination`, `globalFilterField`. Slot `cell(<key>)` avec la portée `{ data: { item } }`. Ici `field.label` n'est **pas** traduit : l'appelant le passe traduit. Pour une petite table statique, `n-data-table` avec `DataTableColumns<T>` est souvent plus simple.

### `TreeView` / `TreeViewAsync` (`views/`)

Arbres `n-tree`. `TreeView<T extends TreeViewOption>` est typé ; il émet `select(T[])`, `toggle(T[])`, avec les slots `node`, `buttons`. `TreeViewAsync` charge à la demande via `searchMethod(parentUri, page, pageSize)` et `searchMethodRoot`, avec `v-model:selection` ; il expose `refresh()`, `reloadNodeChildren(key)`.

## Formulaires

- **`Modal`** (`views/Modal.vue`) : props `modalSize: 'sm' | 'lg' | 'xl'`, `hideHeader`, `hideFooter` ; slots `header`, défaut, `footer` ; expose `show()`, `hide()`. C'est la modale attendue par `useModalFormLogic`. Elle **n'émet rien** à la fermeture par la croix.
- **`useModalFormLogic<T>`** (`@/composables/useModalFormLogic`) :
  - **options** : `{ modalRef, nFormRef, getEmptyForm, create, update, props, emit, reset?, onHide?, successMessage?, overrideSuccessMessage? }` ;
  - **renvoie** : `{ form, isEditMode, showCreateForm, showEditForm, hide, submit, formTitle }` ;
  - **types exportés** : `ModalFormEmits` (`onCreate`, `onUpdate`, `onSuccess`) et `ModalFormProps` (`createTitle`, `editTitle`) ;
  - le composable valide le `n-form`, gère le loader et le toast, émet, puis ferme. Il s'appelle dans `setup`.
- **`FormHeader`** : `title` (affiché tel quel, donc à passer **déjà traduit**), `icon`.
- **`FormFooter`** : émet `cancel` et `submit` (pas `save`) ; libellés `cancelLabel` / `submitLabel` (clés i18n).
- **`WizardForm`** : assistant multi-étapes (`steps: WizardStep[]`, `initForm`, `createAction` / `updateAction`), expose `showCreateForm()` / `showEditForm(form)` / `close()`.
- **Règles de validation** (`@/models/FormFieldsFormatter`) : `requiredTrimmed(labelKey)`, `required(labelKey)`, `requiredNotEmpty(labelKey)`, `validEmail()`, `validUri(message)`. Elles renvoient des `FormItemRule` naive-ui et appellent `useI18n()`, donc on les construit **dans `setup`**, typiquement dans un `computed<FormRules>`.
- **Conversion** : `DTOConverter.extractURIFromResourceProperties(getDto)` (`@/models/DTOConverter`) transforme un *GetDTO* en *Creation/UpdateDTO* (les `NamedResourceDTO` deviennent des URI).

## Champs (`forms/`)

| Composant | v-model | Notes |
|---|---|---|
| `InputForm` | `value` (`string \| null`) | `type: 'text' \| 'password' \| 'textarea' \| 'email' \| 'number' \| 'date'` ; émet aussi `input`, `change`, `blur`, `handlingEnterKey` |
| `TextAreaForm` | `value` | |
| `CheckboxForm` | `value` (`boolean`) | `title` : texte à côté de la case |
| `DateForm` | `value` (`yyyy-MM-dd`) | `minDate`, `maxDate` |
| `DateTimeForm` | `value` (ISO UTC) | émet `clear` |
| `DateRangePickerForm` | `start`, `end` | rend ses `n-form-item` ; voir défauts connus |
| `DateTimeRangeForm` | `startDate`, `endDate`, `isInstant` | libellés figés (`component.events.*`) |
| `TagInputForm` | `value` (`string[]`) | |
| `TypeForm` | `type` | `baseType` (URI de la classe racine, obligatoire), `multiple`, `ignoreRoot` |
| `UriForm` | `uri`, `generated` | `editMode` ; **`v-model:uri`**, jamais `:uri.sync` (syntaxe Vue 2 cassée, encore présente dans 6 formulaires) |
| `IconForm` | `value` | icônes `fa#…` |
| `GeometryForm` | `value` (GeoJSON) | saisie WKT |
| `FileInputForm` | `file` (`File`) | `accept` |
| `SwitchGroup` | `value` (`string[]`) | `options: SwitchOption[]` (type exporté) |
| `AddressForm` | `address` (`FacilityAddressDTO`) | rend ses `n-form-item` |
| `CustomTreeselect` | `selected` | moteur de `FormSelector` ; ne pas l'utiliser directement |

## Boutons (`buttons/`)

Ils sont en Bootstrap à l'intérieur, mais ce sont **les** boutons d'action de la plateforme. Pour éditer, supprimer, créer ou voir le détail d'une ressource, on les utilise plutôt qu'un `n-button` nu.

| Composant | Props | Émet |
|---|---|---|
| `Button` | `label?` (clé i18n), `variant?` (→ `btn-<variant>`), `icon?` (`fa#…` ou `bi-…`), `small?`, `disabled?`, `helpMessage?` | `click` |
| `CreateButton` | `label` | `click` |
| `EditButton` | `label` | `click` |
| `DeleteButton` | `label`, `small` (obligatoire) | `click` **après confirmation** : ne pas redemander confirmation |
| `DetailButton` | `label`, `detailVisible?`, `automaticToogle?` | `click` |
| `AddChildButton`, `HelpButton`, `InteroperabilityButton` | `label` | `click` |
| `DeprecatedButton` | `deprecated` | `click` |
| `FavoriteButton` | `uri` | gère seul l'appel d'API |

`small: true` **masque le libellé**, qui ne reste qu'en infobulle : c'est le mode icône seule des colonnes d'actions.

## Affichage (`views/`)

- `Card` : `label` (clé i18n), `icon` ; slots `title`, `header`, `rightHeader`, `body`, `footer`. C'est le cadre des pages de détail.
- `StringView` (`label`, `value`, `allowCopy`), `TextView`, `DateView` (`value`, `isDatetime`), `UriView` (`uri`, `value`, `to`), `UriLink`, `UriListView`, `LabelUriView`, `TypeView` (`type`, `typeLabel`), `MetadataView` (`publisher`, `publicationDate`, `lastUpdatedDate`), `AddressView`, `IconView`, `GeometryCopy`.
- `Icon` : `icon` au format `fa#nom` ou `bi#bi-nom`. Le préfixe `ik#` n'est pas géré, l'icône n'apparaît pas.
- Toasts : `$opensilex.showSuccessToast / showErrorToast / showInfoToast / showWarningToast`, branchés sur `ToastContainer` par `App.vue`. Ne pas instancier de `n-message` pour ça.

## Naive UI : ce qui est global

La liste est dans `opensilex-front/front/src/main.ts` (`create({ components: […] })`), et elle **dépend de la branche** : sur `vue3/main`, seulement `NButton`, `NDataTable`, `NDropdown`, `NTree` ; `feat/aiimport` en ajoute 14 pour le module ai-import. Règle sûre dans l'hôte : importer depuis `naive-ui` chaque `n-*` utilisé. Une balise ni importée ni globale n'est pas résolue et ne rend rien, sans erreur. Sur `vue3/main`, `FormFooter` (`n-button-group`) et `ModalListBuilder` (`n-tag`) ont ce défaut.

Il n'y a pas de `n-config-provider` racine. `App.vue` fournit `n-dialog-provider` et `n-message-provider` aux seules routes connectées.

Pour un module d'extension, voir `opensilex-doc/src/main/resources/technical-documentation/opensilex-module/module-frontend-loading.md`, et le skill `opensilex-vue3` (`references/extension-module.md`) s'il est installé.

## Défauts connus (à signaler, pas à corriger en douce)

Constatés le 2026-10-09 ; vérifier qu'ils sont toujours là avant de les citer.

- `TableAsyncView` : en `onMounted`, il fait `props.fields.unshift({ key: 'select', … })` quand `isSelectable` et `selectMode !== 'single'`. Il **mute le tableau du parent**, en plus de la colonne `selection` qu'il ajoute déjà. Passer un tableau propre au composant, pas une constante partagée.
- `DateRangePickerForm` : le gabarit lie `startDatePath`, alors que la prop s'appelle `startDateValidatePath`. La validation de la date de début n'est jamais branchée.
- `FormFooter` : `n-button-group` n'est ni importé ni global ; les émissions passent par `$emit`, sans `defineEmits`.
- `ModalListBuilder` : `n-tag` n'est pas résolu.
- `Button` : le mode `href` rend un `<button href>`, qui ne navigue pas.
- `AddChildButton` : props déstructurées sans réactivité.
- `DatePeriodPicker` : dates calculées seulement en `onMounted`, donc `update` émet des dates périmées.
- `WizardForm.getStepRef` utilise `this` dans `<script setup>`.
- `TreeViewAsync` : fichier encadré par des balises Markdown (lignes 1 et 668), et clés i18n `TreeViewAsync.*` inexistantes.
- `FormInputLabelHelper` et `UriLink` rendent du `v-html` : ne pas y faire passer de contenu non maîtrisé.
- `views/NbElementPerPageSelector.vue.vue` : double extension, inutilisable comme balise.
