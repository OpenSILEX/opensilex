# Compliant component skeletons

## Contents

- [Complete component](#complete-component): every region, a concrete case, and what it shows
- [Short component](#short-component): `Public` / `Private` only
- [Modal form](#modal-form): `Modal` + `n-form` + `useModalFormLogic`

These skeletons apply every rule of the skill on a concrete case. The abstract reference example, with the same
structure, is the one of `opensilex-doc/src/main/resources/technical-documentation/opensilex-front/vuejs-file-structure.md`.

## Complete component

`DeviceSelector` is a real host component; `UsageRow` and `DeviceUsage` are fictional.

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

/** Local shape: no generated DTO carries this grouping. */
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

/** v-model:value of the parent. */
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
 * DeviceSelector emits `select` with an `any` payload: the parameter is typed here,
 * at the boundary, and the rest of the component only sees NamedResourceDTO.
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
 * Reloads the usages of the current experiment.
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
```

What this skeleton shows:

- **Block order**: `<template>`, `<script setup lang="ts">`, `<style scoped lang="scss">`. The last two are
  left out when empty: no empty `<style>`.
- **Imports** first, outside any region. API types use `import type`. Local types (`interface`, `type`) follow the
  imports.
- **Public region**: `defineEmits` (named tuples, `[]` for an event without payload) or defineEmits<{...}> (object syntax for events with payload), `defineProps` (+ `withDefaults`),
  `defineModel`.
- **Private region**, sub-regions always in this order: *Plugins & services* → *Template refs* → *Data & Computed* →
  *Hooks & watchers* → *Event handlers* → *Methods*.
- **`defineExpose()` last**, outside the regions.
- **Handlers** named `on<Child><Event>` with a typed payload. When a template-scope variable is needed as well, an arrow
  function calls the method: `(count) => onUsageRowChanged(count, usage)`. A one-line `emit(...)` may stay inline in the
  template.
- **Local interface** when no DTO matches the handled shape (`DeviceUsage`); never `any`.
- **Template ref**: the variable may have the same name as the `ref` attribute (Vue 3.5 handles it), and it is typed
  with `InstanceType<typeof Component>`. For a naive-ui component that exposes an instance interface, use that type:
  `useTemplateRef<FormInst>('formRef')`, `useTemplateRef<DataTableInst>('tableRef')`.
- **Credentials**: `store.state.user.hasCredential(credentials.value.CREDENTIAL_…_ID)`; the constants are in
  `models/Store.ts`.

## Short component

For a small component, the `Public` and `Private` regions are enough, as long as elements of the same kind stay
together:

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

```

## Modal form

For a create or edit modal, the pattern is `Modal.vue` + `n-form` + `useModalFormLogic`. Host examples:
`components/devices/form/DeviceForm.vue` and `components/ontology/OntologyObjectForm.vue`. Read them for the API, not for
the style. Apply the rules of this skill to them:

- `defineEmits<ModalFormEmits>()` and `defineProps<ModalFormProps>()`: these are the composable's types;
- `useTemplateRef<InstanceType<typeof Modal>>('modalRef')` and `useTemplateRef<FormInst>('formRef')`;
- validation rules built in `setup` (`requiredTrimmed`, `required`, `requiredNotEmpty` from
  `@/models/FormFieldsFormatter`), never in a callback, because they call `useI18n()`;
- one `n-form-item` per **simple** field, but **never** around a selector built on `FormSelector` or `InfiniteScrollDropdown`: pass it `path` and
  `label`, it renders the label and wires validation itself (see `common-components.md`).
