<template>
  <n-form v-if="form.name_translations">
    <InputForm
        v-model:value="form.uri"
        label="component.common.uri"
        type="text"
        rules="url"
        :disabled="editMode"
        :required="true"
    ></InputForm>

    <hr/>

    <div class="row">

      <div class="col-lg-6">
        <label for="datatypeRadio">

        </label>
        <n-radio-group v-model:value="form.rdf_type" id="datatypeRadio">
          <div>
            <n-radio
                :value="OWL.DATATYPE_PROPERTY_URI"
                :label="t('OntologyPropertyForm.dataProperty')"
            >
            </n-radio>
          </div>
          <div>
            <n-radio
                :value="OWL.OBJECT_PROPERTY_URI"
                :label="t('OntologyPropertyForm.objectProperty')"
            >
            </n-radio>
          </div>
          <div>
            <n-radio
                :value="null"
                :label="t('OntologyPropertyForm.inheritedType')"
            >
            </n-radio>
          </div>
        </n-radio-group>
      </div>
      <div class="col-lg-6">
        <FormSelector
            v-if="form.rdf_type == OWL.DATATYPE_PROPERTY_URI"
            :label="t('OntologyPropertyForm.data-type')"
            :required="true"
            v-model:selected="form.range"
            :options="dataTypes"
            :filterable="true"
            :helpMessage="t('OntologyPropertyForm.dataProperty-help')"
        ></FormSelector>

        <FormSelector
            v-if="form.rdf_type == OWL.OBJECT_PROPERTY_URI"
            :label="t('OntologyPropertyForm.object-type')"
            :required="true"
            v-model:selected="form.range"
            :options="objectTypes"
            :filterable="true"
            :helpMessage="t('OntologyPropertyForm.objectProperty-help')"
        ></FormSelector>

        <FormSelector
            v-if="form.rdf_type == null"
            :label="t('component.common.parent')"
            :required="true"
            v-model:selected="form.parent"
            :options="parentOptions"
            :filterable="true"
            :helpMessage="t('OntologyPropertyForm.parent-help')"
        ></FormSelector>

        <TypeForm
            v-model:type="form.domain"
            :baseType="data.domain"
            :ignoreRoot="false"
            :label="t('OntologyPropertyForm.domain')"
            :helpMessage="t('OntologyPropertyForm.domain-help')"
        ></TypeForm>
      </div>

    </div>


    <hr/>
    <InputForm
        v-model:value="form.name_translations.en"
        :label="t('OntologyPropertyForm.labelEN')"
        type="text"
        :required="enLangRequired"
    ></InputForm>

    <TextAreaForm
        v-model:value="form.comment_translations.en"
        :label="t('OntologyPropertyForm.commentEN')"
        :required="false"
        @keydown.native.enter.stop
    ></TextAreaForm>

    <InputForm
        v-model:value="form.name_translations.fr"
        :label="t('OntologyPropertyForm.labelFR')"
        type="text"
        :required="otherLangRequired"
    ></InputForm>

    <TextAreaForm
        v-model:value="form.comment_translations.fr"
        :label="t('OntologyPropertyForm.commentFR')"
        :required="false"
        @keydown.native.enter.stop
    ></TextAreaForm>

  </n-form>
</template>

<script setup lang="ts">
import {computed, inject, ref, watchEffect} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {useI18n} from "vue-i18n";
import {useStore} from "vuex";
import OWL from "@/ontologies/OWL";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";
import {NForm, NRadio, NRadioGroup} from "naive-ui";
import InputForm from "@/components/common/forms/InputForm.vue";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import TypeForm from "@/components/common/forms/TypeForm.vue";
import TextAreaForm from "@/components/common/forms/TextAreaForm.vue";

//#region Public

const props = defineProps<{
  editMode: boolean
  data: {
    domain: string
  }
}>();

const form = defineModel("form", {
  default: {
    uri: null,
    rdf_type: OWL.DATATYPE_PROPERTY_URI,
    parent: null,
    name_translations: {en: null, fr: null},
    comment_translations: {en: "", fr: ""},
    domain: null,
    range: null
  }
});

defineExpose({
  getEmptyForm,
  create,
  update
})
//#endregion

//#region Private
const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const store = useStore();
const ontologyService = opensilex.getService<OntologyService>("opensilex-core.OntologyService");
const {t} = useI18n();
const lang = computed(() => store.state.lang);
const enLangRequired = computed(() => lang.value === "en");
const otherLangRequired = computed(() => lang.value !== "en");

const availableParents = ref([]);
const rdfTypeByParentURI = ref({});

