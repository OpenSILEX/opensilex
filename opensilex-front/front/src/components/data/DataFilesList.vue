<template>
  <div>
    <n-space class="listActionButtons">
      <n-dropdown
        trigger="hover"
        placement="bottom-start"
        :options="displayDropdownOptions"
        @select="onDisplayDropdownSelect"
      >
        <n-button size="small" class="greenThemeColor">
          {{ t('DataFilesList.display') }}
        </n-button>
      </n-dropdown>

      <n-dropdown
        trigger="hover"
        placement="bottom-start"
        :options="actionsDropdownOptions"
        :disabled="selectedCount === 0"
        @select="onActionsDropdownSelect"
      >
        <n-button
          size="small"
          :disabled="selectedCount === 0"
          :class="selectedCount === 0 ? 'btn-disabled' : 'greenThemeColor'"
        >
          {{ t('component.common.actions') }}
        </n-button>
      </n-dropdown>

      <div class="displayAndListSelectionCount">
        <div v-if="paginationInfo.hasResults">
          <strong>
            <span class="ml-1">
              {{
                t('component.common.list.pagination.nbEntries', {
                  limit: paginationInfo.start,
                  offset: paginationInfo.end,
                  totalRow: totalCountLoaded ? n(paginationInfo.total) : '...'
                })
              }}
            </span>
          </strong>


          <opensilex-Button
            :small="true"
            @click="loadTotalCount"
            label="component.common.list.pagination.showTotalCount"
            class="totalCountDetailButton"
          >
            <template v-slot:icon>
              <opensilex-Icon icon="fa#eye" />
            </template>
          </opensilex-Button>
        </div>

        <div v-else>
          <strong>
            <span class="ml-1">
              {{ t('component.common.list.pagination.noEntries') }}
            </span>
          </strong>
        </div>

        <span> | </span>

        <n-p>
          {{ t('DataFilesList.selected') }} :
          <span class="badge badge-pill greenThemeColor">{{ selectedCount }}</span>
        </n-p>
      </div>
    </n-space>

    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchDatafiles"
      :countMethod="countDatafiles"
      :fields="fields"
      defaultSortBy="name"
      :isSelectable="true"
      labelNumberOfSelectedRow="DataFilesList.selected"
    >
      <template #cell(target)="{ data }">
        <opensilex-UriLink
          :uri="data.item.target"
          :value="objects[data.item.target]"
          :to="{ path: getTargetPath(data.item.target) }"
        />
      </template>

      <template #cell(format)="{ data }">
        <div>{{ getFileFormat(data.item.filename) }}</div>
      </template>

      <template #cell(filename)="{ data }">
        <div>{{ data.item.filename }}</div>
      </template>

      <template #cell(rdfType)="{ data }">
        <div>{{ rdf_types[data.item.rdf_type] }}</div>
      </template>

      <template #cell(provenance)="{ data }">
        <opensilex-UriLink
          :uri="data.item.provenance.uri"
          :value="provenances[data.item.provenance.uri]"
          :to="{ path: '/provenances/details/' + encodeURIComponent(data.item.provenance.uri) }"
        />
      </template>

      <template #cell(type)="{ data }">
        <div>{{ rdf_types[data.item.rdf_type] }}</div>
      </template>

      <template #cell(actions)="{ data }">
        <n-button-group size="small">
          <opensilex-Button
            :disabled="!images_rdf_types.includes(data.item.rdf_type)"
            component="opensilex-DocumentDetails"
            @click="showImage(data.item)"
            label="ScientificObjectDataFiles.displayImage"
            :small="true"
            icon="fa#image"
            variant="outline-info"
          />
          <opensilex-DetailButton
            v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_MODIFICATION_ID)"
            @click="showDataProvenanceDetailsModal(data.item)"
            label="DataFilesView.details"
            :small="true"
          />
          <opensilex-Button
            @click="deleteDatafile(data.item.uri)"
            variant="outline-danger"
            :small="true"
            label="DataFilesList.delete"
            icon="fa#trash-alt"
          />
        </n-button-group>
      </template>
    </opensilex-TableAsyncView>

    <opensilex-DataProvenanceModalView
      ref="dataProvenanceModalView"
      :datafile="true"
    />

    <opensilex-ImageModal
      ref="imageModal"
      v-model:fileUrl="imageUrl"
    />

    <opensilex-ExportDataFileModal
      ref="exportDataFileModal"
      @onCreate="exportDataFiles"
    />

    <opensilex-EventCsvForm
      v-if="showEventForm && user.hasCredential(credentials.CREDENTIAL_EVENT_MODIFICATION_ID)"
      ref="eventCsvForm"
      :targets="selectedUris"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { NButton, NButtonGroup, NDropdown, NSpace, NP } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { ProvenanceGetDTO, ResourceTreeDTO } from 'opensilex-core/index'
