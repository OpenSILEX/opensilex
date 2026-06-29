<template>
  <ModalForm
    ref="modalForm"
    component="opensilex-OntologyObjectForm"
    createTitle="ExperimentScientificObjects.add"
    editTitle="ExperimentScientificObjects.update"
    modalSize="lg"
    icon="ik#ik-target"
    :createAction="callScientificObjectCreation"
    :updateAction="callScientificObjectUpdate"
    @onCreate="$emit('onCreate', $event)"
    @onUpdate="$emit('onUpdate', $event)"
  >
  </ModalForm>
</template>

<script setup lang="ts">
import {inject, Ref, ref} from "vue";
import {ScientificObjectsService} from "opensilex-core/index";
import ModalForm from "../common/forms/ModalForm.vue";
import Oeso from "../../ontologies/Oeso";
import Rdfs from "../../ontologies/Rdfs";
import {ScientificObjectDetailDTO} from "opensilex-core/model/scientificObjectDetailDTO";
import {ScientificObjectCreationDTO} from "opensilex-core/model/scientificObjectCreationDTO";
import {ScientificObjectUpdateDTO} from "opensilex-core/model/scientificObjectUpdateDTO";
import DTOConverter from "../../models/DTOConverter";
import {UserGetDTO} from "../../../../../opensilex-security/front/src/lib";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {MultiValuedRDFObjectRelation} from "@/components/ontology/models/MultiValuedRDFObjectRelation";
import OntologyObjectForm from "@/components/ontology/OntologyObjectForm.vue";

//#region type helpers
//To not have to put the extremely annoying 'InstanceType...' every time
type OntologyObjectFormInstance = InstanceType<typeof OntologyObjectForm>;
//#endregion

//#region Constant values
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const soService: ScientificObjectsService = $opensilex.getService<ScientificObjectsService>('opensilex.ScientificObjectsService');
//endregion

//#region Props
//TODO MAX find out if pointless prop (Not defined in ScientificObjectsView
interface Props {
  context?: any
}

const props = withDefaults(
  defineProps<Props>(),
  {context: {}}
);

//endregion

//#region Template Refs
const modalForm = ref<InstanceType<typeof ModalForm>>(null);
//endregion

//#region Public methods & Expose
function createScientificObject(parentURI?) {
  let form: OntologyObjectFormInstance = modalForm.value.getFormRef();
  initOntologyObjectForm(form, undefined);

  // if parentURI property is set, then use this value as default isPartOf relation value
  form.setInitHandler((relation: Ref<MultiValuedRDFObjectRelation>) => {
    if (parentURI) {
      if ($opensilex.Oeso.checkURIs(relation.value.property.uri, $opensilex.Oeso.IS_PART_OF)) {
        relation.value.value = parentURI;
        form.updateRelations();
      }
    }
  });

  modalForm.value.showCreateForm();
}

function editScientificObject(objectURI: string) {
  soService
    .getScientificObjectDetail(objectURI, getExperimentURI())
    .then((http) => {
      let form: OntologyObjectFormInstance = modalForm.value.getFormRef();
      let os: ScientificObjectDetailDTO = http.response.result;

      initOntologyObjectForm(form, os.rdf_type);
      excludeCurrentURIFromParentSelector(objectURI, form);
      let publisher: UserGetDTO = os.publisher;
      const editDto = DTOConverter.extractURIFromResourceProperties<ScientificObjectDetailDTO, ScientificObjectUpdateDTO>(os);
      editDto.publisher = publisher;

      modalForm.value.showEditForm(editDto);
    });
}

defineExpose({createScientificObject, editScientificObject})
//endregion

//#region Private methods
/**
 * Inner function used to pass some properties to the OntologyObjectForm
 * @param form
 * @param type
 */
function initOntologyObjectForm(form: OntologyObjectFormInstance, type: string) {

  let excludedProperties = new Set<string>([
    Oeso.getShortURI(Oeso.HAS_GEOMETRY), // location with move
    Rdfs.getShortURI(Rdfs.LABEL) // let OntologyObjectForm handle rdfs:label by default
  ]);

  let xp: string = getExperimentURI();
  form.setContext(xp)
  if (!type) {
    form.setBaseType($opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI, $opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI);
  } else {
    form.setBaseType(type, $opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI)
  }
  form.setExcludedProperties(excludedProperties);

  if (!xp) {
    form.setLoadCustomProperties(false);
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
  if (props.context && props.context.experimentURI) {
    return props.context.experimentURI;
  }
  return undefined;
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
