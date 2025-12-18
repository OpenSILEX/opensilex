<template>
  <div class="container-fluid py-3">
    <!-- Header -->
    <opensilex-PageHeader
      icon="fa#vials"
      :hasIcon="true"
      :title="t('VariableView.title')"
      :description="t('VariableView.description')"
      class="detail-element-header"
    />

    <!-- Actions principales -->
    <opensilex-PageActions :actions="actions" />

    <!-- Onglets -->
    <nav class="tabs mb-3">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="['tab', { active: currentTab === tab.key }]"
        @click="currentTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </nav>

    <!-- Actions secondaires -->
    <opensilex-PageActions>
      <opensilex-HelpButton
        @click="openHelpModal"
        label="component.common.help-button"
        :small="true"
        class="helpButton"
      />

      <opensilex-CreateButton
        v-show="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
        @click="showCreateForm"
        :label="buttonTitle"
        class="createButton"
      />
    </opensilex-PageActions>

    <!-- Composant dynamique selon l'onglet -->
    <component
      v-if="currentTabComponent"
      :is="currentTabComponent"
      v-on="currentTab === 'variables' ? variableListeners : {}"
      :ref="currentTabRef"
      :elementType="elementType"
    />

    <!-- Composant de crea/edit variable (invisible) -->
    <opensilex-VariableCreate
      ref="variableCreate"
      v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
      @onCreate="afterVariableSaved"
      @onUpdate="afterVariableSaved"
    />

    <opensilex-ExternalReferencesModalForm
      ref="skosReferences"
      v-model:references="selected"
      :includeAgroportalSearch="true"
      @onUpdate="updateReferences"
    />

    <!-- Modale de création/édition d’une entité -->
    <opensilex-AgroportalEntityForm
      ref="entityForm"
      v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
      @onCreate="form => onExternalResourceCreatedOrUpdated('entities', form)"
      @onUpdate="form => onExternalResourceCreatedOrUpdated('entities', form)"
    />

    <!-- Modale d'édition d’une entité d'intérêt -->
    <opensilex-AgroportalEntityOfInterestForm
      ref="interestEntityForm"
      v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
      @onCreate="form => onExternalResourceCreatedOrUpdated('interestEntity', form)"
      @onUpdate="form => onExternalResourceCreatedOrUpdated('interestEntity', form)"
    />

    <!-- Modale d'édition d’une caractéristique -->
    <opensilex-AgroportalCharacteristicForm
      ref="characteristicForm"
      v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
      @onCreate="form => onExternalResourceCreatedOrUpdated('characteristics', form)"
      @onUpdate="form => onExternalResourceCreatedOrUpdated('characteristics', form)"
    />

    <!-- Modale d'édition d’une méthode -->
    <opensilex-AgroportalMethodForm
      ref="methodForm"
      v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
      @onCreate="form => onExternalResourceCreatedOrUpdated('methods', form)"
      @onUpdate="form => onExternalResourceCreatedOrUpdated('methods', form)"
    />

    <!-- Modale d'édition d’une unité -->
    <opensilex-AgroportalUnitForm
      ref="unitForm"
      v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
      @onCreate="form => onExternalResourceCreatedOrUpdated('units', form)"
      @onUpdate="form => onExternalResourceCreatedOrUpdated('units', form)"
    />

    <!-- Modale de création/édition d’un groupe de variables -->
    <opensilex-VariableGroupCreate
      ref="variableGroupForm"
      v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
      @onCreate="form => onExternalResourceCreatedOrUpdated('groups', form)"
      @onUpdate="form => onExternalResourceCreatedOrUpdated('groups', form)"
    />

    <!-- Modale d'aide -->
    <teleport to="body">
      <div v-if="showHelpModal" class="modal-backdrop">
        <div class="modal-content">
          <component
            :is="currentTab === 'groups' ? 'opensilex-GroupVariablesHelp' : 'opensilex-VariableHelp'"
            @close="closeHelpModal"
          />
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, type Ref, computed, inject, defineAsyncComponent, nextTick, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin'
import { useRoute, useRouter } from 'vue-router'
import { VariablesService, DataService } from 'opensilex-core/index'
import HttpResponse, { OpenSilexResponse } from 'opensilex-core/HttpResponse'
import { useStore } from 'vuex'