import type HttpResponse from 'opensilex-core/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-core/HttpResponse'
import type { OntologyService } from 'opensilex-core/api/ontology.service'
import type { DataService } from 'opensilex-core/api/data.service'

type DatafileFilter = {
  name?: string | null
  start_date?: string | null
  end_date?: string | null
  rdf_type?: string | null
  provenance?: any
  experiments?: any[]
  scientificObjects?: any[]
  devices?: any[]
}

const { t, n } = useI18n()
const route = useRoute()
const router = useRouter()
const store = useStore()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

const props = withDefaults(defineProps<{
  filter?: DatafileFilter
  device?: string
  contextUri?: string
  hideTarget?: boolean
}>(), {
  filter: () => ({
    name: null,
    start_date: null,
    end_date: null,
    rdf_type: null,
    provenance: null,
    experiments: [],
    scientificObjects: []
  }),
  contextUri: '',
  hideTarget: false
})

const emit = defineEmits<{
  (e: 'redirectToDetail'): void
}>()

const tableRef = ref<any>(null)
const dataProvenanceModalView = ref<any>(null)
const imageModal = ref<any>(null)
const eventCsvForm = ref<any>(null)
const exportDataFileModal = ref<any>(null)

const service = ref<DataService | null>(null)
const ontologyService = ref<OntologyService | null>(null)

const showEventForm = ref(false)
const selectedUris = ref<string[]>([])
const imageUrl = ref<string | null>(null)

const objects = ref<Record<string, string>>({})
const provenances = ref<Record<string, string>>({})
const objectsPath = ref<Record<string, string>>({})
const images_rdf_types = ref<string[]>([])
const rdf_types = ref<Record<string, string>>({})

const currentPage = ref(1)
const currentPageSize = ref(10)
const currentPageItemCount = ref(0)

const totalCount = ref<number | null>(null)
const totalCountLoaded = ref(false)
const totalCountLoading = ref(false)

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)
const lang = computed(() => store.getters.language)

const fields = computed(() => {
  const baseFields: any[] = [
    {
      key: 'date',
      label: t('DataFilesList.date'),
      sortable: true
    },
    {
      key: 'format',
      label: t('DataFilesList.format'),
      sortable: false
    },
    {
      key: 'filename',
      label: t('DataFilesList.filename'),
      sortable: true
    },
    {
      key: 'rdfType',
      label: t('DataFilesList.rdfType'),
      sortable: true
    },
    {
      key: 'provenance',
      label: t('DataFilesList.provenance'),
      sortable: false
    },
    {
      key: 'actions',
      label: t('component.common.actions')
    }
  ]

  if (!props.hideTarget) {
    baseFields.unshift({
      key: 'target',
      label: 'DataView.list.object'
    })
  }

  return baseFields
})

const selectedCount = computed(() => {
  return (tableRef.value?.getSelected?.() ?? []).length
})

const onlySelected = computed(() => !!tableRef.value?.onlySelected)

const paginationInfo = computed(() => {
  const hasResults = currentPageItemCount.value > 0

  if (!hasResults) {
    return {
      start: 0,
      end: 0,
      total: totalCount.value ?? 0,
      hasResults: false
    }
  }

  const start = (currentPage.value - 1) * currentPageSize.value + 1
  const end = start + currentPageItemCount.value - 1

  return {
    start,
    end,
    total: totalCount.value ?? 0,
    hasResults: true
  }
})

const displayDropdownOptions = computed(() => [
  {
    label: onlySelected.value
      ? t('DataFilesList.selected-all')
      : t('component.common.selected-only'),
    key: 'onlySelected'
  },
  { type: 'divider', key: 'd1' },
  {
    label: t('component.common.resetSelected'),
    key: 'resetSelected'
  }
])

const actionsDropdownOptions = computed(() => {
  const options: any[] = [
    {
      label: t('DataFilesList.export'),
      key: 'export'
    }
  ]

  if (user.value?.hasCredential?.(credentials.value.CREDENTIAL_EVENT_MODIFICATION_ID)) {
    options.push({
      label: t('DataFilesList.add-event'),
      key: 'createEvents'
    })
  }

  return options
})

onMounted(() => {
  service.value = $opensilex.getService('opensilex.DataService')
  ontologyService.value = $opensilex.getService('opensilex.OntologyService')
  loadTypes()
  $opensilex.updateFiltersFromURL(route.query, props.filter)
})

