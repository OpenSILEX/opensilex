<template>
  <opensilex-ModalForm
    ref="soForm"
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
    <template v-slot:customFields="{ form }">
      <opensilex-GeometryForm
        :value.sync="form.geometry"
        label="component.common.geometry"
        helpMessage="component.common.geometry-help"
      ></opensilex-GeometryForm>
    </template>
  </opensilex-ModalForm>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { ScientificObjectsService } from "opensilex-core/index";

@Component
export default class ScientificObjectForm extends Vue {
  $opensilex: any;
  $store: any;

  soService: ScientificObjectsService;

  @Prop({
    default: () => {},
  })
  context;

  @Ref("soForm") readonly soForm!: any;

  created() {
    this.soService = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );
  }

  createScientificObject(parentURI?) {
    this.soForm
      .getFormRef()
      .setTypePropertyFilterHandler((properties) => properties);
    
    this.soForm
      .getFormRef()
      .setContext(this.getContext())
      .setBaseType(this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI);

    this.soForm.getFormRef().setInitObjHandler((form) => {
      if (parentURI) {
        for (let relation of form.relations) {
          if (
            this.$opensilex.Oeso.checkURIs(
              relation.property,
              this.$opensilex.Oeso.IS_PART_OF
            )
          ) {
            relation.value = parentURI;
          }
        }
      }

    });

    this.soForm.showCreateForm();
  }

  editScientificObject(objectURI) {
    this.soForm.getFormRef().setTypePropertyFilterHandler((properties) => {
      return properties.filter((propertyDef) => {
        return !this.$opensilex.Oeso.checkURIs(
          propertyDef.definition.property,
          this.$opensilex.Oeso.HAS_FACILITY
        );
      });
    });

    this.soForm
      .getFormRef()
      .setContext(this.getContext())
      .setBaseType(this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI);

    this.soService
      .getScientificObjectDetail(objectURI, this.getExperimentURI())
      .then((http) => {
        let form: any = http.response.result;
        this.soForm.showEditForm(form);
      });
  }

  getContext() {
    if (this.context) {
      return this.context;
    }
    return {};
  }

  getExperimentURI() {
    if (this.context && this.context.experimentURI) {
      return this.context.experimentURI;
    }
    return undefined;
  }

  callScientificObjectCreation(form) {
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

    return this.soService
      .createScientificObject({
        uri: form.uri,
        name: form.name,
        rdf_type: form.rdf_type,
        geometry: form.geometry,
        experiment: this.getExperimentURI(),
        relations: definedRelations,
      })
      .catch((error) => {
        this.$opensilex.errorHandler(error,error.response.result.message);
        throw error;
      });
  }

  callScientificObjectUpdate(form) {
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

    return this.soService
      .updateScientificObject({
        uri: form.uri,
        name: form.name,
        rdf_type: form.rdf_type,
        geometry: form.geometry,
        experiment: this.getExperimentURI(),
        relations: definedRelations,
      })
      .catch((error) => {
        this.$opensilex.errorHandler(error,error.response.result.message);
        throw error;
      });
  }
}
</script>
<style scoped lang="scss">
</style>
