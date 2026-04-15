<template>
  <n-form v-if="form.name_translations">
    <opensilex-InputForm
        v-model:value="form.uri"
        label="component.common.uri"
        type="text"
        rules="url"
        :disabled="editMode"
        :required="true"
    ></opensilex-InputForm>

    <opensilex-FormSelector
        v-model:selected="form.parent"
        :options="parentOptions"
        checkStrategy="parent"
        :required="true"
        label="component.common.parent"
        :filterable="true"
    ></opensilex-FormSelector>

    <opensilex-InputForm
        v-model:value="form.name_translations.en"
        :label="t('OntologyClassForm.labelEN')"
        type="text"
        :required="true"
    ></opensilex-InputForm>

    <opensilex-TextAreaForm
        v-model:value="form.comment_translations.en"
        :label="t('OntologyClassForm.commentEN')"
        :required="true"
        @keydown.native.enter.stop
    ></opensilex-TextAreaForm>

    <opensilex-InputForm
        v-model:value="form.name_translations.fr"
        :label="t('OntologyClassForm.labelFR')"
        type="text"
        :required="true"
    ></opensilex-InputForm>

    <opensilex-TextAreaForm
        v-model:value="form.comment_translations.fr"
        :label="t('OntologyClassForm.commentFR')"
        :required="true"
        @keydown.native.enter.stop
    ></opensilex-TextAreaForm>

    <!-- is abstract -->
    <!-- <opensilex-CheckboxForm
      :value.sync="form.is_abstract"
      title="OntologyClassForm.abstract-type"
    ></opensilex-CheckboxForm> -->

    <opensilex-IconForm
        v-model:value="form.icon"
        :label="t('OntologyClassForm.icon')"
    ></opensilex-IconForm>
  </n-form>
</template>

<script setup lang="ts">
import {computed, inject, onMounted, Ref, ref, watchEffect} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useI18n} from "vue-i18n";
import {VueJsOntologyExtensionService} from "@/lib";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";
import {NForm, NTreeSelect} from "naive-ui";
import {OntologyService} from "opensilex-core/api/ontology.service";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const ontologyService = opensilex.getService<OntologyService>("opensilex-core.OntologyService");
const service = opensilex.getService<VueJsOntologyExtensionService>("opensilex.VueJsOntologyExtensionService");
const {t} = useI18n();

const availableParents = ref<Array<any>>([]);

const parentOptions = computed(() => {
  if (props.editMode) {
    return opensilex.buildTreeListOptions(availableParents.value, {
      disableSubTree: form.value.uri
    });
  } else {
    return opensilex.buildTreeListOptions(availableParents.value);
  }
})

const props = defineProps<{
  editMode: boolean,
  data: {
    parentUri: string
  }
}>();

const form = defineModel("form", {
  default: {
    uri: null,
    parent: null,
    name: null,
    name_translations: {en: null, fr: null},
    comment: null,
    comment_translations: {en: null, fr: null},
    icon: null,
    is_abstract: false
  }
});

defineExpose({
  getEmptyForm,
  create,
  update
})

watchEffect(() => {
  if (props.data.parentUri) {
    ontologyService.searchSubClassesOf(props.data.parentUri, undefined, false).then(http => {
      if (http.response.result.length > 0) {
        availableParents.value = http.response.result;
      }
    })
  }
})

function getEmptyForm() {
  return {
    uri: null,
    parent: null,
    name: null,
    name_translations: {en: null, fr: null},
    comment: null,
    comment_translations: {en: null, fr: null},
    icon: null,
    is_abstract: false
  };
}

function create(form) {
  return service
      .createRDFType(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        let message = t("OntologyClassView.the-type") + " " + uri + t("component.common.success.creation-success-message");
        opensilex.showSuccessToast(message);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Object type already exists", error);
          opensilex.errorHandler(
              error,
              t("OntologyClassForm.object-type-already-exists")
          );
        } else {
          opensilex.errorHandler(error);
        }
      });
}

function update(form) {
  return service
      .updateRDFType(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        let message = t("OntologyClassView.the-type") + " " + uri + t("component.common.success.update-success-message");
        opensilex.showSuccessToast(message);
      })
      .catch(opensilex.errorHandler);
}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
  OntologyClassForm:
    abstract-type: Abstract type
    labelEN: English name
    labelFR: French name
    commentEN: English description
    commentFR: French description
    object-type-already-exists: Object type with same URI already exists
    icon: Icon

fr:
  OntologyClassForm:
    abstract-type: Type abstrait
    labelEN: Nom anglais
    labelFR: Nom français
    commentEN: Description anglaise
    commentFR: Description française
    object-type-already-exists: Un type d'objet existe déjà avec la même URI
    icon: Icône
</i18n>