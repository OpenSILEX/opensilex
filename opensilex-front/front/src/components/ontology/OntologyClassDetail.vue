<template>
  <b-card v-if="selected">
    <template v-slot:header>
      <h3>{{$t("OntologyClassDetail.title")}}</h3>
    </template>
    <div>
      <!-- URI -->
      <opensilex-UriView :uri="selected.uri"></opensilex-UriView>
      <!-- Name -->
      <opensilex-StringView label="component.common.name" :value="selected.label"></opensilex-StringView>
      <!-- Description -->
      <opensilex-TextView label="component.common.comment" :value="selected.comment"></opensilex-TextView>
      <!-- Abstract type -->
      <!-- <opensilex-BooleanView label="OntologyClassForm.abstract-type" :value="selected.isAbstract"></opensilex-BooleanView> -->
      <!-- Icon identifier -->
      <opensilex-IconView label="OntologyClassForm.icon" :value="selected.icon"></opensilex-IconView>

      <div class="static-field">
        <span class="field-view-title">{{$t("OntologyClassDetail.dataProperties")}}:</span>
        <opensilex-Button
          @click="addDataProperty"
          class="align-right"
          variant="primary"
          icon="ik#ik-plus"
          :small="false"
          label="OntologyClassDetail.addDataProperty"
        ></opensilex-Button>
      </div>

      <b-table striped hover small responsive :items="selected.dataProperties" :fields="fields">
        <template v-slot:head(name)="data">{{$t(data.label)}}</template>
        <template v-slot:head(property)="data">{{$t(data.label)}}</template>
        <template v-slot:head(isList)="data">{{$t(data.label)}}</template>
        <template v-slot:head(isRequired)="data">{{$t(data.label)}}</template>
        <template v-slot:head(inherited)="data">{{$t(data.label)}}</template>
        <template v-slot:head(actions)="data">{{$t(data.label)}}</template>

        <template v-slot:cell(isList)="data">
          <span
            class="capitalize-first-letter"
          >{{data.item.isList ? $t("component.common.yes") : $t("component.common.no")}}</span>
        </template>
        <template v-slot:cell(isRequired)="data">
          <span
            class="capitalize-first-letter"
          >{{data.item.isRequired ? $t("component.common.yes") : $t("component.common.no")}}</span>
        </template>
        <template v-slot:cell(inherited)="data">
          <span
            class="capitalize-first-letter"
          >{{data.item.inherited ? $t("component.common.yes") : $t("component.common.no")}}</span>
        </template>

        <template v-slot:cell(actions)="data">
          <b-button-group size="sm">
            <opensilex-EditButton
              v-if="!data.item.inherited"
              @click="editDataProperty(data.item)"
              label="OntologyClassDetail.updateProperty"
              :small="true"
            ></opensilex-EditButton>
            <opensilex-DeleteButton
              v-if="!data.item.inherited"
              @click="deleteClassPropertyRestriction(data.item.property)"
              label="OntologyClassDetail.deleteProperty"
              :small="true"
            ></opensilex-DeleteButton>
          </b-button-group>
        </template>
      </b-table>

      <div class="static-field">
        <span class="field-view-title">{{$t("OntologyClassDetail.objectProperties")}}:</span>
        <opensilex-Button
          @click="addObjectProperty"
          class="align-right"
          variant="primary"
          icon="ik#ik-plus"
          :small="false"
          label="OntologyClassDetail.addObjectProperty"
        ></opensilex-Button>
      </div>

      <b-table striped hover small responsive :items="selected.objectProperties" :fields="fields">
        <template v-slot:head(name)="data">{{$t(data.label)}}</template>
        <template v-slot:head(property)="data">{{$t(data.label)}}</template>
        <template v-slot:head(isList)="data">{{$t(data.label)}}</template>
        <template v-slot:head(isRequired)="data">{{$t(data.label)}}</template>
        <template v-slot:head(inherited)="data">{{$t(data.label)}}</template>
        <template v-slot:head(actions)="data">{{$t(data.label)}}</template>

        <template v-slot:cell(isList)="data">
          <span
            class="capitalize-first-letter"
          >{{data.item.isList ? $t("component.common.yes") : $t("component.common.no")}}</span>
        </template>
        <template v-slot:cell(isRequired)="data">
          <span
            class="capitalize-first-letter"
          >{{data.item.isRequired ? $t("component.common.yes") : $t("component.common.no")}}</span>
        </template>
        <template v-slot:cell(inherited)="data">
          <span
            class="capitalize-first-letter"
          >{{data.item.inherited ? $t("component.common.yes") : $t("component.common.no")}}</span>
        </template>
        <template v-slot:cell(actions)="data">
          <b-button-group size="sm">
            <opensilex-EditButton
              v-if="!data.item.inherited"
              @click="editObjectProperty(data.item)"
              label="OntologyClassDetail.updateProperty"
              :small="true"
            ></opensilex-EditButton>
            <opensilex-DeleteButton
              v-if="!data.item.inherited"
              @click="deleteClassPropertyRestriction(data.item.property)"
              label="OntologyClassDetail.deleteProperty"
              :small="true"
            ></opensilex-DeleteButton>
          </b-button-group>
        </template>
      </b-table>
      <opensilex-ModalForm
        ref="classPropertyForm"
        component="opensilex-OntologyClassPropertyForm"
        createTitle="OntologyClassDetail.addProperty"
        editTitle="OntologyClassDetail.updateProperty"
        @onCreate="$emit('onDetailChange')"
        @onUpdate="$emit('onDetailChange')"
      ></opensilex-ModalForm>
    </div>
  </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import { OntologyService } from "opensilex-core/index";

