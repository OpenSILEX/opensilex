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
    @onCreate="triggerRefresh"
    @onUpdate="triggerRefresh"
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

  triggerRefresh() {
    this.$emit("refresh");
  }

  createScientificObject() {
    this.soForm
      .getFormRef()
      .setContext(this.getContext())
      .setBaseType(this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI);

    this.soForm.showCreateForm();
  }

  editScientificObject(objectURI) {
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

    return this.soService.createScientificObject({
      uri: form.uri,
      name: form.name,
      rdf_type: form.rdf_type,
      geometry: form.geometry,
      experiment: this.getExperimentURI(),
      relations: definedRelations,
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

    return this.soService.updateScientificObject({
      uri: form.uri,
      name: form.name,
      rdf_type: form.rdf_type,
      geometry: form.geometry,
      experiment: this.getExperimentURI(),
      relations: definedRelations,
    });
  }
}
</script>
<style scoped lang="scss">
</style>
