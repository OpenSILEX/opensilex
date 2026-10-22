# Squelette de composant conforme

Ce squelette applique toutes les règles du skill sur un cas concret. L'exemple abstrait de référence, avec la même
structure, est celui de `opensilex-doc/src/main/resources/technical-documentation/opensilex-front/vuejs-file-structure.md`.

## Composant complet

`DeviceSelector` est un vrai composant de l'hôte ; `UsageRow` et `DeviceUsage` sont fictifs.

```vue
<template>
  <n-space vertical>
    <DeviceSelector
      ref="deviceSelector"
      v-model:value="selectedDeviceUri"
      :label="t('DeviceUsage.device')"
      @select="onDeviceSelectorSelect"
    />

    <n-data-table :columns="columns" :data="usages" :loading="isLoading" />

    <template v-for="usage in usages" :key="usage.uri">
      <UsageRow :usage="usage" @changed="(count) => onUsageRowChanged(count, usage)" />
    </template>

    <n-button :disabled="!canEdit || !selectedDeviceUri" @click="emit('validated', selectedDeviceUri)">
      {{ t('DeviceUsage.validate') }}
    </n-button>
  </n-space>
</template>

<script setup lang="ts">
import { computed, h, inject, onMounted, ref, useTemplateRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import { NButton, NDataTable, NSpace, type DataTableColumns } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { OpenSilexStore } from '@/models/Store'
import type HttpResponse from 'opensilex-core/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-core/HttpResponse'
import type { DevicesService, DeviceGetDTO, NamedResourceDTO } from 'opensilex-core/index'
import DeviceSelector from '@/components/devices/DeviceSelector.vue'
import UsageRow from '@/components/devices/UsageRow.vue'

/** Forme locale : aucun DTO généré ne porte ce regroupement. */
interface DeviceUsage {
  uri: string
  name: string
  count: number
}

//#region Public
const emit = defineEmits<{
  validated: [deviceUri: string]
  cleared: []
}>()

const props = withDefaults(
  defineProps<{
    experimentUri: string
    readOnly?: boolean
  }>(),
  {
    readOnly: false,
  },
)

/** v-model:value du parent. */
const value = defineModel<string>('value')
//#endregion

//#region Private

//#region Plugins & services
const { t } = useI18n()
const store = useStore() as OpenSilexStore
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const devicesService = $opensilex.getService<DevicesService>('opensilex.DevicesService')
//#endregion

//#region Template refs
const deviceSelector = useTemplateRef<InstanceType<typeof DeviceSelector>>('deviceSelector')
//#endregion

//#region Data & Computed
const selectedDeviceUri = ref<string>()
const usages = ref<DeviceUsage[]>([])
const isLoading = ref(false)

const credentials = computed(() => store.state.credentials)
const canEdit = computed(
  () => !props.readOnly && store.state.user.hasCredential(credentials.value.CREDENTIAL_DEVICE_MODIFICATION_ID),
)

const columns = computed<DataTableColumns<DeviceUsage>>(() => [
  { title: t('DeviceUsage.name'), key: 'name' },
  { title: t('DeviceUsage.count'), key: 'count', render: (row) => h('strong', row.count) },
])
//#endregion

//#region Hooks & watchers
onMounted(loadUsages)

watch(
  () => props.experimentUri,
  () => loadUsages(),
)
//#endregion

//#region Event handlers
/**
 * DeviceSelector émet `select` avec un payload `any` : on type le paramètre ici,
 * à la frontière, et le reste du composant ne voit que NamedResourceDTO.
 */
function onDeviceSelectorSelect(device: NamedResourceDTO) {
  value.value = device.uri
}

function onUsageRowChanged(count: number, usage: DeviceUsage) {
  usage.count = count
}
//#endregion

//#region Methods
/**
 * Recharge les usages de l'expérimentation courante.
 */
async function loadUsages() {
  isLoading.value = true
  try {
    const http: HttpResponse<OpenSilexResponse<DeviceGetDTO[]>> = await devicesService.searchDevices(
      undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined,
      undefined, undefined, ['name=asc'], 0, 20,
    )
    usages.value = http.response.result.map((d) => ({ uri: d.uri, name: d.name, count: 0 }))
  } catch (error) {
    $opensilex.errorHandler(error)
  } finally {
    isLoading.value = false
  }
}

function reset() {
  selectedDeviceUri.value = undefined
  emit('cleared')
}
//#endregion

//#endregion

defineExpose({
  reset,
  loadUsages,
})
</script>

<style scoped lang="scss">
:deep(.n-data-table-td) {
  vertical-align: top;
}
</style>

<i18n>
en:
  DeviceUsage:
    device: Device
    name: Name
    count: Usages
    validate: Validate
fr:
  DeviceUsage:
    device: Appareil
    name: Nom
    count: Utilisations
    validate: Valider
</i18n>
```

