<template>
  <div>
    <ValidationObserver ref="validatorRef">
      <b-form>
        <p v-if="skosReferences">
          {{ t('component.skos.addTo') }}
          <em>
            <strong class="text-primary">{{ skosReferences.uri}}</strong>
          </em>
        </p>

        <div
            class="row"
            v-if="includeAgroportalSearch && isAgroportalReachable"
        >
          <div class="col">
            <AgroportalSearch
                label="component.common.name"
                type="text"
                placeholder="search"
                v-model:selected="ontologies"
                v-model:isAllOntologies="isAllOntologies"
                @change="onSearchTextChange"
            />

            <AgroportalResults
                ref="searchResults"
                v-model:text="text"
                :isMappingMode="true"
                :mappingOptions="options"
                :importMapping="onImportMapping"
            />
          </div>
        </div>

        <b-card bg-variant="light">
          <div class="row">
            <div class="col">
              <b-form-group
                  label="component.skos.ontologies-references-label"
                  label-size="lg"
                  label-class="font-weight-bold pt-0"
                  class="mb-0"
              >
                <template v-slot:label>
                  {{ t('component.skos.ontologies-references-label') }}
                </template>
              </b-form-group>

              <b-card-text>
                <ul>
                  <li
                      v-for="externalOntologyRef in externalOntologiesRefs"
                      :key="externalOntologyRef.name"
                  >
                    <a
                        target="_blank"
                        :title="externalOntologyRef.name"
                        :href="externalOntologyRef.link"
                        v-b-tooltip.v-info.hover.left="externalOntologyRef.description"
                    >
                      {{ externalOntologyRef.name }}
                    </a>
                  </li>
                </ul>
              </b-card-text>
            </div>

            <div class="col">
              <FilterField :fullWidth="true">
                <FormSelector
                    label="component.skos.relation"
                    helpMessage="component.skos.relation-help"
                    placeholder="component.skos.no-relation"
                    v-model:selected="currentRelation"
                    :options="options"
                    :requiredBlue="true"
                />
              </FilterField>

              <!-- URI -->
              <FilterField :fullWidth="true">
<!--                <b-form-group>-->
<!--                  <div class="helperAndBlueStar">-->
<!--                    <FormInputLabelHelper-->
<!--                        label="component.skos.uri"-->
<!--                        helpMessage="component.skos.uri-help"-->
<!--                    />-->
<!--                    <pre class="blueStar"> *</pre>-->
<!--                  </div>-->

<!--                  <ValidationProvider-->
<!--                      :name="t('component.skos.uri')"-->
<!--                      :rules="{-->
<!--                      required: true,-->
<!--                      regex: /^(http:\/\/www\.|https:\/\/www\.|http:\/\/|https:\/\/)?[a-z0-9]+([\-\.]{1}[a-z0-9]+)*\.[a-z]{2,5}(:[0-9]{1,5})?(\/.*)?$/-->
<!--                    }"-->
<!--                      v-slot="{ errors }"-->
<!--                  >-->
<!--                    <span-->
<!--                        class="error-message alert alert-danger"-->
<!--                        v-if="isIncludedInRelations()"-->
<!--                    >-->
<!--                      {{ t('component.skos.external-already-existing') }}-->
<!--                    </span>-->

<!--                    <b-form-input-->
<!--                        id="externalUri"-->
<!--                        v-model.trim="currentExternalUri"-->
<!--                        type="text"-->
<!--                        required-->
<!--                        :placeholder="t('component.skos.uri-placeholder')"-->
<!--                        debounce="300"-->
<!--                    />-->

