<template>
  <div>
    <!-- CreateButton position on top for FacilityListView -->
    <div class="spaced-actions" v-if="withActions">
      <opensilex-CreateButton
        v-if="user.hasCredential(credentials.CREDENTIAL_FACILITY_MODIFICATION_ID)"
        @click="facilityFormRef?.showCreateForm?.()"
        :label="t(createButtonLabel)"
        class="createButton"
      />
    </div>

    <div class="card">
      <div class="card-header d-flex align-items-start justify-content-between">
        <h5>
          {{ t('FacilitiesView.facilities') }}

          <!-- CreateButton position on card for OrganizationView -->
          <span v-if="!withActions">
            <opensilex-CreateButton
              v-if="user.hasCredential(credentials.CREDENTIAL_FACILITY_MODIFICATION_ID)"
              @click="facilityFormRef?.showCreateForm?.()"
              :label="t(createButtonLabel)"
              class="createButton"
            />
          </span>
          &nbsp;

          <n-tooltip trigger="hover" placement="top">
            <template #trigger>
              <font-awesome-icon icon="question-circle" class="facilitiesHelp" />
            </template>
            {{ t('FacilitiesView.facility-help') }}
          </n-tooltip>
        </h5>
      </div>

      <!-- Facilities table -->
      <opensilex-TableView
        ref="tableViewRef"
        :items="displayableFacilities"
        :fields="fields"
        :globalFilterField="true"
        filterPlaceholder="component.facility.filter-placeholder"
        :sortBy="fetchAndShowCurrentExperiments ? 'experiment_count' : 'rdf_type_name'"
        :sortDesc="fetchAndShowCurrentExperiments"
        :selectable="isSelectable"
        @row-selected="onFacilitySelected"
      >
        <template #cell(name)="{ data }">
          <opensilex-UriLink
            :to="{ path: '/facility/details/' + encodeURIComponent(data.item.uri) }"
            :uri="data.item.uri"
            :value="data.item.name"
          />
        </template>

        <template
          v-if="fetchAndShowCurrentExperiments"
          #cell(experiment_count)="{ data }"
        >
          <opensilex-ExperimentsModalList
            :experiments="experimentByFacility[data.item.uri] || []"
            :currentFacility="data.item"
          />
        </template>

        <template #cell(rdf_type_name)="{ data }">
          <span class="capitalize-first-letter">{{ data.item.rdf_type_name }}</span>
        </template>

        <template v-if="withActions" #cell(actions)="{ data }">
          <n-button-group size="small">
            <opensilex-EditButton
              v-if="user.hasCredential(credentials.CREDENTIAL_FACILITY_MODIFICATION_ID)"
              @click="editFacility(data.item)"
              :label="t('FacilitiesView.update')"
              :small="true"
            />
            <opensilex-DeleteButton
              v-if="user.hasCredential(credentials.CREDENTIAL_FACILITY_DELETE_ID)"
              @click="deleteFacility(data.item.uri)"
              :label="t('FacilitiesView.delete')"
              :small="true"
            />
          </n-button-group>
        </template>
      </opensilex-TableView>

      <opensilex-FacilityModalForm
        v-if="withActions && user.hasCredential(credentials.CREDENTIAL_FACILITY_MODIFICATION_ID)"
        ref="facilityFormRef"
        @onCreate="onCreate"
        @onUpdate="onUpdate"
        :initForm="initForm"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import { NCard, NTooltip, NButtonGroup } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { OrganizationsService } from 'opensilex-core/api/organizations.service'
import type { ExperimentsService } from 'opensilex-core/api/experiments.service'
import type {
  FacilityCreationDTO,
  FacilityGetDTO,
  NamedResourceDTO,
  NamedResourceDTOFacilityModel,
  NamedResourceDTOOrganizationModel,
  NamedResourceDTOSiteModel
} from 'opensilex-core/index'
import type { ExperimentGetListDTO } from 'opensilex-core/model/experimentGetListDTO'
import type HttpResponse from '@/lib/HttpResponse'
import type { OpenSilexResponse } from '@/lib/HttpResponse'

type FacilityModelWithExperimentCount = NamedResourceDTOFacilityModel & { experiment_count: number }

const emit = defineEmits<{
  (e: 'facilitySelected', selected: FacilityGetDTO): void
  (e: 'onUpdate'): void
  (e: 'onCreate'): void
  (e: 'onDelete', uri: string): void
}>()

const props = withDefaults(
  defineProps<{
    organization?: NamedResourceDTOOrganizationModel
    site?: NamedResourceDTOSiteModel
    facilities?: NamedResourceDTOFacilityModel[]
    isSelectable?: boolean
    withActions?: boolean
    fetchAndShowCurrentExperiments?: boolean
    createButtonLabel?: string
  }>(),
  {
    isSelectable: false,
    withActions: false,
    fetchAndShowCurrentExperiments: false,
    createButtonLabel: 'FacilitiesView.add'
  }
)

const { t } = useI18n()
const store = useStore()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = $opensilex.getService<OrganizationsService>('opensilex-core.OrganizationsService')
const experimentsService = $opensilex.getService<ExperimentsService>('opensilex-core.ExperimentsService')

// refs (child components)
const facilityFormRef = ref<any>(null)
const tableViewRef = ref<any>(null)

// computed
const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

// data
const fetchedFacilities = ref<NamedResourceDTOFacilityModel[]>([])
const currentExperiments = ref<ExperimentGetListDTO[]>([])
const filter = ref('')

const useFetchedFacilities = computed(() => !Array.isArray(props.facilities))

