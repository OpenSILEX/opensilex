<template>
  <div
    class="container-fluid"
    v-if="factor.uri"
  >
    <b-row>
      <b-col md="5">
        <Card label="component.factor.details.description">
          <template v-slot:rightHeader>
            <div class="ml-3">
              <EditButton
                v-if="
                  user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID) &&
                  xpUri == factor.experiment
                "
                @click="factorForm.showEditForm(factor)"
                variant="outline-primary"
                label="component.factor.update-button"
              ></EditButton>
              <InteroperabilityButton
                v-if="
                  user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID) &&
                  xpUri == factor.experiment
                "
                label="component.skos.update"
                @click="skosReferences.show()"
              ></InteroperabilityButton>
              <DeleteButton
                v-if="
                  user.hasCredential(credentials.CREDENTIAL_FACTOR_DELETE_ID) &&
                  xpUri == factor.experiment
                "
                :small="true"
                label="component.common.list.buttons.delete"
                @click="$emit('onDelete')"
              ></DeleteButton>
            </div>
          </template>

          <template v-slot:body>
            <LabelUriView
              label="component.factor.uri"
              :uri="factor.uri"
            ></LabelUriView>
            <StringView
              label="component.factor.name"
              :value="factor.name"
            ></StringView>
            <StringView
              label="component.factor.category"
              :value="getCategoryLabel()"
            ></StringView>
            <StringView
              label="component.factor.description"
              :value="factor.description"
            ></StringView>
            <MetadataView
              v-if="factor.publisher && factor.publisher.uri"
              :publisher="factor.publisher"
              :publicationDate="factor.publication_date"
              :lastUpdatedDate="factor.last_updated_date"
            ></MetadataView>
          </template>
        </Card>

        <Card
          label="component.skos.ontologies-references-label"
          icon="fa#globe-americas"
        >
          <template v-slot:body>
            <ExternalReferencesDetails
              v-if="factor.uri != null"
              :skosReferences="factor"
            ></ExternalReferencesDetails>
          </template>
        </Card>
      </b-col>

      <b-col>
        <Card
          label="component.factor.details.factorLevels"
          icon="fa#list"
        >
          <template v-slot:body>
            <TableView
              v-if="factor.levels != undefined && factor.levels.length > 0"
              filterPlaceholder="component.factor.details.search"
              :items="factor.levels"
              :fields="factorLevelFields"
              :globalFilterField="true"
            >
              <template v-slot:export>
                <b-button
                  class="mb-2 mr-2"
                  variant="secondary"
                  @click="exportFactorLevels()"
                  >{{ $t('component.factor.details.export') }}
                </b-button>
              </template>
              <template v-slot:cell(name)="{ data }">
                <UriLink
                  :uri="data.item.uri"
                  :value="data.item.name"
                ></UriLink>
              </template>
            </TableView>

            <p v-else>
              <strong>{{ $t('component.factor.details.no-factorLevels-provided') }}</strong>
            </p>
          </template>
        </Card>
      </b-col>
    </b-row>
    <ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)"
      ref="factorForm"
      modalSize="lg"
      :tutorial="true"
      :successMessage="successMessage"
      component="FactorForm"
      createTitle="component.factor.add"
      editTitle="component.factor.update"
      icon="fa#sun"
    ></ModalForm>

    <ExternalReferencesModalForm
      ref="skosReferences"
      :references.sync="factor"
      @onUpdate="updateReferences"
    ></ExternalReferencesModalForm>
  </div>
</template>

<script setup lang="ts">
import Vue, { computed, inject, onMounted, onUnmounted, ref, useTemplateRef } from 'vue';
// @ts-ignore
import { FactorsService } from 'core/index';
import Card from '@/components/common/views/Card.vue';
import EditButton from '@/components/common/buttons/EditButton.vue';
import DeleteButton from '@/components/common/buttons/DeleteButton.vue';
import LabelUriView from '@/components/common/views/LabelUriView.vue';
import StringView from '@/components/common/views/StringView.vue';
import MetadataView from '@/components/common/views/MetadataView.vue';
import ExternalReferencesDetails from '@/components/common/external-references/ExternalReferencesDetails.vue';
import TableView from '@/components/common/views/TableView.vue';
import ExternalReferencesModalForm from '@/components/common/external-references/ExternalReferencesModalForm.vue';
import OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin';
import { useStore } from 'vuex';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { NForm } from 'naive-ui';
import UriLink from '@/components/common/views/UriLink.vue';

