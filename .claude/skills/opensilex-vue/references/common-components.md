# Shared building blocks of the host (`opensilex-front/front/src/components/common/`)

## Contents

- [Label and validation: who wraps what](#label-and-validation-who-wraps-what)
- [Lists and filters](#lists-and-filters): `TableAsyncView`, `SearchFiltersSidebar`, `StringFilter`, `TableView`, trees
- [Forms](#forms): `Modal`, `useModalFormLogic`, header and footer, wizard, validation rules
- [Fields](#fields-forms)
- [Buttons](#buttons-buttons)
- [Display](#display-views)
- [Naive UI: what is global](#naive-ui-what-is-global)
- [Known defects](#known-defects-report-them-do-not-fix-them-on-the-side)

This catalogue summarises the API; when in doubt, the source file is the authority, so read it.

**Three rules:**

1. **Reuse** these building blocks rather than writing a new one.
2. **Do not modify them** unless asked: lists and views already migrated depend on them. If a defect gets in the way,
   report it (section "Known defects") and work around it on the caller's side.
3. **Replacing Bootstrap with Naive UI** applies to the markup being written. Several building blocks still use
   Bootstrap inside (`Button` and its variants, `Card`, `Modal`): use them anyway, since they are the shared ones.

Every `.vue` file under `components/` is registered globally as `opensilex-<FileName>`. In new code, import it
explicitly (`import X from '@/components/common/…/X.vue'`): recent files do so, and it is the only way to type a template
ref with `InstanceType<typeof X>`.

## Label and validation: who wraps what

This is the most frequent source of mistakes, hence this table.

| Component | Renders its label? | Renders its `n-form-item`? | Use in an `n-form` |
|---|---|---|---|
| `FormField` | yes (div + red `*`, help in a tooltip) | **no**, no validation | the wrapper of the fields below |
| `InputForm`, `TextAreaForm`, `DateForm`, `DateTimeForm`, `CheckboxForm`, `TagInputForm`, `IconForm`, `GeometryForm`, `FileInputForm`, `UriForm`, `TypeForm` | yes, through the `label` prop | no | `<n-form-item path="field">` **without `label`**; the field carries `label` |
| `FormSelector`, `InfiniteScrollDropdown` and **every selector built on them** | yes | **yes** (`path`, `:show-label="false"`) | **do not wrap**: pass `path` and `label` to the selector |
| `DateRangePickerForm`, `AddressForm`, `CSVSelectorInputForm` | yes | yes | do not wrap |

The two mistakes to avoid:

- an `n-form-item` with a `label` around a field that also has a `label`, which shows two labels;
- an `n-form-item` around `FormSelector`, `InfiniteScrollDropdown` or a selector built on one of them, which gives two
  labels and two validation messages.

`InfiniteScrollDropdown` (`forms/InfiniteScrollDropdown.vue`) behaves like `FormSelector` here: it wraps `FormField` for the
label and renders its own `n-form-item :path`, so the rules of the parent `n-form` apply through `path`. It is the base of the
selectors that load their options page by page (infinite scroll), whereas `FormSelector` is built on `CustomTreeselect`.

The `rules` and `vid` props passed to some fields are Vue 2 leftovers: `FormField` does not declare them, they have no
effect. Rules are declared on the `n-form` (`:rules`) and attached through `path`.

**Selectors built on `FormSelector`** (do not wrap): `AgentTypeSelector`, `CriteriaOperatorSelector`,
`DataProvenanceSelector`, `DatafileProvenanceSelector`, `FacilityPropertySelector`, `FactorLevelSelector`,
`FundingSelector`, `GermplasmGroupSelector`, `OrganizationSelector`, `ScientificObjectTypeSelector`,
`SharedResourceInstanceSelector`, `SpeciesSelector`, `VariableDataTypeSelector`, `VariableTimeIntervalSelector`.

**Selectors built on `InfiniteScrollDropdown`** (do not wrap): `CharacteristicSelector`, `DeviceSelector`,
`EntitySelector`, `ExperimentSelector`, `FacilitySelector`, `GermplasmSelector`, `GroupSelector`,
`GroupVariablesSelector`, `InterestEntitySelector`, `MethodSelector`, `PersonSelector`, `ProjectSelector`,
`ProvenanceSelector`, `ScientificObjectParentPropertySelector`, `SiteSelector`, `UnitSelector`, `VariableSelector`.

A selector that composes another one (`DevicePropertySelector` on `DeviceSelector`, `GermplasmPropertySelector` on
`GermplasmSelector`, `FactorLevelPropertySelector` on `FactorLevelSelector`, `VariablePropertySelector` on
`VariableSelector`) follows the rule of the selector it wraps. For any other selector, look in its template for `FormSelector` (also written
`<opensilex-FormSelector`) or `InfiniteScrollDropdown`.

## Lists and filters

### `TableAsyncView` (`views/TableAsyncView.vue`)

Server-side paginated table, based on `n-data-table` in `remote` mode. It is **the** table of list pages.

- **props**: `fields: TableField[]` (required); `searchMethod` (required), called with
  `{ orderBy: string[], currentPage: number /* 0-based */, pageSize: number }`, which must return
  `Promise<HttpResponse<OpenSilexResponse<T[]>>>`; `defaultSortBy` (default `'name'`); `defaultSortDesc`;
  `fieldKeyToSortableModelLabelMap` (column key → name of the sortable field on the API side); `isSelectable`;
  `selectMode: 'multi' | 'single'`; `maximumSelectedRows`; `labelNumberOfSelectedRow`; `iconNumberOfSelectedRow`;
  `defaultPageSize` (20); `showCount` (true); `showHeaderCount` (true).
- **`TableField`** (`views/TableField.ts`): `{ key: string; label?: string /* i18n key */; sortable?: boolean;
  resizable?: boolean; isSelect?: boolean; naiveProps?: object }`.
- **slots**: `cell(<key>)` with the scope `{ data: { item, index } }`, the shape inherited from Bootstrap-Vue;
  `row-details` (expanded row, when `item._showDetails`); `export`; `selectableTableButtons` (`{ numberOfSelectedRows }`).
- **emits**: `select(item)`, `unselect(item)`, `row-selected(count)`, `refreshed`, `selectall(items)`.
- **exposes**: `refresh()`, `getSelected()`, `resetSelection()`, `toggleOnlySelected()`, `setOnlySelected(b)`,
  `setPage(n)`, `getCurrentPage()`, `getPageSize()`, `getTotalRow()`, `getPaginationInfo()`, `checkSelectedItems(uri)`,
  `onItemSelected(item)`, `onItemUnselected(item)`.
- The row key is `row.uri`. Running a search means calling `tableRef.value?.refresh()`.
- **Weakly typed boundary**: `searchMethod: Function`, and the `select` / `unselect` payloads are typed
  `NamedResourceDTO`. On the caller's side, type `searchMethod` precisely (parameters and return), and type the handlers
  with the real row DTO.

### `SearchFiltersSidebar` (`filters/SearchFiltersSidebar.vue`)

Magnifier button and side filter panel (`n-layout-sider`), to be placed **inside an `<n-layout has-sider>`**.

- **v-model**: `v-model:filtersCollapsed` (`boolean`).
- **props**: `activeFiltersCount: number` (required; the parent computes it from its filter object),
  `searchButtonLabelTranslationKey` (default `searchfilter.label`).
- **emits**: `refresh` (Search button or Enter key), `reset` (Clear button).
- **default slot**: the filter fields. The slot already sits in an `n-form` (`label-placement="top"`, `size="small"`),
  so it holds `n-form-item`s or selectors built on `FormSelector` / `InfiniteScrollDropdown`, without wrapping them.
- The Search and Clear buttons are provided: do not add others.

### `StringFilter` (`filters/StringFilter.vue`)

Text filter field with a clear button: `v-model:filter`, `placeholder` (i18n key, required), `label?`, `type`
(`'text'`), `min` / `max` (for `number`). It emits `update`, `handlingEnterKey`. It still uses Bootstrap inside
(`input-group`). In a new filter, an `n-input` in an `n-form-item` works as well.

### `TableView` (`views/TableView.vue`)

**Client-side** table (data already loaded): `items`, `fields: TableField[]`, `withPagination`, `globalFilterField`. Slot
`cell(<key>)` with the scope `{ data: { item } }`. Here `field.label` is **not** translated: the caller passes it
translated. For a small static table, `n-data-table` with `DataTableColumns<T>` is often simpler.

### `TreeView` / `TreeViewAsync` (`views/`)

`n-tree` trees. `TreeView<T extends TreeViewOption>` is typed; it emits `select(T[])`, `toggle(T[])`, with the slots
`node` and `buttons`. `TreeViewAsync` loads on demand through `searchMethod(parentUri, page, pageSize)` and
`searchMethodRoot`, with `v-model:selection`; it exposes `refresh()`, `reloadNodeChildren(key)`.

## Forms

- **`Modal`** (`views/Modal.vue`): props `modalSize: 'sm' | 'lg' | 'xl'`, `hideHeader`, `hideFooter`; slots `header`,
  default, `footer`; exposes `show()`, `hide()`. It is the modal `useModalFormLogic` expects. It emits **nothing** when
  closed with the cross.
- **`useModalFormLogic<T>`** (`@/composables/useModalFormLogic`):
  - **options**: `{ modalRef, nFormRef, getEmptyForm, create, update, props, emit, reset?, onHide?, successMessage?,
    overrideSuccessMessage? }`;
  - **returns**: `{ form, isEditMode, showCreateForm, showEditForm, hide, submit, formTitle }`;
  - **exported types**: `ModalFormEmits` (`onCreate`, `onUpdate`, `onSuccess`) and `ModalFormProps` (`createTitle`,
    `editTitle`);
  - the composable validates the `n-form`, handles the loader and the toast, emits, then closes. Call it in `setup`.
- **`FormHeader`**: `title` (displayed as is, so pass it **already translated**), `icon`.
- **`FormFooter`**: emits `cancel` and `submit` (not `save`); labels `cancelLabel` / `submitLabel` (i18n keys).
- **`WizardForm`**: multi-step wizard (`steps: WizardStep[]`, `initForm`, `createAction` / `updateAction`), exposes
  `showCreateForm()` / `showEditForm(form)` / `close()`.
- **Validation rules** (`@/models/FormFieldsFormatter`): `requiredTrimmed(labelKey)`, `required(labelKey)`,
  `requiredNotEmpty(labelKey)`, `validEmail()`, `validUri(message)`. They return naive-ui `FormItemRule`s and call
  `useI18n()`, so build them **in `setup`**, typically in a `computed<FormRules>`.
- **Conversion**: `DTOConverter.extractURIFromResourceProperties(getDto)` (`@/models/DTOConverter`) turns a *GetDTO*
  into a *Creation/UpdateDTO* (`NamedResourceDTO`s become URIs).

## Fields (`forms/`)

| Component | v-model | Notes |
|---|---|---|
| `InputForm` | `value` (`string \| null`) | `type: 'text' \| 'password' \| 'textarea' \| 'email' \| 'number' \| 'date'`; also emits `input`, `change`, `blur`, `handlingEnterKey` |
| `TextAreaForm` | `value` | |
| `CheckboxForm` | `value` (`boolean`) | `title`: text next to the box |
| `DateForm` | `value` (`yyyy-MM-dd`) | `minDate`, `maxDate` |
| `DateTimeForm` | `value` (ISO UTC) | emits `clear` |
| `DateRangePickerForm` | `start`, `end` | renders its `n-form-item`s; see known defects |
| `DateTimeRangeForm` | `startDate`, `endDate`, `isInstant` | fixed labels (`component.events.*`) |
| `TagInputForm` | `value` (`string[]`) | |
| `TypeForm` | `type` | `baseType` (URI of the root class, required), `multiple`, `ignoreRoot` |
| `UriForm` | `uri`, `generated` | `editMode`; **`v-model:uri`**, never `:uri.sync` (broken Vue 2 syntax, still found in some forms) |
| `IconForm` | `value` | `fa#…` icons |
| `GeometryForm` | `value` (GeoJSON) | WKT input |
| `FileInputForm` | `file` (`File`) | `accept` |
| `SwitchGroup` | `value` (`string[]`) | `options: SwitchOption[]` (exported type) |
| `AddressForm` | `address` (`FacilityAddressDTO`) | renders its `n-form-item`s |
| `CustomTreeselect` | `selected` | engine of `FormSelector`; do not use it directly |

## Buttons (`buttons/`)

They use Bootstrap inside, but they are **the** action buttons of the platform. To edit, delete, create or show the
detail of a resource, use them rather than a bare `n-button`.

| Component | Props | Emits |
|---|---|---|
| `Button` | `label?` (i18n key), `variant?` (→ `btn-<variant>`), `icon?` (`fa#…` or `bi-…`), `small?`, `disabled?`, `helpMessage?` | `click` |
| `CreateButton` | `label` | `click` |
| `EditButton` | `label` | `click` |
| `DeleteButton` | `label`, `small` (required) | `click` **after confirmation**: do not ask again |
| `DetailButton` | `label`, `detailVisible?`, `automaticToogle?` | `click` |
| `AddChildButton`, `HelpButton`, `InteroperabilityButton` | `label` | `click` |
| `DeprecatedButton` | `deprecated` | `click` |
| `FavoriteButton` | `uri` | handles the API call itself |

`small: true` **hides the label**, which only remains as a tooltip: it is the icon-only mode of action columns.

## Display (`views/`)

- `Card`: `label` (i18n key), `icon`; slots `title`, `header`, `rightHeader`, `body`, `footer`. It frames detail pages.
- `StringView` (`label`, `value`, `allowCopy`), `TextView`, `DateView` (`value`, `isDatetime`), `UriView` (`uri`,
  `value`, `to`), `UriLink`, `UriListView`, `LabelUriView`, `TypeView` (`type`, `typeLabel`), `MetadataView`
  (`publisher`, `publicationDate`, `lastUpdatedDate`), `AddressView`, `IconView`, `GeometryCopy`.
- `Icon`: `icon` as `fa#name` or `bi#bi-name`. The `ik#` prefix is not handled: the icon does not show.
- Toasts: `$opensilex.showSuccessToast / showErrorToast / showInfoToast / showWarningToast`, wired to `ToastContainer`
  by `App.vue`. Do not create an `n-message` for that.

## Naive UI: what is global

The list is in `opensilex-front/front/src/main.ts` (`create({ components: […] })`). It is short and **differs between
branches**, so read it on the current branch. The safe rule in the host: import from `naive-ui` every `n-*` used. A tag
that is neither imported nor global is not resolved and renders nothing, without any error.

There is no root `n-config-provider`. `App.vue` provides `n-dialog-provider` and `n-message-provider` to logged-in
routes only.

An extension module does not import naive-ui itself; its loading is described in
`opensilex-doc/src/main/resources/technical-documentation/opensilex-module/module-frontend-loading.md`.

## Known defects (report them, do not fix them on the side)

These were found while writing this skill; check that a defect is still there before citing it.

- `TableAsyncView`: in `onMounted`, it runs `props.fields.unshift({ key: 'select', … })` when `isSelectable` and
  `selectMode !== 'single'`. It **mutates the parent's array**, on top of the `selection` column it already adds. Pass
  the component an array of its own, not a shared constant.
- `DateRangePickerForm`: the template binds `startDatePath`, whereas the prop is named `startDateValidatePath`. The
  validation of the start date is never wired.
- `FormFooter`: `n-button-group` is neither imported nor global; emits go through `$emit`, without `defineEmits`.
- `ModalListBuilder`: `n-tag` is not resolved.
- `Button`: the `href` mode renders a `<button href>`, which does not navigate.
- `AddChildButton`: destructured props, without reactivity.
- `DatePeriodPicker`: dates computed only in `onMounted`, so `update` emits stale dates.
- `WizardForm.getStepRef` uses `this` in `<script setup>`.
- `TreeViewAsync`: file wrapped in Markdown fences (first and last lines), and `TreeViewAsync.*` i18n keys that do not
  exist.
- `FormInputLabelHelper` and `UriLink` render `v-html`: do not pass uncontrolled content through them.
- `views/NbElementPerPageSelector.vue.vue`: double extension, unusable as a tag.
