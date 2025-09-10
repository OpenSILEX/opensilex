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
      :ref="tabDefinitions.find(tab => tab.key === currentTab)?.refKey"
      :elementType="elementType"
    />
      <!-- v-show="currentTab !== 'groups' || loadGroupForm" -->
      <!-- @ready="markTabReady(currentTab)" -->

    <!-- Composant de crea/edit variable (invisible) -->
    <opensilex-VariableCreate
      ref="variableCreate" 
      v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
    />

    <opensilex-ExternalReferencesModalForm
      ref="skosReferences"
      v-model:references="selected"
      :includeAgroportalSearch="true"
      @onUpdate="updateReferences"
    />

    <!-- Modale de création/édition d’un groupe de variables -->
    <opensilex-VariableGroupCreate
    ref="variableGroupCreate"
    v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"

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
import { ref, computed, inject, defineAsyncComponent, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin';
import { VariablesService, DataService } from 'opensilex-core/index';
import HttpResponse, { OpenSilexResponse } from 'opensilex-core/HttpResponse';
import { useStore } from "vuex";
import VariableCreate from './form/VariableCreate.vue';
import VariableGroupCreate from './form/VariableGroupCreate.vue';


const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const variablesService = opensilex.getService<VariablesService>("opensilex.VariablesService");
const datasService = opensilex.getService<DataService>("opensilex.DataService");
const { t } = useI18n();
const store = useStore();
const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);

// Onglets
const tabDefinitions = [
  { key: 'variables', labelKey: 'component.menu.variables', component: () => import('./VariableList.vue'), refKey: 'variableList' },
  { key: 'entities', labelKey: 'VariableView.entity', component: () => import('./agroportal/AgroportalEntityForm.vue'), refKey: 'entityForm' },
  { key: 'interestEntity', labelKey: 'VariableView.entityOfInterest', component: () => import('./agroportal/AgroportalEntityOfInterestForm.vue'), refKey: 'interestEntityForm' },
  { key: 'characteristics', labelKey: 'VariableView.characteristic', component: () => import('./agroportal/AgroportalCharacteristicForm.vue'), refKey: 'characteristicForm' },
  { key: 'methods', labelKey: 'VariableView.method', component: () => import('./agroportal/AgroportalMethodForm.vue'), refKey: 'methodForm' },
  { key: 'units', labelKey: 'VariableView.unit', component: () => import('./agroportal/AgroportalUnitForm.vue'), refKey: 'unitForm' },
  { key: 'groups', labelKey: 'VariableView.groupVariable', component: () => import('./../groupVariable/GroupVariablesView.vue'), refKey: 'groupVariablesView' }
];

const tabs = computed(() =>
  tabDefinitions.map(({ key, labelKey }) => ({ key, label: t(labelKey) }))
);

const currentTab = ref('variables');
const readyTabs = new Set<string>();

// Composants asynchrones & références
const tabComponents = Object.fromEntries(
  tabDefinitions.map(tab => [tab.key, defineAsyncComponent(tab.component)])
);

// ajout de toutes les refs, y compris celle pour VariableCreate
const formRefs = {
  variableCreate: ref(null),
  variableGroupCreate: ref(null),
  ...Object.fromEntries(
    tabDefinitions.map(tab => [tab.refKey, ref()])
  )
};

const variableCreate = formRefs.variableCreate; // pour lier dans le template
const variableGroupCreate = formRefs.variableGroupCreate;

const currentTabComponent = computed(() => tabComponents[currentTab.value]);


// Actions
const actions = ref([
  {
    label: t('actions.create'),
    icon: 'bi bi-plus-circle',
    onClick: () => showCreateForm()
  }
]);

const tabToLabelKey = new Map<string, string>([
  ['variables', 'VariableView.add-variable'],
  ['entities', 'VariableView.add-entity'],
  ['interestEntity', 'VariableView.add-entityOfInterest'],
  ['characteristics', 'VariableView.add-characteristic'],
  ['methods', 'VariableView.add-method'],
  ['units', 'VariableView.add-unit'],
  ['groups', 'VariableView.add-groupVariable']
]);

const buttonTitle = computed(() => {
  const key = tabToLabelKey.get(currentTab.value);
  return key ? t(key) : t('actions.create');
});

// Gestion modale
const showHelpModal = ref(false);
function openHelpModal() {
  showHelpModal.value = true;
}
function closeHelpModal() {
  showHelpModal.value = false;
}

// Onglets prêts
function markTabReady(tabKey: string) {
  readyTabs.add(tabKey);
}

// Création
const loadGroupForm = ref(false);

const tabRefMap = {
  variables: formRefs.variableCreate,
  entities: formRefs.entityForm,
  interestEntity: formRefs.interestEntityForm,
  characteristics: formRefs.characteristicForm,
  methods: formRefs.methodForm,
  units: formRefs.unitForm,
  groups: formRefs.variableGroupCreate
};

const tabToElementType = {
  variables: 'VARIABLE_TYPE',
  entities: 'ENTITY_TYPE',
  interestEntity: 'INTEREST_ENTITY_TYPE',
  characteristics: 'CHARACTERISTIC_TYPE',
  methods: 'METHOD_TYPE',
  units: 'UNIT_TYPE',
  groups: 'GROUP_VARIABLE_TYPE'
};

const elementType = computed(() => tabToElementType[currentTab.value] || 'VARIABLE_TYPE');


function showCreateForm() {
  if (currentTab.value === 'groups') {
    loadGroupForm.value = true;
    nextTick(() => {
      tabRefMap[currentTab.value]?.value?.showCreateForm();
    });
  } else {
    tabRefMap[currentTab.value]?.value?.showCreateForm();
  }
}


// Edition de variable

let currentEditRequest = null;

async function onEditVariable(uri: string) {
  if (currentEditRequest) {
    console.warn("Édition déjà en cours, annulation de la nouvelle demande.");
    return;
  }

  try {
    currentEditRequest = variablesService.getVariable(uri);
    const getResult = await currentEditRequest;
    currentEditRequest = null;

    if (getResult?.response) {
      formRefs.variableCreate.value?.showEditForm(getResult.response.result);
    }
  } catch (e) {
    console.error(e);
    currentEditRequest = null;
  }
}

const selected = ref(null);
const skosReferences = ref(null); // accès à la modale

const showVariableReferences = async (uri: string) => {
  try {
    const http = await variablesService.getVariable(uri);
    selected.value = http.response.result;
    skosReferences.value?.show(); // appelle la méthode show() sur la modale
  } catch (error) {
    opensilex.errorHandler(error);
  }
};


// Event binding conditionnel
const variableListeners = {
  edit: onEditVariable,
    onInteroperability: (variable: any) => {
    showVariableReferences(variable);
  },
  delete: (item: any) => console.log("Delete:", item),
  reset: () => console.log("Reset")
};
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
        description : Gérer et configurer les variables, entités, charactéristiques, méthodes et unités
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