@Component
export default class OntologyClassDetail extends Vue {
  $opensilex: any;

  @Prop()
  selected;

  @Prop()
  rdfClass;

  @Ref("classPropertyForm") readonly classPropertyForm!: any;

  fields = [
    {
      key: "name",
      label: "component.common.name"
    },
    {
      key: "property",
      label: "component.common.type"
    },
    {
      key: "isRequired",
      label: "OntologyClassDetail.required"
    },
    {
      key: "isList",
      label: "OntologyClassDetail.list"
    },
    {
      key: "inherited",
      label: "OntologyClassDetail.inherited"
    },
    {
      label: "component.common.actions",
      key: "actions"
    }
  ];

  ontologyService: OntologyService;

  created() {
    this.ontologyService = this.$opensilex.getService(
      "opensilex-core.OntologyService"
    );
  }

  addDataProperty() {
    this.ontologyService.getDataProperties(this.rdfClass).then(http => {
      let formRef = this.classPropertyForm.getFormRef();
      formRef.setClassURI(this.selected.uri);
      formRef.setProperties(http.response.result, this.selected.dataProperties);
      formRef.setIsObjectProperty(false);
      this.classPropertyForm.showCreateForm();
    });
  }

  editDataProperty(item) {
    this.ontologyService.getDataProperties(this.rdfClass).then(http => {
      let formRef = this.classPropertyForm.getFormRef();
      formRef.setClassURI(this.selected.uri);
      formRef.setProperties(http.response.result, this.selected.dataProperties);
      formRef.setIsObjectProperty(false);
      console.error(item);
      this.classPropertyForm.showEditForm(item);
    });
  }

  addObjectProperty() {
    this.ontologyService.getObjectProperties(this.rdfClass).then(http => {
      let formRef = this.classPropertyForm.getFormRef();
      formRef.setClassURI(this.selected.uri);
      formRef.setProperties(
        http.response.result,
        this.selected.objectProperties
      );
      formRef.setIsObjectProperty(true);
      this.classPropertyForm.showCreateForm();
    });
  }

  editObjectProperty(item) {
    this.ontologyService.getObjectProperties(this.rdfClass).then(http => {
      let formRef = this.classPropertyForm.getFormRef();
      formRef.setClassURI(this.selected.uri);
      formRef.setProperties(
        http.response.result,
        this.selected.objectProperties
      );
      formRef.setIsObjectProperty(true);
      this.classPropertyForm.showEditForm(item);
    });
  }


  deleteClassPropertyRestriction(propertyURI) {
    this.ontologyService.deleteClassPropertyRestriction(
      this.selected.uri,
      propertyURI
    );
    this.$emit("onDetailChange");
  }

  
}
</script>

<style scoped lang="scss">
.align-right {
  float: right;
}
</style>


<i18n>
en:
  OntologyClassDetail:
    title: Object type detail 
    required: Required
    list: List of values
    inherited: Inherited
    dataProperties: Data properties
    addDataProperty: Add data property
    objectProperties: Object properties
    addObjectProperty: Add object property
    addProperty: Add property
    updateProperty: Update property
    deleteProperty: Delete property

fr:
  OntologyClassDetail:
    title: Détail du type d'objet
    required: Obligatoire
    list: Liste de valeurs
    inherited: Héritée
    dataProperties: Propriétés litérales
    addDataProperty: Ajouter une propriété litérale
    objectProperties: Relations vers des objets
    addObjectProperty: Ajouter une relation vers un objet
    addProperty: Ajouter une propriété
    updateProperty: Mettre à jour la propriété
    deleteProperty: Supprimer la propriété
</i18n>

