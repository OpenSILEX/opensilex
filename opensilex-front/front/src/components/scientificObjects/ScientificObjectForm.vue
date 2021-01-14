<template>
  <opensilex-ModalForm
    ref="soForm"
    component="opensilex-OntologyObjectForm"
    createTitle="ExperimentScientificObjects.add"
    editTitle="ExperimentScientificObjects.update"
    modalSize="lg"
    :createAction="callScientificObjectCreation"
    :updateAction="callScientificObjectUpdate"
    @onCreate="refresh()"
    @onUpdate="refresh()"
  >
    <template v-slot:customFields="{ form }">
      <opensilex-SelectForm
        label="ExperimentScientificObjects.parent-label"
        :selected.sync="form.parent"
        :multiple="false"
        :required="false"
        :searchMethod="searchParents"
        :itemLoadingMethod="getParentsByURI"
      ></opensilex-SelectForm>

      <opensilex-GeometryForm
        :value.sync="form.geometry"
        label="component.common.geometry"
        helpMessage="component.common.geometry-help"
      ></opensilex-GeometryForm>
    </template>
  </opensilex-ModalForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";

import {
  ExperimentCreationDTO,
  SpeciesService,
  SpeciesDTO,
  ScientificObjectsService
} from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class ScientificObjectForm extends Vue {
  $opensilex: any;
  $store: any;

  soService: ScientificObjectsService;

  @Ref("soForm") readonly soForm!: any;

  created() {
    this.soService = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );
  }

  createScientificObject() {
    this.soForm
      .getFormRef()
      .setBaseType(this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI);

    this.soForm.showCreateForm();
  }

  editScientificObject(objectURI) {
    this.soForm
      .getFormRef()
      .setBaseType(this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI);

    this.soService.getScientificObjectDetail(objectURI).then(http => {
      let form: any = http.response.result;
      this.soForm.showEditForm(form);
    });
  }

  callScientificObjectCreation(form) {
    let definedRelations = [];
    for (let i in form.relations) {
      let relation = form.relations[i];
      if (relation.property == "vocabulary:isPartOf") {
        relation.value = form.parent;
      }
      if (relation.value != null) {
        if (Array.isArray(relation.value)) {
          for (let j in relation.value) {
            definedRelations.push({
              property: relation.property,
              value: relation.value[j]
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
        type: form.type,
        geometry: form.geometry,
        relations: definedRelations
      })
      .then(http => {
        // this.refresh();
      });
  }

  callScientificObjectUpdate(form) {
    let definedRelations = [];
    let parentSet = false;
    for (let i in form.relations) {
      let relation = form.relations[i];
      if (relation.property == "vocabulary:isPartOf") {
        relation.value = form.parent;
        parentSet = true;
      }
      if (relation.value != null) {
        if (Array.isArray(relation.value)) {
          for (let j in relation.value) {
            definedRelations.push({
              property: relation.property,
              value: relation.value[j]
            });
          }
        } else {
          definedRelations.push(relation);
        }
      }
    }

    if (!parentSet && form.parent != null) {
      definedRelations.push({
        property: "vocabulary:isPartOf",
        value: form.parent
      });
    }

    return this.soService
      .updateScientificObject({
        uri: form.uri,
        name: form.name,
        type: form.type,
        geometry: form.geometry,
        relations: definedRelations
      })
      .then(http => {
        // this.refresh();
      });
  }
}
</script>
<style scoped lang="scss">
</style>