const experimentByFacility = computed<Record<string, ExperimentGetListDTO[]>>(() => {
  const filteredExpe: Record<string, ExperimentGetListDTO[]> = {}
  for (const exp of currentExperiments.value) {
    for (const facilityUri of (exp as any).facilities || []) {
      if (!filteredExpe[facilityUri]) filteredExpe[facilityUri] = []
      filteredExpe[facilityUri].push(exp)
    }
  }
  return filteredExpe
})

function addExperimentCountIfNecessary(
  facility: NamedResourceDTOFacilityModel
): NamedResourceDTOFacilityModel | FacilityModelWithExperimentCount {
  if (!props.fetchAndShowCurrentExperiments) return facility
  const count = experimentByFacility.value[facility.uri]?.length ?? 0
  return { ...(facility as any), experiment_count: count }
}

const displayableFacilities = computed<(NamedResourceDTOFacilityModel | FacilityModelWithExperimentCount)[]>(() => {
  const base = useFetchedFacilities.value ? fetchedFacilities.value : (props.facilities ?? [])
  const withCount = base.map(addExperimentCountIfNecessary)

  // comme avant : filtrage local seulement si facilities vient des props
  if (useFetchedFacilities.value) return withCount

  if (!filter.value) return withCount
  const re = new RegExp(filter.value, 'i')
  return withCount.filter((f: any) => (f?.name ?? '').match(re))
})

const fields = computed(() => {
  const f: any[] = [
    { key: 'name', label: t('component.common.name'), sortable: true, resizable: true,}
  ]

  if (props.fetchAndShowCurrentExperiments) {
    f.push({ key: 'experiment_count', label: t('FacilitiesView.experiment_count'), sortable: true })
  }

  f.push({ key: 'rdf_type_name', label: t('component.common.type'), sortable: true })

  if (props.withActions) {
    f.push({ key: 'actions', label: t('component.common.actions'), sortable: false })
  }
  return f
})

function onFacilitySelected(selected: FacilityGetDTO) {
  emit('facilitySelected', selected)
}

function onUpdate() {
  emit('onUpdate')
}

function onCreate() {
  emit('onCreate')
}

async function deleteFacility(uri: string) {
  try {
    await service.deleteFacility(uri)

    const title = t('OrganizationFacilityForm.name', {}, { default: t('FacilitiesView.facilities') })
    const msg = `${title} ${uri} ${t('component.common.success.delete-success-message')}`

    $opensilex.showSuccessToast(msg)
    emit('onDelete', uri)
  } catch (e) {
    $opensilex.errorHandler(e)
  }
}

async function fetchFacilities(): Promise<void> {
  const http = await service.minimalSearchFacilities(filter.value) as unknown as HttpResponse<OpenSilexResponse<NamedResourceDTO[]>>
  fetchedFacilities.value = (http as any).response.result
}

function experimentIsEnded(experiment: ExperimentGetListDTO): boolean {
  return !!(experiment.end_date && new Date(experiment.end_date) < new Date())
}

async function fetchExperiments(): Promise<void> {
  const facilitiesUris = displayableFacilities.value.map((f: any) => f.uri)

  const http = await experimentsService.searchExperiments(
    '', // label
    undefined, // year
    null, // is_ended
    null, // species
    null, // factorCategories
    null, // projects
    null, // isPublic
    facilitiesUris, // facilities
    null,
    null,
    0,
    0
  ) as unknown as HttpResponse<OpenSilexResponse<ExperimentGetListDTO[]>>

  const experiments = (http as any).response.result as ExperimentGetListDTO[]
  currentExperiments.value = experiments.filter(e => !experimentIsEnded(e))
}

async function refresh(): Promise<void> {
  $opensilex.showLoader()
  try {
    if (useFetchedFacilities.value) await fetchFacilities()
    if (props.fetchAndShowCurrentExperiments) await fetchExperiments()

    // Select the first element
    if (props.isSelectable && displayableFacilities.value.length > 0) {
      tableViewRef.value?.selectRow?.(0)
    }
  } finally {
    $opensilex.hideLoader()
  }
}

function initForm(form: FacilityCreationDTO) {
  if (props.organization) form.organizations = [props.organization.uri]
  if (props.site) form.sites = [props.site.uri]
}

function editFacility(facility: FacilityGetDTO) {
  facilityFormRef.value?.showEditForm?.(facility.uri)
}

onMounted(() => {
  refresh()
})

defineExpose({
  refresh,
  deleteFacility
})
</script>

<style scoped lang="scss">
.facilitiesHelp {
  font-size: 1.3em;
  background: #f1f1f1;
  color: #00A38D;
  border-radius: 50%;
}

.spaced-actions {
  margin-top: -15px;
  margin-bottom: 10px;
}
.card{
  margin-bottom: -20px !important;
}
</style>

<i18n>
en:
  FacilitiesView:
    update: Update facility
    delete: Delete facility
    add: Add facility
    facilities: Facilities
    facility-help: "Factilities correspond to the various fixed installations of an organization.
                                  These can be greenhouses, fields, culture chambers, growth chambers ..."
    experiment_count: "Current experiments"
fr:
  FacilitiesView:
    update: Modifier l'installation environnementale
    delete: Supprimer l'installation environnementale
    add: Ajouter une installation environnementale
    facilities: Installations environnementales
    facility-help: "Les installations environnementales correspondent aux différentes installations fixes d'une organisation.
                                  Il peut s'agir de serres, champs, chambres de culture, chambres de croissance ..."
    experiment_count: "Expériences en cours"
</i18n>