// imports de composants de formulaires
import VariableCreate from './form/VariableCreate.vue'
import VariableGroupCreate from './form/VariableGroupCreate.vue'
import AgroportalEntityForm from './agroportal/AgroportalEntityForm.vue'
import AgroportalEntityOfInterestForm from './agroportal/AgroportalEntityOfInterestForm.vue'
import AgroportalCharacteristicForm from './agroportal/AgroportalCharacteristicForm.vue'
import AgroportalMethodForm from './agroportal/AgroportalMethodForm.vue'
import AgroportalUnitForm from './agroportal/AgroportalUnitForm.vue'

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const variablesService = opensilex.getService<VariablesService>('opensilex.VariablesService')
const datasService = opensilex.getService<DataService>('opensilex.DataService')
const { t } = useI18n()
const store = useStore()
const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)
const route = useRoute()
const router = useRouter()

// ----------------------
// Onglets
// ----------------------
const tabDefinitions = [
  { key: 'variables',       labelKey: 'component.menu.variables',     component: () => import('./VariableList.vue'),         refKey: 'variableList' },
  { key: 'entities',        labelKey: 'VariableView.entity',          component: () => import('./views/EntitiesView.vue'),   refKey: 'entitiesView' },
  { key: 'interestEntity',  labelKey: 'VariableView.entityOfInterest',component: () => import('./views/EntityOfInterestView.vue'), refKey: 'entityOfInterestView' },
  { key: 'characteristics', labelKey: 'VariableView.characteristic',  component: () => import('./views/CharacteristicsView.vue'), refKey: 'characteristicsView' },
  { key: 'methods',         labelKey: 'VariableView.method',          component: () => import('./views/MethodView.vue'),    refKey: 'methodView' },
  { key: 'units',           labelKey: 'VariableView.unit',            component: () => import('./views/UnitsView.vue'),     refKey: 'unitView' },
  { key: 'groups',          labelKey: 'VariableView.groupVariable',   component: () => import('./../groupVariable/GroupVariablesView.vue'), refKey: 'groupVariablesView' }
] as const

const tabs = computed(() =>
  tabDefinitions.map(({ key, labelKey }) => ({ key, label: t(labelKey) }))
)

const currentTab = ref<'variables' | 'entities' | 'interestEntity' | 'characteristics' | 'methods' | 'units' | 'groups'>('variables')
const readyTabs = new Set<string>()

// Composants asynchrones
const tabComponents = Object.fromEntries(
  tabDefinitions.map(tab => [tab.key, defineAsyncComponent(tab.component)])
) as Record<(typeof tabDefinitions)[number]['key'], any>

// ----------------------
// Refs centralisées
// ----------------------
const formRefs: Record<string, Ref<any>> = {
  // modales
  variableCreate: ref(null),
  entityForm: ref(null),
  characteristicForm: ref(null),
  methodForm: ref(null),
  unitForm: ref(null),
  interestEntityForm: ref(null),
  variableGroupForm: ref(null),
}

// on ajoute les refs des vues d’onglets
tabDefinitions.forEach(tab => {
  formRefs[tab.refKey] = ref(null)
})

const variableCreate = formRefs['variableCreate']
const variableGroupForm = formRefs['variableGroupForm']
const entityForm = formRefs['entityForm']
const interestEntityForm = formRefs['interestEntityForm']
const characteristicForm = formRefs['characteristicForm']
const methodForm = formRefs['methodForm']
const unitForm = formRefs['unitForm']

const currentTabComponent = computed(() => tabComponents[currentTab.value])

const currentTabRef = computed(() => {
  const def = tabDefinitions.find(t => t.key === currentTab.value)
  return def ? formRefs[def.refKey] : undefined
})

// ----------------------
// Actions / titres
// ----------------------
const actions = ref([
  {
    label: t('actions.create'),
    icon: 'bi bi-plus-circle',
    onClick: () => showCreateForm()
  }
])

const tabToLabelKey = new Map<string, string>([
  ['variables', 'VariableView.add-variable'],
  ['entities', 'VariableView.add-entity'],
  ['interestEntity', 'VariableView.add-entityOfInterest'],
  ['characteristics', 'VariableView.add-characteristic'],
  ['methods', 'VariableView.add-method'],
  ['units', 'VariableView.add-unit'],
  ['groups', 'VariableView.add-groupVariable']
])