const parentOptions = computed(() => {
  if (props.editMode) {
    return opensilex.buildTreeListOptions(availableParents.value, {
      disableSubTree: form.value.uri
    });
  } else {
    return opensilex.buildTreeListOptions(availableParents.value);
  }
})

const dataTypes = computed(() => {
  let types: Array<{ id: string, label: string }> = [];

  opensilex.datatypes.forEach(type => {
    let label: any = t(type.label_key);
    types.push({
      id: type.uri,
      label: label.charAt(0).toUpperCase() + label.slice(1)
    });
  });

  sortTypesByLabel(types);
  return types;
});

const objectTypes = computed(() => {
  let types: Array<{ id: string, label: string }> = [];
  opensilex.objectTypes
      .filter(type => type.name)
      .forEach(type => {
        // try to get translated name
        let translatedLabel: string = type.rdf_type.name_translations[store.getters.language];

        // if no translation found, then use default name
        if (!translatedLabel || translatedLabel.length == 0) {
          translatedLabel = type.rdf_type.name;
        }
        types.push({
          id: type.uri,
          label: translatedLabel
        });
      });

  sortTypesByLabel(types);
  return types;
})


watchEffect(() => {
  if (props.data.domain) {
    ontologyService.getProperties(props.data.domain, undefined, true).then(http => {
      if (http.response.result.length > 0) {
        const dtoList = http.response.result;
        availableParents.value = dtoList;
        dtoList.forEach((dto) => {
          rdfTypeByParentURI.value[dto.uri] = dto.rdf_type;
        })
      }
    })
  }
});


function getEmptyForm() {
  return {
    uri: null,
    rdf_type: OWL.DATATYPE_PROPERTY_URI,
    parent: null,
    name_translations: {},
    comment_translations: {en: "", fr: ""},
    domain: null,
    range: null
  };
}

function sortTypesByLabel(types: Array<{ id: string, label: string }>): void {
  types.sort((a, b) => {
    if (a.label > b.label) {
      return 1;
    } else if (a.label < b.label) {
      return -1;
    }
    return 0;
  });
}

function computeFormToSend(form) {
  let sentForm = {...form};

  if (!sentForm.rdf_type) {
    sentForm.rdf_type = rdfTypeByParentURI.value[form.parent];
  } else {
    sentForm.parent = null;
  }

  return sentForm;
}

function create(form) {
  return ontologyService.createProperty(computeFormToSend(form))
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        let message = t("OntologyPropertyView.the-property") + " " + uri + t("component.common.success.creation-success-message");
        opensilex.showSuccessToast(message);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Property already exists", error);
          opensilex.errorHandler(
              error,
              t("OntologyPropertyForm.property-already-exists")
          );
        } else {
          opensilex.errorHandler(error);
        }
      });
}

function update(form) {
  return ontologyService.updateProperty(computeFormToSend(form))
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        let message = t("OntologyPropertyView.the-property") + " " + uri + t("component.common.success.update-success-message");
        opensilex.showSuccessToast(message);
      })
      .catch(opensilex.errorHandler);
}

//#endregion
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
  OntologyPropertyForm:
    propertyType: Property Type
    dataProperty: Data property
    objectProperty: Object property
    inheritedType: Type inherited from parent
    data-type: Data type
    dataProperty-help: 'Property which relate resource (e.g. device,scientific object, facility) to literal data (integer,decimal,date,string,etc)'
    object-type: Object class
    objectProperty-help: 'Property which relate resource (e.g. device,scientific object, facility) to other resource (e.g. device,scientific object, facility)'
    parent-help: 'Parent'
    labelEN: English name
    labelFR: French name
    commentEN: English description
    commentFR: French description
    property-already-exists: 'Property with same URI already exists'
    domain: Domain
    domain-help: 'Type concerned by the property. The property can be linked to the domain and on all domain descendant'

fr:
  OntologyPropertyForm:
    propertyType: Type de propriété
    dataProperty: Propriété litérale
    objectProperty: Relation vers un objet
    inheritedType: Type hérité du parent
    data-type: Type de donnée
    dataProperty-help: 'Propriété associant une valeur (nombre,date,chaîne de caractères, etc) à une ressource(ex: équipement, object scientifique, évenement) '
    object-type: Classe d'objet
    objectProperty-help: 'Propriété liant une ressource(ex: équipement, object scientifique, évenement) à une autre ressource'
    parent-help: 'Parent'
    labelEN: Nom anglais
    labelFR: Nom français
    commentEN: Description anglaise
    commentFR: Description française
    property-already-exists: Une propriété existe déjà avec la même URI
    domain: Domaine
    domain-help: 'Type concerné par la propriété. La propriété peut être liée au domaine choisi et à tous les descendants du domaine'

</i18n>