<!--                    <div class="error-message alert alert-danger">-->
<!--                      {{ errors[0] }}-->
<!--                    </div>-->
<!--                  </ValidationProvider>-->
<!--                </b-form-group>-->

                <b-form-group label-align-sm="right">
                  <b-button
                      @click="addRelationsToSkosReferences"
                      class="greenThemeColor"
                  >
                    {{ t('component.skos.add') }}
                  </b-button>
                </b-form-group>
              </FilterField>
            </div>
          </div>
        </b-card>

        <b-form-group
            v-if="displayInsertButton"
            label-align-sm="right"
        >
          <b-button
              class="float-right"
              @click="update"
          >
            {{ t("component.skos.update") }}
          </b-button>
        </b-form-group>
      </b-form>
    </ValidationObserver>

    <div>
      <b-table
          v-if="relations.length !== 0"
          striped
          hover
          small
          responsive
          sort-icon-left
          bordered
          :items="relations"
          :fields="fields"
      >
        <template v-slot:head(relation)="data">
          {{ t(data.label) }}
        </template>

        <template v-slot:cell(relation)="data">
          {{ t(data.value) }}
        </template>

        <template v-slot:head(relationURI)="data">
          {{ t(data.label) }}
        </template>

        <template v-slot:cell(relationURI)="data">
          <a :href="data.value" target="_blank">
            {{ data.value }}
          </a>
        </template>

        <template v-slot:head(actions)="data">
          {{ t(data.label) }}
        </template>

        <template v-slot:cell(actions)="data">
          <div class="text-center">
            <b-button-group size="md">
              <b-button
                  size="md"
                  @click="removeRelationsToSkosReferences(data.item)"
                  variant="danger"
              >
                <Icon icon="fa#trash-alt" />
              </b-button>
            </b-button-group>
          </div>
        </template>
      </b-table>

      <p v-else>
        <strong>
          {{ t('component.skos.no-external-links-provided') }}
        </strong>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { useStore } from "vuex";
import { useI18n } from "vue-i18n";

import SUPPORTED_SKOS_RELATIONS from "../../../models/SkosRelations";
import { ExternalOntologies } from "../../../models/ExternalOntologies";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import { AgroportalAPIService } from "opensilex-core/api/agroportalAPI.service";
import { AgroportalTermDTO } from "opensilex-core/model/agroportalTermDTO";
import HttpResponse from "../../../lib/HttpResponse";

import AgroportalSearch from "@/components/common/external-references/agroportal/AgroportalSearch.vue";
import AgroportalResults from "@/components/common/external-references/agroportal/AgroportalResults.vue";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import FormInputLabelHelper from "@/components/common/forms/FormInputLabelHelper.vue";
import Icon from "@/components/common/views/Icon.vue";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const store = useStore();
const { t } = useI18n();

const props = withDefaults(
    defineProps<{
      displayInsertButton: boolean;
      includeAgroportalSearch: boolean;
      ontologiesToSelect: string[];
    }>(),
    {
      displayInsertButton: true,
      includeAgroportalSearch: false,
      ontologiesToSelect: () => []
    }
);

const emit = defineEmits<{
  (e: "onAdd", value: any, callback: (result: any) => void): void;
  (e: "onDelete", value: any, callback: (result: any) => void): void;
  (e: "onUpdate", value: any, callback: (result: any) => void): void;
}>();

const skosReferences = defineModel('references')

const agroportalAPIService = ref<AgroportalAPIService>();

const currentRelation = ref<string>("");
const currentExternalUri = ref<string>("");
const text = ref<string>("");
const ontologies = ref<string[]>([]);
const isAllOntologies = ref<boolean>(false);

const validatorRef = ref<any>();

const externalOntologiesRefs = computed<any[]>(() => {
  if (!props.ontologiesToSelect) {
    return [];
  }

  return ExternalOntologies
      .getExternalOntologiesReferences(props.ontologiesToSelect)
      .map(ref => ({
        ...ref,
        description: t(ref.description)
      }));
});

const isAgroportalReachable = ref<boolean>(false);

function checkAgroportalReachable() {
  agroportalAPIService.value.pingAgroportal()
      .then((http) => {
        if (http && http.response) {
          isAgroportalReachable.value = http.response.result;
        }
      })
      .catch((error: HttpResponse) => {
        if (error.status === 503) {
          isAgroportalReachable.value = false;
          return;
        }

        opensilex?.errorHandler(error);
      });
}

const relationsInternal = ref<any[]>([]);
const options = ref<any[]>([]);

function setOptions() {
  options.value = [];

  for (const skosRelation of SUPPORTED_SKOS_RELATIONS) {
    options.value.push({
      id: skosRelation.dtoKey,
      label: t(skosRelation.label),
      title: t(skosRelation.description)
    });
  }
}

