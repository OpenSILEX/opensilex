<template>
  <div>
    <opensilex-ProjectForm
      v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)"
      ref="projectForm"
      class="projectDescription"
      @onUpdate="loadProject"
    />

    <div v-if="project" class="row">
      <div class="col col-xl-5" style="min-width: 400px">
        <!-- General Informations Card -->
        <opensilex-Card icon="bi-clipboard" :no-footer="true" :label="t('component.common.informations')">
          <!-- Boutons -->  
          <template #rightHeader>
              <opensilex-EditButton
                v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)"
                @click="showEditForm"
                label="component.project.update"
                :small="true"
              />
              <opensilex-DeleteButton
                v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_DELETE_ID)"
                @click="deleteProject(project.uri)"
                label="component.project.delete"
                :small="true"
              />
          </template>

          <template #body>
            <div class="detailsCard">
            <opensilex-StringView label="component.project.name" :value="project.name" />

            <div class="static-field">
              <span class="field-view-title">{{ t('component.common.state') }}</span>
              <span class="static-field-line">
                <!-- Etat: en cours -->
                <span
                  v-if="!isEnded(project)"
                  class="badge badge-pill badge-info-phis"
                  :title="t('component.project.common.status.in-progress')"
                >
                  <i class="bi bi-activity badge-icon badge-info-opensilex"></i>
                  {{ t('component.project.common.status.in-progress') }}
                </span>
                <!-- Etat : terminé -->
                <span
                  v-else
                  class="badge badge-pill"
                  :title="t('component.project.common.status.finished')"
                >
                  <i class="bi bi-archive"></i>
                  {{ t('component.project.common.status.finished') }}
                </span>
              </span>
            </div>

            <opensilex-StringView label="component.common.date-time.period" :value="period" />
            <opensilex-UriView :uri="project.uri" />
            <opensilex-StringView label="component.project.shortname" :value="project.shortname" />
            <opensilex-TextView
              label="component.project.financialFunding"
              :value="project.financial_funding"
            />
            <opensilex-UriView
              title="component.project.website"
              :value="project.website"
              :uri="project.website"
              :allowCopy="true"
            />
            <opensilex-UriListView
              label="component.project.relatedProjects"
              :list="relatedProjectsList"
              :allowCopy="true"
            />
            <opensilex-TextView label="component.project.objective" :value="project.objective" />
            <opensilex-TextView label="component.project.description" :value="project.description" />

            <opensilex-MetadataView
              v-if="project.publisher && project.publisher.uri"
              :publisher="project.publisher"
              :publicationDate="project.publication_date"
              :lastUpdatedDate="project.last_updated_date"
            />
            </div>
          </template>
        </opensilex-Card>
      </div>

      <div class="col col-xl-7">
        <!-- Contacts Card -->
        <opensilex-Card label="component.common.contacts" icon="bi-people" :no-footer="true">
          <template #body>
          <div class="detailsCard">
            <opensilex-ContactsList
              label="component.project.scientificContacts"
              :list="scientificContactsList"
            />
            <opensilex-ContactsList
              label="component.project.coordinators"
              :list="coordinatorsList"
            />
            <opensilex-ContactsList
              label="component.project.administrativeContacts"
              :list="administrativeContactsList"
            />
          </div>
          </template>
        </opensilex-Card>

        <opensilex-AssociatedExperimentsList
          :searchMethod="loadExperiments"
          v-model:nameFilter="experimentName"
          :debounce="300"
          :lazy="false"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { useI18n } from 'vue-i18n'

import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'

import type { ProjectsService, ProjectGetDetailDTO } from 'opensilex-core/index'
import type { ExperimentsService } from 'opensilex-core/api/experiments.service'
import type { SecurityService, PersonDTO } from 'opensilex-security/index'

import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const store = useStore()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

const projectForm = ref<any>(null)
const uri = ref<string>('')

const project = ref<ProjectGetDetailDTO | null>(null)
const period = ref<string>('')

const scientificContactsList = ref<PersonDTO[]>([])
const coordinatorsList = ref<PersonDTO[]>([])
const administrativeContactsList = ref<PersonDTO[]>([])

type UriListItem = { uri: string; value: string; to?: any }
const relatedProjectsList = ref<UriListItem[]>([])

const experimentName = ref<string>('')

// services
const projectsService = computed(() =>
  $opensilex.getService<ProjectsService>('opensilex.ProjectsService')
)
const securityService = computed(() =>
  $opensilex.getService<SecurityService>('opensilex.SecurityService')
)
const experimentsService = computed(() =>
  $opensilex.getService<ExperimentsService>('opensilex.ExperimentsService')
)