function resetCountState() {
  totalCount.value = null
  totalCountLoaded.value = false
  totalCountLoading.value = false
}

function refresh() {
  resetCountState()
  $opensilex.updateURLParameters(props.filter)
  tableRef.value?.setPage?.(1)
  tableRef.value?.changeCurrentPage?.(1)
  tableRef.value?.refresh?.()
}

function getFileFormat(filename?: string) {
  if (!filename) return ''
  const parts = filename.split('.')
  return parts.length > 1 ? parts.pop()?.toUpperCase() ?? '' : ''
}

async function getProvenance(uri: string) {
  if (!uri) return null

  const http = await $opensilex
    .getService('opensilex.DataService')
    .getProvenance(uri) as HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>

  return http.response.result
}

async function showDataProvenanceDetailsModal(item: any) {
  $opensilex.enableLoader()
  const result = await getProvenance(item.provenance.uri)
  const value = {
    provenance: result,
    data: item
  }
  dataProvenanceModalView.value?.setProvenanceAndBatch?.(value)
  dataProvenanceModalView.value?.show?.()
}

async function loadObjectsPath(): Promise<void> {
  const objectURIs = Object.keys(objects.value)
  if (!objectURIs || objectURIs.length === 0) {
    return
  }

  const httpObj = await ontologyService.value?.getURITypes(objectURIs)
  for (const obj of httpObj?.response?.result ?? []) {
    objectsPath.value[obj.uri] = $opensilex.getPathFromUriTypes(obj.rdf_types)
  }
}

function getTargetPath(uri: string) {
  const defaultOsPath = objectsPath.value[uri]
  if (!defaultOsPath) return ''

  let osPath = defaultOsPath.replace(':uri', encodeURIComponent(uri))

  if (props.contextUri && props.contextUri.length > 0) {
    return osPath.replace(':experiment', encodeURIComponent(props.contextUri))
  }

  return osPath.replace(':experiment', '')
}

function countDatafiles() {
  let provUris = $opensilex.prepareGetParameter(props.filter.provenance)
  if (provUris != undefined) {
    provUris = [provUris]
  }

  return service.value?.countDatafiles(
    props.filter.scientificObjects,
    props.filter.devices,
    $opensilex.prepareGetParameter(props.filter.name),
    $opensilex.prepareGetParameter(props.filter.rdf_type),
    $opensilex.prepareGetParameter(props.filter.start_date),
    $opensilex.prepareGetParameter(props.filter.end_date),
    undefined,
    props.filter.experiments,
    provUris,
    undefined
  )
}

async function loadTotalCount() {
  totalCountLoading.value = true

  try {
    const http: any = await countDatafiles()

    const count =
      http?.response?.result ??
      http?.response?.metadata?.pagination?.totalCount ??
      http?.response?.metadata?.pagination?.total ??
      0

    totalCount.value = typeof count === 'number' ? count : 0
    totalCountLoaded.value = true
  } catch (error) {
    totalCount.value = 0
    totalCountLoaded.value = true
    $opensilex.errorHandler(error)
  } finally {
    totalCountLoading.value = false
  }
}

async function searchDatafiles(options: any) {
  let provUris = $opensilex.prepareGetParameter(props.filter.provenance)
  if (provUris != undefined) {
    provUris = [provUris]
  }

  currentPage.value = (options.currentPage ?? 0) + 1
  currentPageSize.value = options.pageSize ?? 10

  const http = await service.value?.getDataFileDescriptionsByTargets(
    $opensilex.prepareGetParameter(props.filter.name),
    $opensilex.prepareGetParameter(props.filter.rdf_type),
    $opensilex.prepareGetParameter(props.filter.start_date),
    $opensilex.prepareGetParameter(props.filter.end_date),
    undefined,
    props.filter.experiments,
    props.filter.devices,
    provUris,
    undefined,
    options.orderBy,
    options.currentPage,
    options.pageSize,
    props.filter.scientificObjects
  )

  const results = http?.response?.result ?? []
  currentPageItemCount.value = results.length

  if (results.length === 0) {
    return http
  }

  const objectsToLoad: string[] = []
  const provenancesToLoad: string[] = []

  for (const item of results) {
    const objectURI = item.target
    if (objectURI != null && !objectsToLoad.includes(objectURI)) {
      objectsToLoad.push(objectURI)
    }

    const provenanceURI = item.provenance?.uri
    if (provenanceURI && !provenancesToLoad.includes(provenanceURI)) {
      provenancesToLoad.push(provenanceURI)
    }
  }

  const promiseArray: Promise<any>[] = []

  if (objectsToLoad.length > 0) {
    const promiseObject = $opensilex
      .getService('opensilex.OntologyService')
      .getURILabelsList(objectsToLoad, props.contextUri)
      .then((httpObj: any) => {
        for (const obj of httpObj.response.result) {
          objects.value[obj.uri] = `${obj.name} (${obj.rdf_type_name})`
        }
      })
    promiseArray.push(promiseObject)
  }

  if (provenancesToLoad.length > 0) {
    const promiseProvenance = $opensilex
      .getService('opensilex.DataService')
      .getProvenancesByURIs(provenancesToLoad)
      .then((httpObj: any) => {
        for (const prov of httpObj.response.result) {
          provenances.value[prov.uri] = prov.name
        }
      })
    promiseArray.push(promiseProvenance)
  }

  await Promise.all(promiseArray)
  await loadObjectsPath()

  return http
}

