<template>
  <div class="page">
    <div class="two-cols">
      <!-- Colonne 1 : Infos principales -->
      <div>
        <opensilex-Card :no-footer="true" :label="t('component.common.informations')" icon="bi-clipboard">
          
            
          <template #rightHeader>
            <opensilex-EditButton
              v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID) && displayLocalActions"
              label="VariableDetails.edit"
              variant="outline-primary"
              @click="showEditForm"
              :small="true"
            />

            <opensilex-VariableCreate
              v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
              ref="variableForm"
              @onUpdate="$emit('onUpdate', $event)"
            />

            <opensilex-InteroperabilityButton
              v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID) && displayLocalActions"
              label="VariableDetails.edit-references"
              @click="skosReferences?.show?.()"
              :small="true"
            />

            <opensilex-ExternalReferencesModalForm
              v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
              ref="skosReferences"
              v-model:references="mutableVariable"
              @onUpdate="update"
            />

            <opensilex-DeleteButton
              v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_DELETE_ID) && displayLocalActions"
              @click="deleteVariable"
              label="component.common.list.buttons.delete"
              :small="true"
            />
          </template>

          <template #body>
            <opensilex-UriView
              v-if="mutableVariable && mutableVariable.uri"
              :uri="mutableVariable.uri"
              :allowCopy="true"
            />
            <opensilex-StringView
              label="component.common.name"
              :value="mutableVariable?.name"
            />
            <opensilex-StringView
              label="component.variable.altName"
              :value="mutableVariable?.alternative_name"
            />
            <opensilex-TextView
              label="component.common.description"
              :value="mutableVariable?.description"
            />

            <div v-if="mutableVariable?.from_shared_resource_instance">
              <opensilex-UriView
                title="component.sharedResourceInstances.label"
                :uri="mutableVariable.from_shared_resource_instance.apiUrl"
                :value="mutableVariable.from_shared_resource_instance.label"
              />
            </div>

            <opensilex-MetadataView
              v-if="mutableVariable?.publisher?.uri"
              :publisher="mutableVariable.publisher"
              :publicationDate="mutableVariable.publication_date"
              :lastUpdatedDate="mutableVariable.last_updated_date"
            />
          </template>
        </opensilex-Card>
      </div>

      <!-- Colonne 2 : Structure -->
      <div>
        <opensilex-Card :no-footer="true" :label="t('VariableDetails.structure')" icon="bi-clipboard">
          <template #body>
            <opensilex-UriView
              title="component.variable.entity.entity"
              v-if="mutableVariable?.entity"
              :value="mutableVariable.entity.name"
              :uri="mutableVariable.entity.uri"
              :to="getEntityPath()"
              :allowCopy="true"
            />
            <opensilex-UriView
              title="component.variable.entityOfInterest.entityOfInterest"
              :value="mutableVariable?.entity_of_interest ? mutableVariable.entity_of_interest.name : undefined"
              :uri="mutableVariable?.entity_of_interest ? mutableVariable.entity_of_interest.uri : undefined"
              :to="mutableVariable?.entity_of_interest ? getInterestEntityPath() : undefined"
              :allowCopy="true"
            />
            <opensilex-UriView
              title="component.variable.characteristic.characteristic"
              v-if="mutableVariable?.characteristic"
              :value="mutableVariable.characteristic.name"
              :uri="mutableVariable.characteristic.uri"
              :to="getCharacteristicPath()"
              :allowCopy="true"
            />
            <opensilex-UriView
              title="component.variable.method.method"
              v-if="mutableVariable?.method"
              :value="mutableVariable.method.name"
              :uri="mutableVariable.method.uri"
              :to="getMethodPath()"
              :allowCopy="true"
            />
            <opensilex-UriView
              title="component.variable.unit.unit"
              v-if="mutableVariable?.unit"
              :value="mutableVariable.unit.name"
              :uri="mutableVariable.unit.uri"
              :to="getUnitPath()"
              :allowCopy="true"
            />
          </template>
        </opensilex-Card>
      </div>
    </div>

    <div class="two-cols mt-3">
      <!-- Références SKOS -->
      <div>
        <opensilex-Card label="component.skos.ontologies-references-label" icon="fa#globe-americas" :no-footer="true">
            
          <template #body>
            <opensilex-ExternalReferencesDetails :skosReferences="mutableVariable" />
          </template>
        </opensilex-Card>
      </div>

      <!-- Avancé -->
      <div>
        <opensilex-Card :no-footer="true" :label="t('VariableDetails.advanced')" icon="bi-clipboard">
          <template #body>
            <opensilex-UriListView
              v-if="!isGermplasmMenuExcluded"
              label="component.common.species"
              :list="speciesList"
              :allowCopy="true"
            />
            <opensilex-StringView
            label="component.variable.dataType.data-type"
            :value="mutableVariable?.datatype
                        ? opensilex.getVariableDatatypeLabel(mutableVariable.datatype)
                        : ''"
            />
                <!-- :value="mutableVariable?.datatype ? t(`${mutableVariable.datatype}`) : ''" -->

            <opensilex-StringView
              label="component.variable.timeInterval.time-interval"
              :value="mutableVariable?.time_interval"
            />
            <opensilex-StringView
              label="component.variable.samplingInterval.sampling-interval"
              :value="mutableVariable?.sampling_interval"
            />
            <opensilex-UriView
              v-if="mutableVariable?.trait"
              title="VariableForm.trait-uri"
              :uri="mutableVariable.trait"
              :url="mutableVariable.trait"
            />
            <opensilex-StringView
              v-if="mutableVariable?.trait"
              label="VariableForm.trait-name"
              :value="mutableVariable.trait_name"
            />
          </template>
        </opensilex-Card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, reactive, ref, watch } from 'vue';
