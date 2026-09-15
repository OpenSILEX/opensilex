<template>
  <div>
    <p v-if="skosReferences">
      {{ t('component.skos.addTo') }}
      <em>
        <strong class="text-primary">{{ skosReferences.uri }}</strong>
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

    <Card noHeader :noFooter="true">
      <template #body>
        <div class="row">
          <div class="col">
            <h5 class="font-weight-bold mb-0">
              {{ t('component.skos.ontologies-references-label') }}
            </h5>

            <div>
              <ul>
                <li
                    v-for="externalOntologyRef in externalOntologiesRefs"
                    :key="externalOntologyRef.name"
                >
                  <a
                      target="_blank"
                      :title="`${externalOntologyRef.name}: ${externalOntologyRef.description}`"
                      :href="externalOntologyRef.link"
                  >
                    {{ externalOntologyRef.name }}
                  </a>
                </li>
              </ul>
            </div>
          </div>

          <div class="col">
            <n-form
                ref="nFormRef"
                :model="form"
                :rules="rules"
                label-placement="top"
                :show-require-mark="true"
            >
              <FormSelector
                  label="component.skos.relation"
                  helpMessage="component.skos.relation-help"
                  placeholder="component.skos.no-relation"
                  path="relation"
                  v-model:selected="currentRelation"
                  :options="options"
                  :required="true"
              />

              <!-- URI -->
              <n-form-item path="externalUri" :show-label="false">
                <FormField
                    label="component.skos.uri"
                    helpMessage="component.skos.uri-help"
                    :required="true"
                >
                  <template #field="{ id }">
                    <span
                        class="error-message alert alert-danger"
                        v-if="isIncludedInRelations()"
                    >
                      {{ t('component.skos.external-already-existing') }}
                    </span>

                    <n-input
                        :id="id"
                        v-model:value="form.externalUri"
                        type="text"
                        :placeholder="t('component.skos.uri-placeholder')"
                    />
                  </template>
                </FormField>
              </n-form-item>

              <div class="text-end">
                <Button
                    label="component.skos.add"
                    @click="addRelationsToSkosReferences"
                    class="greenThemeColor"
                />
              </div>
            </n-form>
          </div>
        </div>
      </template>
    </Card>

    <div
        v-if="displayInsertButton"
        class="text-end mt-2"
    >
      <Button
          label="component.skos.update"
          @click="update"
      />
    </div>

    <div>
      <n-data-table
          v-if="relations.length !== 0"
          :columns="columns"
          :data="relations"
          :pagination="false"
          size="small"
          bordered
      />

      <p v-else>
        <strong>
          {{ t('component.skos.no-external-links-provided') }}
        </strong>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, h, inject, nextTick, onBeforeUnmount, onMounted, reactive, ref, toRef, useTemplateRef} from "vue";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import {NDataTable, NForm, NFormItem, NInput} from "naive-ui";
import {required} from "@/models/FormFieldsFormatter";

import SUPPORTED_SKOS_RELATIONS from "../../../models/SkosRelations";
import {ExternalOntologies} from "../../../models/ExternalOntologies";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {AgroportalAPIService} from "opensilex-core/api/agroportalAPI.service";
import {AgroportalTermDTO} from "opensilex-core/model/agroportalTermDTO";
import HttpResponse from "../../../lib/HttpResponse";

import AgroportalSearch from "@/components/common/external-references/agroportal/AgroportalSearch.vue";
import AgroportalResults from "@/components/common/external-references/agroportal/AgroportalResults.vue";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import FormField from "@/components/common/forms/FormField.vue";
import Card from "@/components/common/views/Card.vue";
import Button from "@/components/common/buttons/Button.vue";
import DeleteButton from "@/components/common/buttons/DeleteButton.vue";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const store = useStore();
const {t} = useI18n();

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

const nFormRef = useTemplateRef<InstanceType<typeof NForm>>("nFormRef");

const form = reactive({
  relation: "",
  externalUri: ""
});

const currentRelation = toRef(form, "relation");
const currentExternalUri = toRef(form, "externalUri");

const EXTERNAL_URI_REGEX = /^(http:\/\/www\.|https:\/\/www\.|http:\/\/|https:\/\/)?[a-z0-9]+([\-\.]{1}[a-z0-9]+)*\.[a-z]{2,5}(:[0-9]{1,5})?(\/.*)?$/;

const rules = computed(() => ({
  relation: required(t('component.skos.relation')),
  externalUri: {
    validator: (_rule: any, value: string) => {
      if (!value || value.trim().length === 0) {
        return new Error(t("validations.required_if", {_field_: t("component.skos.uri")}));
      }
      if (!EXTERNAL_URI_REGEX.test(value.trim())) {
        return new Error(t("validations.url", {_field_: t("component.skos.uri")}));
      }
      return true;
    },
    trigger: ["blur", "change"]
  }
}));

const text = ref<string>("");
const ontologies = ref<string[]>([]);
const isAllOntologies = ref<boolean>(false);

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
    nFormRef.value?.restoreValidation();
  });
}

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
      relation: t(skosRelation.label),
      relationURI: externalUri
    });
  }
}

const columns = [
  {
    title: t('component.skos.relation'),
    key: 'relation'
  },
  {
    title: t('component.skos.uri'),
    key: 'relationURI',
    render: (row: any) => h('a', {href: row.relationURI, target: '_blank'}, row.relationURI)
  },
  {
    title: t('component.common.actions'),
    key: 'actions',
    align: 'center' as const,
    render: (row: any) =>
        h(DeleteButton, {
          label: 'component.common.delete',
          small: true,
          onClick: () => removeRelationsToSkosReferences(row)
        })
  }
];

function validateForm() {
  return nFormRef.value.validate()
      .then(() => true)
      .catch(() => false);
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
            function (value, index, arr) {
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
</style>