function showImage(item: any) {
  const path = `/core/datafiles/${encodeURIComponent(item.uri)}/thumbnail?scaled_width=600&scaled_height=600`
  $opensilex.viewImageFromGetService(path).then((result: string) => {
    imageUrl.value = result
    imageModal.value?.show?.()
  })
}

function loadTypes() {
  $opensilex.getService('opensilex.OntologyService')
    .getSubClassesOf($opensilex.Oeso.DATAFILE_TYPE_URI, false)
    .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
      images_rdf_types.value = []
      rdf_types.value = {}

      const parentType = http.response.result[0]
      if (!parentType) return

      rdf_types.value[parentType.uri] = parentType.name

      for (const child of parentType.children || []) {
        rdf_types.value[child.uri] = child.name

        if ($opensilex.Oeso.checkURIs(child.uri, $opensilex.Oeso.IMAGE_TYPE_URI)) {
          images_rdf_types.value.push(child.uri)
          for (const imageChild of child.children || []) {
            rdf_types.value[imageChild.uri] = imageChild.name
            images_rdf_types.value.push(imageChild.uri)
          }
        } else if ($opensilex.Oeso.checkURIs(child.uri, $opensilex.Oeso.SPECTRA_TYPE_URI)) {
          images_rdf_types.value.push(child.uri)
          for (const spectraChild of child.children || []) {
            rdf_types.value[spectraChild.uri] = spectraChild.name
            images_rdf_types.value.push(spectraChild.uri)
          }
        } else {
          for (const subChild of child.children || []) {
            rdf_types.value[subChild.uri] = subChild.name
          }
        }
      }
    })
    .catch($opensilex.errorHandler)
}

function redirectToDetail(uri: string) {
  const uriArr = uri.split('/')

  if (uriArr[1] === 'scientific-object') {
    router.push({
      path: '/scientific-objects/details/' + encodeURIComponent(uri)
    })
    return
  }

  if (uriArr[1] === 'device') {
    router.push({
      path: '/device/details/' + encodeURIComponent(uri)
    })
    return
  }

  emit('redirectToDetail')
}

function resetSelected() {
  tableRef.value?.resetSelection?.()
  tableRef.value?.resetSelected?.()
}

function clickOnlySelected() {
  tableRef.value?.toggleOnlySelected?.()
  tableRef.value?.clickOnlySelected?.()
}

function updateSelectedUris() {
  selectedUris.value = []
  for (const select of tableRef.value?.getSelected?.() ?? []) {
    selectedUris.value.push(select.uri)
  }
}

function createEvents() {
  showEventForm.value = true
  Promise.resolve().then(() => {
    updateSelectedUris()
    eventCsvForm.value?.show?.()
  })
}

function onDisplayDropdownSelect(key: string) {
  switch (key) {
    case 'onlySelected':
      clickOnlySelected()
      break
    case 'resetSelected':
      resetSelected()
      break
  }
}

function onActionsDropdownSelect(key: string) {
  switch (key) {
    case 'export':
      exportDataFileModal.value?.show?.()
      break
    case 'createEvents':
      createEvents()
      break
  }
}