Ce que montre ce squelette :

- **Ordre des blocs** : `<template>`, `<script setup lang="ts">`, `<style scoped lang="scss">`, `<i18n>`. Les deux derniers sont omis s'ils sont vides : pas de `<style>` vide, contrairement à `SearchFiltersSidebar.vue`.
- **Imports** en tête, hors région. Les types de l'API passent par `import type`. Les types locaux (`interface`, `type`) suivent les imports.
- **Région Public** : `defineEmits` (syntaxe tableau nommé, `[]` pour un événement sans payload), `defineProps` (+ `withDefaults`), `defineModel`.
- **Région Private**, sous-régions toujours dans cet ordre : *Plugins & services* → *Template refs* → *Data & Computed* → *Hooks & watchers* → *Event handlers* → *Methods*.
- **`defineExpose()` en dernier**, hors des régions.
- **Gestionnaires** nommés `on<Enfant><Événement>` avec le payload typé. Quand il faut une variable du scope du gabarit en plus, une fonction fléchée appelle la méthode : `(count) => onUsageRowChanged(count, usage)`. Un `emit(...)` d'une ligne peut rester en ligne dans le gabarit.
- **Interface locale** quand aucun DTO ne correspond à la forme manipulée (`DeviceUsage`) ; jamais `any`.
- **Ref de gabarit** : la variable peut porter le même nom que l'attribut `ref` (Vue 3.5 le gère), et elle est typée par `InstanceType<typeof Composant>`. Pour un composant naive-ui qui expose une interface d'instance, utiliser ce type : `useTemplateRef<FormInst>('formRef')`, `useTemplateRef<DataTableInst>('tableRef')`.
- **Credentials** : `store.state.user.hasCredential(credentials.value.CREDENTIAL_…_ID)` ; les constantes sont dans `models/Store.ts`.

## Composant court

Pour un petit composant, les régions `Public` et `Private` suffisent, à condition que les éléments de même nature restent groupés :

```vue
<template>
  <n-tag :type="tagType" size="small">{{ t(`StatusTag.${status}`) }}</n-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { NTag } from 'naive-ui'

type Status = 'active' | 'archived'

//#region Public
const props = defineProps<{
  status: Status
}>()
//#endregion

//#region Private
const { t } = useI18n()

const tagType = computed(() => (props.status === 'active' ? 'success' : 'default'))
//#endregion
</script>

<i18n>
en:
  StatusTag:
    active: Active
    archived: Archived
fr:
  StatusTag:
    active: Actif
    archived: Archivé
</i18n>
```

## Formulaire modal

Pour une modale de création ou d'édition, le patron est `Modal.vue` + `n-form` + `useModalFormLogic`. Exemples dans l'hôte : `components/devices/form/DeviceForm.vue`, `components/ontology/OntologyObjectForm.vue` (sources d'API, pas modèles de style). Le skill `opensilex-vue3`, s'il est installé, en donne un gabarit (`references/sfc-templates.md`, gabarit 3). On y applique les règles de ce skill :

- `defineEmits<ModalFormEmits>()` et `defineProps<ModalFormProps>()` : ce sont les types du composable ;
- `useTemplateRef<InstanceType<typeof Modal>>('modalRef')` et `useTemplateRef<FormInst>('formRef')` ;
- règles de validation calculées dans `setup` (`requiredTrimmed`, `required`, `requiredNotEmpty` de `@/models/FormFieldsFormatter`), jamais dans un callback, parce qu'elles appellent `useI18n()` ;
- un `n-form-item` par champ **simple**, mais **jamais** autour d'un sélecteur bâti sur `FormSelector` : on lui passe `path` et `label`, il rend lui-même le libellé et branche la validation (voir `common-components.md`).