const opensilex = inject<OpenSilexVuePlugin>('$opensilex');
const store = useStore();
const route = useRoute();
const { t } = useI18n();
const service = opensilex.getService<FactorsService>('opensilex.FactorsService');
const categoryName = ref<string>('');

const skosReferences =
  useTemplateRef<InstanceType<typeof ExternalReferencesModalForm>>('skosReferences');
const factorForm = useTemplateRef<any>('factorForm');

const user = computed(() => {
  return store.state.user;
});

const credentials = computed(() => {
  return store.state.credentials;
});

function beforeDestroy() {
  langUnwatcher();
}

function exportFactorLevels() {
  // Format levels in array
  let levels = props.factor.levels;
  let rows: any = [['uri', 'name', 'description']];
  levels.forEach((level) => {
    rows.push([
      level.uri,
      level.name,
      level.description == null || undefined ? '' : level.description,
    ]);
  });

  // Create csv content
  let csvContent = rows.map((e) => e.join(',')).join('\n');
  // download
  let fileLink = document.createElement('a');
  var blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  var url = URL.createObjectURL(blob);
  fileLink.href = url;
  fileLink.setAttribute('download', 'export_' + props.factor.name + '_factors_levels');
  fileLink.click();
}

const emit = defineEmits([
  'onReload',
  'update:experiment',
  'onUpdate',
  'onUpdateReferences',
  'onDelete',
]);

let langUnwatcher: (() => void) | undefined;

onMounted(() => {
  langUnwatcher = store.watch(
    () => store.getters.language,
    async () => {
      await opensilex.loadFactorCategories();
      emit('onReload', props.factor.uri);
    }
  );
});

onUnmounted(() => {
  langUnwatcher?.();
});

function getCategoryLabel() {
  return opensilex.getFactorCategoryName(props.factor.category);
}

function created() {
  const service = opensilex.getService<FactorsService>('opensilex.FactorsService');
}

const props = defineProps({
  factor: {
    default: () => ({
      uri: null,
      name: null,
      category: null,
      description: null,
      experiment: null,
      exact_match: [],
      close_match: [],
      broad_match: [],
      narrow_match: [],
      levels: [],
      publisher: null,
      publication_date: null,
      last_updated_date: null,
    }),
  },
  experiment: {
    type: String,
    default: null,
  },
});

const xpUri = computed({
  get: () => props.experiment,
  set: (value) => emit('update:experiment', value),
});

interface FactorLevelField {
  key: string;
  label: string;
  sortable: boolean;
}

const factorLevelFields: FactorLevelField[] = [
  {
    key: 'name',
    label: 'component.factorLevel.name',
    sortable: true,
  },
  {
    key: 'description',
    label: 'component.factorLevel.description',
    sortable: false,
  },
];
function update() {
  emit('onUpdate');
}

function updateReferences() {
  emit('onUpdateReferences', props.factor);
}

function successMessage(factor) {
  return t('component.factor.label') + ' ' + factor.name;
}
</script>

<style scoped lang="scss">
.details-actions-row {
  margin-top: -35px;
  margin-left: -15px;
  margin-right: 15px;
}
::v-deep .static-field .uri {
  padding-left: 0px !important;
}
</style>
<i18n>
en:
  component:
    factor :
      details:
        label: Details
        description: Description
        factorLevels: Levels
        no-factorLevels-provided: No factor levels provided
        search: Search in name and description
        export: Export all
fr:
  component:
    factor:
      details:
        label: Détails
        description: Description
        factorLevels: Niveaux de facteurs associés
        no-factorLevels-provided: Aucun niveau de facteur associé
        search: Recherche dans nom et description
        export: Tout exporter

</i18n>
