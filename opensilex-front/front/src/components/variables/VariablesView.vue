<template>
  <div class="container-fluid py-3">
    <!-- Header -->
    <opensilex-PageHeader
      icon="fa#vials"
      :hasIcon="true"
      :title="t('VariableView.title')"
      :description="t('VariableView.description')"
      class="detail-element-header"
    ></opensilex-PageHeader>

    <opensilex-PageActions
      :actions="actions"
    />

    <!-- Onglets -->
    <ul class="nav nav-tabs mb-3">
      <li class="nav-item" v-for="tab in tabs" :key="tab.key">
        <button
          class="nav-link"
          :class="{ active: currentTab === tab.key }"
          @click="currentTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </li>
    </ul>


    <opensilex-PageActions>
      <opensilex-HelpButton
        @click="openHelpModal"
        label="component.common.help-button"
        :small="true"
        class="helpButton"
      ></opensilex-HelpButton>

      <!-- <b-modal ref="helpModal" size="xl" hide-header hide-footer>
          <opensilex-VariableHelp v-if="elementType != 'VariableGroup'" @hideBtnIsClicked="hide()"></opensilex-VariableHelp>
          <opensilex-GroupVariablesHelp v-else @hideBtnIsClicked="hide()"></opensilex-GroupVariablesHelp>
      </b-modal> -->

          <!-- v-show="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)" -->
      <opensilex-CreateButton
          @click="showCreateForm"
          :label="buttonTitle"
          class="createButton"
      ></opensilex-CreateButton>
    </opensilex-PageActions>

    <!-- Contenu des onglets -->
    <div>
    <component
      v-if="currentTab === 'variables'"
      :is="tabComponents.variables"
      ref="variableCreate"
      @ready="markTabReady('variables')"
    />

    <component
      v-else-if="currentTab === 'entities'"
      :is="tabComponents.entities"
      ref="entityForm"
      @ready="markTabReady('entities')"
    />

    <component
      v-else-if="currentTab === 'interestEntity'"
      :is="tabComponents.interestEntity"
      ref="interestEntityForm"
      @ready="markTabReady('interestEntity')"
    />

    <component
      v-else-if="currentTab === 'characteristics'"
      :is="tabComponents.characteristics"
      ref="characteristicForm"
      @ready="markTabReady('characteristics')"
    />

    <component
      v-else-if="currentTab === 'methods'"
      :is="tabComponents.methods"
      ref="methodForm"
      @ready="markTabReady('methods')"
    />

    <component
      v-else-if="currentTab === 'units'"
      :is="tabComponents.units"
      ref="unitForm"
      @ready="markTabReady('units')"
    />

    <component
      v-else-if="currentTab === 'groups'"
      :is="tabComponents.groups"
      ref="groupVariablesForm"
      v-show="loadGroupForm"
      @ready="markTabReady('groups')"
    />

    </div>

    <!-- Fenêtre modale d'aide-->
    <div v-if="showHelpModal" class="modal-backdrop">
      <div class="modal-content">
        <opensilex-VariableHelp @close="closeHelpModal" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, inject, defineAsyncComponent, nextTick } from 'vue';
import PageActions from '@/components/PageActions.vue';
import { useI18n } from 'vue-i18n';
import type { OpenSilexVuePlugin } from '@/opensilex/OpenSilexVuePlugin';
import VariableHelp from "./views/VariableHelp.vue";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const { t } = useI18n();
const helpModal = ref<InstanceType<typeof VariableHelp> | null>(null);

const showHelpModal = ref(false)

function openHelpModal() {
  showHelpModal.value = true
}

function closeHelpModal() {
  showHelpModal.value = false
}