const buttonTitle = computed(() => {
  const key = tabToLabelKey.get(currentTab.value)
  return key ? t(key) : t('actions.create')
})

// ----------------------
// Gestion modale d'aide
// ----------------------
const showHelpModal = ref(false)
function openHelpModal() {
  showHelpModal.value = true
}
function closeHelpModal() {
  showHelpModal.value = false
}

// ----------------------
// Gestion des formulaires de création
// ----------------------
const loadGroupForm = ref(false)

const tabRefMap: Record<string, Ref<any>> = {
  variables: formRefs['variableCreate'],
  entities: formRefs['entityForm'],
  interestEntity: formRefs['interestEntityForm'],
  characteristics: formRefs['characteristicForm'],
  methods: formRefs['methodForm'],
  units: formRefs['unitForm'],
  groups: formRefs['variableGroupForm']
}

function showCreateForm() {
  if (currentTab.value === 'groups') {
    loadGroupForm.value = true
    nextTick(() => {
      tabRefMap[currentTab.value]?.value?.showCreateForm?.()
    })
  } else {
    tabRefMap[currentTab.value]?.value?.showCreateForm?.()
  }
}

// ----------------------
// Mapping des vues externes (entité d’intérêt, caractéristique, méthode, unité)
// ----------------------
const externalViewRefs = {
  entities: formRefs['entitiesView'],
  interestEntity: formRefs['entityOfInterestView'],
  characteristics: formRefs['characteristicsView'],
  methods: formRefs['methodView'],
  units: formRefs['unitView'],
  groups: formRefs['groupVariablesView']
} as const

function onExternalResourceCreatedOrUpdated(type: keyof typeof externalViewRefs, form: any) {
  console.log('[VariablesView] onExternalResourceCreatedOrUpdated', type, form)
  const viewRef = externalViewRefs[type]
  const view = viewRef?.value
  // Chaque vue doit exposer onFormSuccess via defineExpose({ onFormSuccess })
  view?.onFormSuccess?.(form)
}

// ----------------------
// URL / routing : elementType dans la query
// ----------------------
const tabToElementType = {
  variables: 'Variable',
  entities: 'Entity',
  interestEntity: 'InterestEntity',
  characteristics: 'Characteristic',
  methods: 'Method',
  units: 'Unit',
  groups: 'VariableGroup'
} as const

const elementType = computed(() => tabToElementType[currentTab.value] || 'Variable')

const elementTypeToTab = Object.fromEntries(
  Object.entries(tabToElementType).map(([key, value]) => [value, key])
) as Record<string, keyof typeof tabToElementType>

watch(currentTab, key => {
  const val = tabToElementType[key]
  const currentEl = typeof route.query.elementType === 'string' ? route.query.elementType : undefined

  if (currentEl === val) {
    router.replace({ query: { ...route.query, elementType: val } })
  } else {
    const { selected, ...rest } = route.query
    router.replace({ query: { ...rest, elementType: val } })
  }
})

const initTabFromQuery = () => {
  const pathElementType = route.query.elementType as string | undefined
  if (pathElementType && elementTypeToTab[pathElementType]) {
    currentTab.value = elementTypeToTab[pathElementType]
  }
}
initTabFromQuery()

watch(
  () => route.query.elementType,
  pathElementType => {
    if (
      typeof pathElementType === 'string' &&
      elementTypeToTab[pathElementType] &&
      currentTab.value !== elementTypeToTab[pathElementType]
    ) {
      currentTab.value = elementTypeToTab[pathElementType]
    }
  }
)

// ----------------------
// Variables : édition / suppression
// ----------------------
let currentEditRequest: Promise<any> | null = null

async function onEditVariable(uri: string) {
  if (currentEditRequest) {
    console.warn('Édition déjà en cours, annulation de la nouvelle demande.')
    return
  }

  try {
    currentEditRequest = variablesService.getVariable(uri)
    const getResult = await currentEditRequest
    currentEditRequest = null

    if (getResult?.response) {
      formRefs['variableCreate'].value?.showEditForm(getResult.response.result)
    }
  } catch (e) {
    console.error(e)
    currentEditRequest = null
  }
}