onMounted(() => {
  setOptions();

  agroportalAPIService.value =
      opensilex?.getService<AgroportalAPIService>(
          "opensilex.AgroportalAPIService"
      );

  checkAgroportalReachable();

  langUnwatcher = store.watch(
      () => store.getters.language,
      () => {
        setOptions();
      }
  );
});

let langUnwatcher: (() => void) | undefined;

onBeforeUnmount(() => {
  if (langUnwatcher) {
    langUnwatcher();
  }
});

function resetForm() {
  currentRelation.value = "";
  currentExternalUri.value = "";
}

function resetExternalUriForm() {
  currentExternalUri.value = "";

  nextTick(() => {
    validatorRef.value.reset();
  });
}

const fields = [
  {
    key: "relation",
    label: "component.skos.relation",
    sortable: true
  },
  {
    key: "relationURI",
    label: "component.skos.uri",
    sortable: false
  },
  {
    key: "actions",
    label: "component.common.actions"
  }
];

const relations = computed(() => {
  relationsInternal.value = [];

  for (const skosRelation of SUPPORTED_SKOS_RELATIONS) {
    updateRelations(
        skosRelation.dtoKey,
        skosReferences.value[skosRelation.dtoKey]
    );
  }

  return relationsInternal.value;
});

function updateRelations(
    relation: string,
    references: string[]
) {
  if (references !== undefined) {
    for (let index = 0; index < references.length; index++) {
      const element = references[index];
      addRelation(relation, element);
    }
  }
}

function addRelation(
    relation: string,
    externalUri: string
) {
  const skosRelation = [...SUPPORTED_SKOS_RELATIONS]
      .find(r => r.dtoKey === relation);

  if (skosRelation) {
    relationsInternal.value.push({
      relation: skosRelation.label,
      relationURI: externalUri
    });
  }
}

function validateForm() {
  const validator = validatorRef.value;
  return validator.validate();
}

function addRelationsToSkosReferences() {
  validateForm().then(isValid => {
    if (isValid) {
      addRelationToSkosReferences();

      return new Promise((resolve, reject) => {
        emit("onAdd", skosReferences.value, result => {
          if (result instanceof Promise) {
            result.then(resolve).catch(reject);
          } else {
            resolve(result);
          }
        });
      });
    }
  });
}

function addRelationToSkosReferences() {
  if (!isIncludedInRelations()) {
    skosReferences.value[currentRelation.value].push(
        currentExternalUri.value
    );

    resetExternalUriForm();
  }
}

function isIncludedInRelations(): boolean {
  if (
      currentExternalUri.value == undefined ||
      currentExternalUri.value == "" ||
      currentExternalUri.value.length == 0
  ) {
    return false;
  }

  let includedInRelations = false;

  for (const skosRelation of SUPPORTED_SKOS_RELATIONS) {
    if (
        skosReferences.value[skosRelation.dtoKey]
            .includes(currentExternalUri.value)
    ) {
      includedInRelations = true;
      break;
    }
  }

  return includedInRelations;
}

function removeRelationsToSkosReferences(row: any) {
  for (const skosRelation of SUPPORTED_SKOS_RELATIONS) {
    skosReferences.value[skosRelation.dtoKey] =
        skosReferences.value[skosRelation.dtoKey].filter(
            function(value, index, arr) {
              return value != row.relationURI;
            }
        );
  }

  return new Promise((resolve, reject) => {
    emit("onDelete", skosReferences.value, result => {
      if (result instanceof Promise) {
        result.then(resolve).catch(reject);
      } else {
        resolve(result);
      }
    });
  });
}

async function update() {
  return new Promise((resolve, reject) => {
    emit("onUpdate", skosReferences.value, result => {
      if (result instanceof Promise) {
        result.then(resolve).catch(reject);
      } else {
        resolve(result);
      }
    });
  });
}

function onSearchTextChange(searchedText: string) {
  text.value = searchedText;
}

function onImportMapping(
    entity: AgroportalTermDTO,
    relation: any
) {
  currentExternalUri.value = entity.id;
  currentRelation.value = relation.id;
}
</script>

<style scoped lang="scss">
a {
  color: #007bff;
}

.helperAndBlueStar {
  display: flex;
}

.blueStar {
  color: #007bff;
}
</style>