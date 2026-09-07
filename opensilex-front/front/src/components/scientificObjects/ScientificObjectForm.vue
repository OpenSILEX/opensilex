<template>
  <OntologyObjectForm
    ref="modalForm"
    create-title="component.scientificObjects.actions.add"
    edit-title="component.scientificObjects.actions.update"
    :current-type="currentType || $opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"
    :base-type="$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"
    :create-action="callScientificObjectCreation"
    :update-action="callScientificObjectUpdate"
  ></OntologyObjectForm>
</template>

<script setup lang="ts">
import {inject, Ref, ref, useTemplateRef} from "vue";
import Oeso from "../../ontologies/Oeso";
import Rdfs from "../../ontologies/Rdfs";
import DTOConverter from "../../models/DTOConverter";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {MultiValuedRDFObjectRelation} from "@/components/ontology/models/MultiValuedRDFObjectRelation";
import OntologyObjectForm, {OntologyObjectFormModel} from "@/components/ontology/OntologyObjectForm.vue";
import {
  ScientificObjectCreationDTO,
  ScientificObjectDetailDTO,
  ScientificObjectsService,
  ScientificObjectUpdateDTO
} from "../../../../../opensilex-core/front/src/lib";
import {UserGetDTO} from "@/lib";

//#region type helpers
//To not have to put the extremely annoying 'InstanceType...' every time
type OntologyObjectFormInstance = InstanceType<typeof OntologyObjectForm>;
//#endregion

//#region Constant values
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const soService: ScientificObjectsService = $opensilex.getService<ScientificObjectsService>('opensilex.ScientificObjectsService');
//endregion

//#region Props
interface Props {
  context?: string
}

const props = defineProps<Props>();

//endregion


//#region reactive data
//Data to track what type of OS is being created or updated
const currentType = ref<string>(null);
//#endregion

//#region Template refs
const modalForm = useTemplateRef<OntologyObjectFormInstance>('modalForm')
//#endregion

//#region Public methods & Expose
function createScientificObject(parentURI?) {

  let ontologyObjectForm: OntologyObjectFormInstance = modalForm.value;
  initOntologyObjectForm(ontologyObjectForm);

  // if parentURI property is set, then use this value as default isPartOf relation value
  ontologyObjectForm.setInitHandler((relation: Ref<MultiValuedRDFObjectRelation>) => {
    if (parentURI) {
      if ($opensilex.Oeso.checkURIs(relation.value.property.uri, $opensilex.Oeso.IS_PART_OF)) {
        relation.value.value = parentURI;
        ontologyObjectForm.updateRelations();
      }
    }
  });
  modalForm.value.showCreateForm();
}

function editScientificObject(objectURI: string) {
  soService
    .getScientificObjectDetail(objectURI, getExperimentURI())
    .then((http) => {
      let ontologyObjectForm: OntologyObjectFormInstance = modalForm.value;
      let os: ScientificObjectDetailDTO = http.response.result;

      currentType.value = os.rdf_type;
      initOntologyObjectForm(ontologyObjectForm);
      excludeCurrentURIFromParentSelector(objectURI, ontologyObjectForm);
      let publisher: UserGetDTO = os.publisher;
      const editDto = DTOConverter.extractURIFromResourceProperties<ScientificObjectDetailDTO, OntologyObjectFormModel>(os);
      editDto.publisher = publisher;

      modalForm.value.showEditForm(editDto);
    });
}

defineExpose({createScientificObject, editScientificObject})
//endregion

//#region Private methods
/**
 * Inner function used to pass some properties to the OntologyObjectForm
 */
function initOntologyObjectForm(ontologyObjectForm: OntologyObjectFormInstance) {

  let excludedProperties = new Set<string>([
    Oeso.getShortURI(Oeso.HAS_GEOMETRY), // location with move
    Rdfs.getShortURI(Rdfs.LABEL) // let OntologyObjectForm handle rdfs:label by default
  ]);

  ontologyObjectForm.setExcludedProperties(excludedProperties);

  if (!props.context) {
    ontologyObjectForm.setLoadCustomProperties(false);
  }
}

function excludeCurrentURIFromParentSelector(objectURI: string, form: OntologyObjectFormInstance) {
  let customComponentProps = new Map<string, Map<string, any>>();

  let isPartOf = Oeso.getShortURI(Oeso.IS_PART_OF);
  customComponentProps.set(isPartOf, new Map<string, any>());
  customComponentProps.get(isPartOf).set("excluded", new Set<string>([objectURI]));

  form.setCustomComponentProps(customComponentProps);
}

function getExperimentURI() {
  return props.context;
}

function callScientificObjectCreation(form: ScientificObjectCreationDTO) {
  let definedRelations = [];
  for (let i in form.relations) {
    let relation = form.relations[i];
    if (relation.value != null) {
      if (Array.isArray(relation.value)) {
        for (let j in relation.value) {
          definedRelations.push({
            property: relation.property,
            value: relation.value[j],
          });
        }
      } else {
        definedRelations.push(relation);
      }
    }
  }

  return soService
    .createScientificObject({
      uri: form.uri,
      name: form.name,
      rdf_type: form.rdf_type,
      experiment: getExperimentURI(),
      relations: definedRelations,
    })
    .catch((error) => {
      $opensilex.errorHandler(error, error.response.result.message);
      throw error;
    });
}

function callScientificObjectUpdate(form: ScientificObjectUpdateDTO) {
  let definedRelations = [];
  for (let i in form.relations) {
    let relation = form.relations[i];
    if (relation.value != null) {
      if (Array.isArray(relation.value)) {
        for (let j in relation.value) {
          definedRelations.push({
            property: relation.property,
            value: relation.value[j],
          });
        }
      } else {
        definedRelations.push(relation);
      }
    }
  }

  return soService
    .updateScientificObject({
      uri: form.uri,
      name: form.name,
      rdf_type: form.rdf_type,
      publisher: form.publisher,
      publication_date: form.publication_date,
      experiment: getExperimentURI(),
      relations: definedRelations
    })
    .catch((error) => {
      $opensilex.errorHandler(error, error.response.result.message);
      throw error;
    });
}
//endregion

</script>
<style scoped lang="scss">
</style>