function exportDataFiles(format: string, includeAverage: boolean, includeSampleDatetime: boolean) {
  const exportList = (tableRef.value?.getSelected?.() ?? []).map((select: any) => select.uri)

  if (format === 'img') {
    const filenames = (tableRef.value?.getSelected?.() ?? []).map((select: any) => select.filename)

    for (let i = 0; i < filenames.length; i++) {
      const path = `/core/datafiles/${encodeURIComponent(exportList[i])}/thumbnail?scaled_width=600&scaled_height=600`
      $opensilex.downloadFilefromService(
        path,
        filenames[i],
        undefined,
        null
      ).then((http: HttpResponse<OpenSilexResponse<any>>) => {
        if (http && http.status === 200) {
          $opensilex.showSuccessToast(t('DataFilesList.upload-success-message'))
        }
      }).catch((error: any) => {
        if (error.status === 500) {
          console.error('DataFile not found', error)
          $opensilex.errorHandler(
            error,
            t('DataFilesList.datafile-not-found')
          )
        } else {
          $opensilex.errorHandler(error)
        }
      })
    }
    return
  }

  const today = new Date()
  const filename =
    'export_datafiles' +
    (includeAverage ? '_avg_' : '_') +
    (includeSampleDatetime ? '_WithDatetime_' : '_') +
    $opensilex.$dateTimeFormatter.formatISODate(today)

  const path =
    `/core/datafiles/export-spectra-files?format=${format}` +
    `&includeAverage=${includeAverage}` +
    `&includeSampleDatetime=${includeSampleDatetime}`

  return $opensilex.downloadFilefromPostService(
    path,
    filename,
    format,
    exportList,
    lang.value
  ).then((http: HttpResponse<OpenSilexResponse<any>>) => {
    if (http && http.status === 200) {
      $opensilex.showSuccessToast(t('DataFilesList.upload-success-message'))
    }
  }).catch((error: any) => {
    if (error.status === 500) {
      console.error('DataFile not found', error)
      $opensilex.errorHandler(
        error,
        t('DataFilesList.datafile-not-found')
      )
    } else {
      $opensilex.errorHandler(error)
    }
  })
}

function deleteDatafile(uri: string) {
  const confirmed = window.confirm(t('component.common.delete-datafile-confirmation').toString())
  if (!confirmed) return

  $opensilex
    .getService('opensilex.DataService')
    .deleteDatafile(uri)
    .then(() => {
      tableRef.value?.checkSelectedItems?.(uri)
      refresh()
      const message = `${uri} ${t('component.common.success.delete-success-message')}`
      $opensilex.showSuccessToast(message)
    })
    .catch($opensilex.errorHandler)
}

defineExpose({
  refresh,
  resetSelected,
  clickOnlySelected,
  updateSelectedUris,
  createEvents,
  redirectToDetail,
  loadTotalCount
})
</script>

<style scoped>
.btn-disabled {
  background-color: #e0e0e0 !important;
  color: #2e2e2e !important;
  border: none !important;
  cursor: not-allowed;
}

.listActionButtons {
  position: relative;
  display: flex;
  gap: 0 !important;
  margin-bottom: 10px;
}

.displayAndListSelectionCount {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.countButton {
  color: #00A28C;

}

.totalCountDetailButton {
  background: none;
  border: none;
  color: #00A38D;
}

.totalCountDetailButton:focus {
  color: #02c5ab;
  background: none
}

</style>

<i18n>
en:
  DataFilesList:
    add: Add datafiles
    add-event: Add events
    datafile-not-found: Datafile not found
    date: Date
    display: Display
    format: Format
    filename: Filename
    selected: Selected datafiles
    selected-all: All datafiles
    export: Export datafiles
    exportDX: Export DX datafiles to DX datafile
    exportTSVAVG: Export DX datafiles to TSV datafile with average
    exportTSV: Export DX datafiles to TSV datafile
    exportCSV: Export CSV datafiles to CSV datafile
    exportTSVDATE: Export DX datafiles to TSV datafile with datetime
    error:
      datafile-not-found: Datafile not found
    provenance: Provenance
    rdfType: Type
    upload-success-message: File uploaded and processed successfully.

fr:
  DataFilesList:
    add: Ajouter un fichier de données
    add-event: Ajouter des événements
    datafile-not-found: Fichiers de données non trouvés
    date: Date
    display: Affichage
    format: Format
    filename: Nom du fichier
    selected: Fichiers de données selectionnés
    selected-all: Tous les fichiers
    export: Exporter des fichiers de données
    exportDX: Export des fichiers de données DX
    exportTSVAVG: Export des fichiers de données DX au format TSV avec moyenne
    exportTSV: Export des fichiers de données DX au format TSV
    exportCSV: Export des fichiers de données CSV
    exportTSVDATE: Export des fichiers de données DX au format TSV avec la date
    error:
      datafile-not-found: Fichiers de données non trouvés
    provenance: Provenance
    rdfType: Type
    upload-success-message: Fichier téléchargé et traité avec succès.
</i18n>