// Onglets
const tabs = computed(() => [
  { key: 'variables', label: t('component.menu.variables') },
  { key: 'entities', label: t('VariableView.entity') },
  { key: 'interestEntity', label: t('VariableView.entityOfInterest') },
  { key: 'characteristics', label: t('VariableView.characteristic') },
  { key: 'methods', label: t('VariableView.method') },
  { key: 'units', label: t('VariableView.unit') },
  { key: 'groups', label: t('VariableView.groupVariable') },
]);

const currentTab = ref<string>('variables');

// Composants dynamiques (à décommenter quand migrés)
const tabComponents: Record<string, any> = {
  variables: defineAsyncComponent(() => import('./VariableList.vue')),
  entities: defineAsyncComponent(() => import('./agroportal/AgroportalEntityForm.vue')),
  interestEntity: defineAsyncComponent(() => import('./agroportal/AgroportalEntityOfInterestForm.vue')),
  characteristics: defineAsyncComponent(() => import('./agroportal/AgroportalCharacteristicForm.vue')),
  methods: defineAsyncComponent(() => import('./agroportal/AgroportalMethodForm.vue')),
  units: defineAsyncComponent(() => import('./agroportal/AgroportalUnitForm.vue')),
  groups: defineAsyncComponent(() => import('./../groupVariable/GroupVariablesForm.vue'))
};

const currentTabComponent = computed(() => tabComponents[currentTab.value]);


// Refs vers les composants enfants
const variableCreate = ref();
const entityForm = ref();
const interestEntityForm = ref();
const characteristicForm = ref();
const methodForm = ref();
const unitForm = ref();
const groupVariablesForm = ref();
const loadGroupForm = ref(false); 

// Actions de la page
const actions = ref([
  {
    label: t('actions.create'),
    icon: 'bi bi-plus-circle',
    // TODO: ajouter une méthode d'ouverture de formulaire
    onClick: () => console.log('create clicked')
  }
]);


const readyTabs = new Set<string>();

function markTabReady(tabKey: string) {
  console.log("markTabReady tab ", tabKey)
  readyTabs.add(tabKey);
}



const tabToLabelKey = new Map<string, string>([
  ['variables', 'VariableView.add-variable'],
  ['entities', 'VariableView.add-entity'],
  ['interestEntity', 'VariableView.add-entityOfInterest'],
  ['characteristics', 'VariableView.add-characteristic'],
  ['methods', 'VariableView.add-method'],
  ['units', 'VariableView.add-unit'],
  ['groups', 'VariableView.add-groupVariable']
]);

const elementTypeToForm = new Map<string, any>([
  ['variables', variableCreate],
  ['entities', entityForm],
  ['interestEntity', interestEntityForm],
  ['characteristics', characteristicForm],
  ['methods', methodForm],
  ['units', unitForm],
  ['groups', groupVariablesForm]
]);

function getForm() {
  console.log("variablesView getForm : ", elementTypeToForm.get(currentTab.value).value)
  return elementTypeToForm.get(currentTab.value)?.value;
}

// function showCreateForm() {
//   if (currentTab.value === 'groups') {
//     loadGroupForm.value = true;
//     nextTick(() => {
//       getForm()?.showCreateForm();
//     });
//   } else {
//     console.log("variablesView showCreateForm")
//     getForm()?.showCreateForm();
//   }
// }

function showCreateForm() {
  if (!readyTabs.has(currentTab.value)) {
    console.warn(`Component pour l'onglet '${currentTab.value}' pas encore pret.`);
    return;
  }

  const form = getForm();

  if (form?.showCreateForm) {
    form.showCreateForm();
  } else {
    console.log("showCreateForm", form)
    console.warn(`showCreateForm pas dispo pour l'onglet :  '${currentTab.value}'`);
  }
}



const buttonTitle = computed(() => {
  const key = tabToLabelKey.get(currentTab.value);
  return key ? t(key) : t('actions.create');
});
</script>

<style scoped>
.nav-tabs .nav-link.active {
  font-weight: bold;
}
.nav-link {
  color: black
}

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
        unit: "Unit/Scale"
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
