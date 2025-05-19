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
        @click="showCreateForm"
        :label="buttonTitle"
        class="createButton"
      />
    </opensilex-PageActions>

    <!-- Composant dynamique selon l'onglet -->
    <component
      v-if="currentTabComponent"
      :is="currentTabComponent"
      :ref="getCurrentFormRefKey"
      @ready="markTabReady(currentTab)"
      v-on="currentTab === 'variables' ? variableListeners : {}"
      v-show="currentTab !== 'groups' || loadGroupForm"
    />

    <!-- Composant de crea/edit variable (invisible) -->
    <opensilex-VariableCreate
      ref="variableCreate"
      style="display: none;"
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

import VariableCreate from './form/VariableCreate.vue';

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const variablesService = opensilex.getService<VariablesService>("opensilex.VariablesService");
const datasService = opensilex.getService<DataService>("opensilex.DataService");
const { t } = useI18n();

// Onglets
const tabDefinitions = [
  { key: 'variables', labelKey: 'component.menu.variables', component: () => import('./VariableList.vue'), refKey: 'variableList' },
  { key: 'entities', labelKey: 'VariableView.entity', component: () => import('./agroportal/AgroportalEntityForm.vue'), refKey: 'entityForm' },
  { key: 'interestEntity', labelKey: 'VariableView.entityOfInterest', component: () => import('./agroportal/AgroportalEntityOfInterestForm.vue'), refKey: 'interestEntityForm' },
  { key: 'characteristics', labelKey: 'VariableView.characteristic', component: () => import('./agroportal/AgroportalCharacteristicForm.vue'), refKey: 'characteristicForm' },
  { key: 'methods', labelKey: 'VariableView.method', component: () => import('./agroportal/AgroportalMethodForm.vue'), refKey: 'methodForm' },
  { key: 'units', labelKey: 'VariableView.unit', component: () => import('./agroportal/AgroportalUnitForm.vue'), refKey: 'unitForm' },
  { key: 'groups', labelKey: 'VariableView.groupVariable', component: () => import('./../groupVariable/GroupVariablesForm.vue'), refKey: 'groupVariablesForm' }
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
  ...Object.fromEntries(
    tabDefinitions.map(tab => [tab.refKey, ref()])
  )
};

const variableCreate = formRefs.variableCreate; // pour lier dans le template

const currentTabComponent = computed(() => tabComponents[currentTab.value]);
const getCurrentFormRefKey = computed(() =>
  tabDefinitions.find(tab => tab.key === currentTab.value)?.refKey || ''
);

function getFormInstance() {
  const key = getCurrentFormRefKey.value;
  return formRefs[key]?.value || null;
}

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
function showCreateForm() {
  if (!readyTabs.has(currentTab.value)) {
    console.warn(`Composant pour l'onglet '${currentTab.value}' non prêt.`);
    return;
  }
  if (currentTab.value === 'groups') {
    loadGroupForm.value = true;
    nextTick(() => {
      getFormInstance()?.showCreateForm();
    });
  } else {
    getFormInstance()?.showCreateForm();
  }
}

// Edition de variable
function getCountDataPromise(variable: string) {
  return datasService.countData(undefined, undefined, undefined, undefined, [variable], undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, 1, undefined);
}

function onEditVariable(uri: string) {
  console.log("formRefs : ", formRefs)
  if (!formRefs.variableCreate) return;

  getCountDataPromise(uri).then(countResult => {
    if (countResult?.response) {
      variablesService.getVariable(uri).then((getResult: HttpResponse<OpenSilexResponse>) => {
        if (getResult?.response) {
          console.log("response")
          const form = getResult.response.result;
          const linkedDataNb = countResult.response.result;
          console.log("linkedDatanb ", linkedDataNb)
          formRefs.variableCreate.value?.showEditForm(form, linkedDataNb);
        }
      });
    }
  });
}

// Event binding conditionnel
const variableListeners = {
  edit: onEditVariable,
  interoperability: (item: any) => console.log("Interop:", item),
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