async function onDeleteVariable(item: any) {
  const uri: string | undefined = typeof item === 'string' ? item : item?.uri
  if (!uri) {
    console.warn('onDeleteVariable: aucune URI fournie')
    return
  }

  try {
    const http: HttpResponse<OpenSilexResponse<number>> = await datasService.countData(
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      [uri],
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      1,
      undefined,
      undefined
    )
    const count = http?.response?.result ?? 0

    if (count > 0) {
      opensilex.showErrorToast(`${count} ${t('VariableView.associated-data-error')}`)
      return
    }

    const deletVarResponse: HttpResponse<OpenSilexResponse<string>> = await variablesService.deleteVariable(uri)

    formRefs['variableList'].value?.refresh?.()
    const message = `${t('VariableView.name')} ${uri} ${t('component.common.success.delete-success-message')}`
    opensilex.showSuccessToast(message)

    return deletVarResponse
  } catch (error: any) {
    const status = error?.httpStatus || error?.response?.code || error?.status
    if (status === 409) {
      opensilex.showErrorToast(t('VariableView.associated-data-error') as string)
      return
    }
    opensilex.errorHandler(error)
  }
}

function afterVariableSaved() {
  formRefs['variableList'].value?.refresh?.()
}

// ----------------------
// Références / interop
// ----------------------
const selected = ref<any | null>(null)
const skosReferences = ref<any | null>(null)

const showVariableReferences = async (uri: string) => {
  try {
    const http = await variablesService.getVariable(uri)
    selected.value = http.response.result
    skosReferences.value?.show?.()
  } catch (error) {
    opensilex.errorHandler(error)
  }
}

function updateReferences() {
  // TODO ? logique de mise à jour des références SKOS
}

// Events pour la liste de variables
const variableListeners = {
  edit: onEditVariable,
  onInteroperability: (variable: any) => {
    showVariableReferences(variable)
  },
  delete: onDeleteVariable,
  reset: () => console.log('Reset')
}
</script>


<style scoped>
.tabs {
  display: flex;
  gap: 1rem;
  border-bottom: 1px solid #dee2e6;
}
.tab {
  padding: 0.5rem 1rem;
  border: none;
  background: none;
  cursor: pointer;
  border-bottom: 2px solid transparent;
}
.tab.active {
  font-weight: bold;
  border-bottom-color: #007bff;
}
/* .modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
}
.modal-content {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  max-width: 90%;
  max-height: 90%;
  overflow: auto;
} */

.createButton, .helpButton{
  margin: -10px 15px 5px -10px
}

.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-content {
  background: white;
  padding: 2rem;
  border-radius: 10px;
  max-width: 800px;
  width: 100%;
}
</style>


<i18n>
en:
    VariableView:
        name: The variable
        title: Variables
        type: Variable
        description : Manage and configure variables, entities, characteristics, methods and units
        add-variable: Add variable
        entity: Entity
        add-entity: Add entity
        entity-placeholder: Plant
        entityOfInterest: Entity of interest
        add-entityOfInterest: Add observation level
        entityOfInterest-placeholder: Canopy
        characteristic: Characteristic
        add-characteristic: Add characteristic
        characteristic-placeholder: Height
        method: Method
        add-method: Add method
        method-placeholder: Image analysis
        unit: Unit/Scale
        add-unit: Add unit
        groupVariable: Group of variables
        groupVariableAssociated: Group of associated variables
        add-groupVariable: Add a group of variables
        no-var-provided: No variable provided
        associated-data-error: Data are associated with this variable

fr:
    VariableView:
        name: La variable
        title: Variables
        type: Variable
        description : Gérer et configurer les variables, entités, caractéristiques, méthodes et unités
        add-variable: Ajouter une variable
        entity: Entité
        add-entity: Ajouter une entité
        entity-placeholder: Plante
        entityOfInterest: Entité d'intérêt
        add-entityOfInterest: Ajouter un niveau d'observation
        entityOfInterest-placeholder: Canopée
        characteristic: Caractéristique
        add-characteristic: Ajouter une caractéristique
        characteristic-placeholder: Hauteur
        method: Méthode
        add-method: Ajouter une méthode
        method-placeholder: Analyse d'image
        unit: "Unité/Echelle"
        add-unit: Ajouter une unité
        groupVariable: Groupe de variables
        groupVariableAssociated: Groupe de variables associées
        add-groupVariable: Ajouter un groupe de variables
        no-var-provided: Aucune variable associée
        associated-data-error: Données sont associées à cette variable

</i18n>