import { useStore } from 'vuex';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';

import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin';
import DTOConverter from '../../models/DTOConverter';

import HttpResponse, { OpenSilexResponse } from 'opensilex-core/HttpResponse';
import type {
  VariablesService,
  DataService,
  VariableDetailsDTO,
  VariableUpdateDTO
} from 'opensilex-core/index';

// Props / Emits
const props = withDefaults(defineProps<{
  variable: VariableDetailsDTO | null
  displayLocalActions?: boolean
}>(), {
  variable: null,
  displayLocalActions: true
});

const emit = defineEmits<{
  (e: 'onUpdate', v: VariableDetailsDTO): void
}>();

// Services & env
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const variablesService = opensilex.getService<VariablesService>('opensilex.VariablesService');
const dataService = opensilex.getService<DataService>('opensilex.DataService');
const { t } = useI18n();
const store = useStore();
const route = useRoute();
const router = useRouter();

// Store
const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);

// Local copy modifiable (car en Vue 3, les props sont readonly)
const mutableVariable = ref<VariableDetailsDTO | null>(props.variable ?? null);
watch(() => props.variable, (v) => { mutableVariable.value = v ? JSON.parse(JSON.stringify(v)) : null; }, { immediate: true });

// Refs enfants
const variableForm = ref<any>(null);
const skosReferences = ref<any>(null);

// Listes & flags
const speciesList = computed(() => {
  const v = mutableVariable.value;
  console.log("mutableVariable : ", mutableVariable)
  if (!v?.species) return [];
  return v.species.map((s: any) => ({
    uri: s.uri,
    value: s.name,
    to: { path: `/germplasm/details/${encodeURIComponent(s.uri)}` }
  }));
});

const isGermplasmMenuExcluded = computed(() => {
  try {
    return opensilex.getConfig().menuExclusions.includes('germplasm');
  } catch {
    return false;
  }
});

// Actions
function showEditForm() {
  const uri = mutableVariable.value?.uri as string;
  if (!uri) return;

  getCountDataPromise(uri).then((http) => {
    const count = http?.response?.result ?? 0;
    const copy = JSON.parse(JSON.stringify(mutableVariable.value));
    variableForm.value?.showEditForm?.(copy, count);
  }).catch(opensilex.errorHandler);
}