function syncUriFromRoute() {
  const raw = route.params.uri
  const param = Array.isArray(raw) ? raw[0] : (raw as string | undefined)
  uri.value = param ? decodeURIComponent(param) : ''
}

function computePeriod() {
  if (!project.value) {
    period.value = ''
    return
  }
  period.value = $opensilex.$dateTimeFormatter.formatPeriod(
    project.value.start_date,
    project.value.end_date
  )
}

async function loadProject() {
  if (!uri.value) return
  try {
    const http: HttpResponse<OpenSilexResponse<ProjectGetDetailDTO>> =
      await projectsService.value.getProject(uri.value)
    project.value = http.response.result
    computePeriod()
    await Promise.all([loadPersonsContact(), loadRelatedProject()])
  } catch (e) {
    $opensilex.errorHandler(e)
  }
}

function showEditForm() {
  if (!project.value) return
  const copy = JSON.parse(JSON.stringify(project.value))
  projectForm.value?.showEditForm?.(copy)
}

async function deleteProject(targetUri: string) {
  try {
    await projectsService.value.deleteProject(targetUri)
    const msg = `${t('ProjectList.name')} ${targetUri} ${t('component.common.success.delete-success-message')}`
    $opensilex.showSuccessToast(msg)
    router.push({ path: '/projects' })
  } catch (e) {
    $opensilex.errorHandler(e)
  }
}

async function loadRelatedProject() {
  relatedProjectsList.value = []
  const relatedProjects = project.value?.related_projects ?? []
  if (!relatedProjects.length) return

  await Promise.all(
    relatedProjects.map(async (relatedUri: string) => {
      try {
        const http: HttpResponse<OpenSilexResponse<ProjectGetDetailDTO>> =
          await projectsService.value.getProject(relatedUri)
        const detail = http.response.result
        relatedProjectsList.value.push({
          uri: detail.uri,
          value: detail.shortname || detail.name,
          to: { path: '/project/details/' + encodeURIComponent(detail.uri) }
        })
      } catch (e) {
        $opensilex.errorHandler(e)
      }
    })
  )
}

async function loadPersonsContact() {
  scientificContactsList.value = []
  coordinatorsList.value = []
  administrativeContactsList.value = []

  const selectedProject = project.value
  if (!selectedProject) return

  try {
    if (selectedProject.scientific_contacts?.length) {
      const http: HttpResponse<OpenSilexResponse<PersonDTO[]>> =
        await securityService.value.getPersonsByURI(selectedProject.scientific_contacts)
      scientificContactsList.value = http.response.result ?? []
    }
    if (selectedProject.coordinators?.length) {
      const http: HttpResponse<OpenSilexResponse<PersonDTO[]>> =
        await securityService.value.getPersonsByURI(selectedProject.coordinators)
      coordinatorsList.value = http.response.result ?? []
    }
    if (selectedProject.administrative_contacts?.length) {
      const http: HttpResponse<OpenSilexResponse<PersonDTO[]>> =
        await securityService.value.getPersonsByURI(selectedProject.administrative_contacts)
      administrativeContactsList.value = http.response.result ?? []
    }
  } catch (e) {
    $opensilex.errorHandler(e)
  }
}

function isEnded(selectedProject: any) {
  if (selectedProject?.end_date) return new Date(selectedProject.end_date).getTime() < Date.now()
  return false
}

/** search experiments */
function loadExperiments(options: any) {
  return experimentsService.value.searchExperiments(
    experimentName.value, // name
    undefined, // year
    undefined, // isEnded
    undefined, // species
    undefined, // factors
    [uri.value], // projects
    undefined, // isPublic
    undefined,
    undefined,
    options.orderBy,
    options.currentPage,
    options.pageSize
  )
}

/** init + react route changes */
onMounted(() => {
  // localStorage comme avant
  const routeArr = route.path.split('/')
  localStorage.setItem('tabPath', routeArr[2] ?? '')
  localStorage.setItem('tabPage', '1')

  syncUriFromRoute()
  loadProject()
})

watch(
  () => route.params.uri,
  () => {
    syncUriFromRoute()
    loadProject()
  }
)

/** Recalcul période quand la langue change*/
watch(
  () => store.getters.language,
  () => {
    computePeriod()
  }
)
</script>

<style lang="scss">
.detailsCard {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.badge-info-phis {
  color: black
}
</style>