function update(updatedVar: VariableDetailsDTO) {
  const formatted: VariableUpdateDTO = DTOConverter.extractURIFromResourceProperties(updatedVar);
  variablesService.updateVariable(formatted).then(() => {
    const message = `${t('VariableView.name')} ${formatted.name} ${t('component.common.success.update-success-message')}`;
    opensilex.showSuccessToast(message);
    emit('onUpdate', updatedVar);
  }).catch(opensilex.errorHandler);
}

function deleteVariable() {
  const uri = mutableVariable.value?.uri as string;
  if (!uri) return;

  getCountDataPromise(uri).then((http) => {
    const count = http?.response?.result ?? 0;
    if (count > 0) {
      opensilex.showErrorToast(`${count} ${t('VariableView.associated-data-error')}`);
    } else {
      variablesService.deleteVariable(uri).then(() => {
        const message = `${t('VariableView.name')} ${mutableVariable.value?.name} ${t('component.common.success.delete-success-message')}`;
        opensilex.showSuccessToast(message);
        router.push({ path: '/variables' });
      }).catch(opensilex.errorHandler);
    }
  });
}

function getCountDataPromise(variableUri: string) {
  // Check if there exist at least one data linked to the variable
  return dataService.countData(
    undefined, undefined, undefined, undefined, undefined,
    [variableUri], // variables
    undefined, undefined, undefined, undefined, undefined,
    undefined, undefined, 1, undefined, undefined
  ) as Promise<HttpResponse<OpenSilexResponse<number>>>;
}

// Navigation vers VariablesView pré-filtré
const ELEMENT_TYPES = {
  ENTITY: 'ENTITY_TYPE',
  INTEREST_ENTITY: 'INTEREST_ENTITY_TYPE',
  CHARACTERISTIC: 'CHARACTERISTIC_TYPE',
  METHOD: 'METHOD_TYPE',
  UNIT: 'UNIT_TYPE'
} as const;

function getPath(elementType: string, uri?: string) {
  if (route.query.sharedResourceInstance) return undefined;
  if (!uri) return undefined;
  return { path: `/variables/?elementType=${elementType}&selected=${encodeURIComponent(uri)}` };
}

function getEntityPath() {
  return getPath(ELEMENT_TYPES.ENTITY, mutableVariable.value?.entity?.uri);
}
function getInterestEntityPath() {
  return getPath(ELEMENT_TYPES.INTEREST_ENTITY, mutableVariable.value?.entity_of_interest?.uri);
}
function getCharacteristicPath() {
  return getPath(ELEMENT_TYPES.CHARACTERISTIC, mutableVariable.value?.characteristic?.uri);
}
function getMethodPath() {
  return getPath(ELEMENT_TYPES.METHOD, mutableVariable.value?.method?.uri);
}
function getUnitPath() {
  return getPath(ELEMENT_TYPES.UNIT, mutableVariable.value?.unit?.uri);
}
</script>

<style scoped lang="scss">
.page { margin-top: 20px; }

/* grille simple 2 colonnes responsive */
.two-cols {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}
@media (min-width: 992px) {
  .two-cols {
    grid-template-columns: 1fr 1fr;
  }
}
</style>

<i18n>
en:
  VariableDetails:
    title: Detailled variable view
    entity-name: Entity name
    characteristic-name: Characteristic name
    method-name: Method name
    unit-name: Unit name
    structure: Structure
    advanced: Advanced information
    edit: Edit variable
    edit-references: Edit references
    visualization: Device associated Data Visualization
fr:
  VariableDetails:
    title: Vue détaillée de la variable
    entity-name: Nom d'entité
    characteristic-name: Nom de la caractéristique
    method-name: Nom de méthode
    unit-name: Nom d'unité
    structure: Structure
    advanced: Informations avancées
    edit: Editer la variable
    edit-references: Editer les références
    visualization: Visualisation des données associées à un appareil
</i18n>
