<template>
  <b-card v-if="selected">
    <template v-slot:header>
      <h3>{{ $t("OntologyClassDetail.title") }}</h3>
    </template>
    <div>
      <!-- URI -->
      <opensilex-UriView :uri="selected.uri"></opensilex-UriView>
      <!-- Name -->
      <opensilex-StringView
        label="component.common.name"
        :value="selected.name"
      ></opensilex-StringView>
      <!-- Description -->
      <opensilex-TextView
        label="component.common.comment"
        :value="selected.comment"
      ></opensilex-TextView>
      <!-- Abstract type -->
      <!-- <opensilex-BooleanView label="OntologyClassForm.abstract-type" :value="selected.is_abstract"></opensilex-BooleanView> -->
      <!-- Icon identifier -->
      <opensilex-IconView
        label="OntologyClassForm.icon"
        :value="selected.icon"
      ></opensilex-IconView>

      <div class="static-field">
        <span class="field-view-title" style="float: none"
          >{{ $t("OntologyClassDetail.dataProperties") }}:</span
        >
      </div>
      <div>
        <opensilex-Button
          v-if="user.isAdmin()"
          @click="addDataProperty"
          variant="primary"
          icon="ik#ik-plus"
          :small="false"
          label="OntologyClassDetail.addDataProperty"
        ></opensilex-Button>
        &nbsp;
        <opensilex-Button
          v-if="user.isAdmin()"
          @click="startSetPropertiesOrder"
          variant="primary"
          icon="fa#pencil-alt"
          :small="false"
          label="OntologyClassDetail.setPropertiesOrder"
        ></opensilex-Button>
        <b-modal ref="setPropertiesOrderRef" size="md" :static="true">
          <template v-slot:modal-title>
            {{ $t("OntologyClassDetail.setPropertiesOrder") }}
          </template>

          <template v-slot:modal-footer>
            <button
              type="button"
              class="btn btn-secondary"
              v-on:click="setPropertiesOrderRef.hide()"
            >
              {{ $t("component.common.close") }}
            </button>
            <button
              type="button"
              class="btn btn-primary"
              v-on:click="setPropertiesOrder()"
            >
              {{ $t("component.common.validateSelection") }}
            </button>
          </template>
          <p>{{ $t("OntologyClassDetail.setPropertiesOrderInfo") }}:</p>
          <b-list-group>
            <draggable
              v-model="customPropertyOrder"
              @start="drag = true"
              @end="drag = false"
            >
              <b-list-group-item
                v-for="element in customPropertyOrder"
                :key="element.property"
                >{{ element.name }}</b-list-group-item
              >
            </draggable>
          </b-list-group>
        </b-modal>
      </div>
      <b-table
        striped
        hover
        small
        responsive
        :items="properties"
        :fields="fields"
      >
        <template v-slot:head(name)="data">{{ $t(data.label) }}</template>
        <template v-slot:head(property)="data">{{ $t(data.label) }}</template>
        <template v-slot:head(is_list)="data">{{ $t(data.label) }}</template>
        <template v-slot:head(is_required)="data">{{ $t(data.label) }}</template>
        <template v-slot:head(inherited)="data">{{ $t(data.label) }}</template>
        <template v-slot:head(actions)="data">{{ $t(data.label) }}</template>

        <template v-slot:cell(is_list)="data">
          <span class="capitalize-first-letter">{{
            data.item.is_list
              ? $t("component.common.yes")
              : $t("component.common.no")
          }}</span>
        </template>
        <template v-slot:cell(is_required)="data">
          <span class="capitalize-first-letter">{{
            data.item.is_required
              ? $t("component.common.yes")
              : $t("component.common.no")
          }}</span>
        </template>
        <template v-slot:cell(inherited)="data">
          <span class="capitalize-first-letter">{{
            data.item.inherited
              ? $t("component.common.yes")
              : $t("component.common.no")
          }}</span>
        </template>

        <template v-slot:cell(actions)="data">
          <b-button-group size="sm">
            <opensilex-DeleteButton
              v-if="!data.item.inherited && data.item.is_custom && user.isAdmin()"
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

  get user() {
    return this.$store.state.user;
  }

  @Prop()
  selected;

  @Prop()
  rdfType;

  @Ref("classPropertyForm") readonly classPropertyForm!: any;
  @Ref("setPropertiesOrderRef") readonly setPropertiesOrderRef!: any;

  fields = [
    {
      key: "name",
      label: "component.common.name",
    },
    {
      key: "property",
      label: "component.common.type",
    },
    {
      key: "is_required",
      label: "OntologyClassDetail.required",
    },
    {
      key: "is_list",
      label: "OntologyClassDetail.list",
    },
    {
      key: "inherited",
      label: "OntologyClassDetail.inherited",
    },
    {
      label: "component.common.actions",
      key: "actions",
    },
  ];

  ontologyService: OntologyService;

  customPropertyOrder = [];

  created() {
    this.ontologyService = this.$opensilex.getService(
      "opensilex-core.OntologyService"
    );
  }

  get properties() {
    let allProps = this.selected.data_properties.concat(
      this.selected.object_properties
    );
    let pOrder = this.selected.properties_order;
    allProps.sort((a, b) => {
      if (a.property == b.property) {
        return 0;
      }

      if (a.property == "rdfs:label") {
        return -1;
      }

      if (b.property == "rdfs:label") {
        return 1;
      }

      let aIndex = pOrder.indexOf(a.property);
      let bIndex = pOrder.indexOf(b.property);
      if (aIndex == -1) {
        if (bIndex == -1) {
          return a.property.localeCompare(b.property);
        } else {
          return -1;
        }
      } else {
        if (bIndex == -1) {
          return 1;
        } else {
          return aIndex - bIndex;
        }
      }
    });
    return allProps;
  }

  addDataProperty() {
    this.ontologyService.getProperties(this.rdfType).then((http) => {
      let formRef = this.classPropertyForm.getFormRef();
      formRef.setDomain(this.rdfType);
      formRef.setClassURI(this.selected.uri);
      formRef.setProperties(http.response.result, this.properties);
      this.classPropertyForm.showCreateForm();
    });
  }

  deleteClassPropertyRestriction(propertyURI) {
    this.ontologyService
      .deleteClassPropertyRestriction(this.selected.uri, propertyURI)
      .then(() => {
        this.$emit("onDetailChange");
      })
      .catch(this.$opensilex.errorHandler);
  }

  setPropertiesOrder() {
    let propertiesOrder = ["rdfs:label"];
    for (let p of this.customPropertyOrder) {
      propertiesOrder.push(p.property);
    }

    this.ontologyService = this.$opensilex
      .getService("opensilex-front.VueJsOntologyExtensionService")
      .setRDFTypePropertiesOrder(this.selected.uri, propertiesOrder)
      .then(() => {
        this.setPropertiesOrderRef.hide();
        this.$emit("onDetailChange");
      });
  }

  startSetPropertiesOrder() {
    this.customPropertyOrder = [];
    for (let p of this.properties) {
      if (p.property != "rdfs:label") {
        this.customPropertyOrder.push(p);
      }
    }
    this.setPropertiesOrderRef.show();
  }
}
</script>

<style scoped lang="scss">
.align-right {
  float: right;
}

::v-deep td > span {
  white-space: nowrap;
}
</style>


<i18n>
en:
  OntologyClassDetail:
    title: Object type detail 
    required: Required
    list: List of values
    inherited: Inherited
    dataProperties: Properties
    addDataProperty: Add property
    setPropertiesOrder: Set properties order
    objectProperties: Object properties
    addProperty: Add property
    deleteProperty: Delete property
    setPropertiesOrderInfo: You can define properties display order by drag & drop them in the list below

fr:
  OntologyClassDetail:
    title: Détail du type d'objet
    required: Obligatoire
    list: Liste de valeurs
    inherited: Héritée
    dataProperties: Propriétés
    addDataProperty: Ajouter une propriété
    setPropertiesOrder: Définir l'ordre des propriétés
    objectProperties: Relations vers des objets
    addProperty: Ajouter une propriété
    deleteProperty: Supprimer la propriété
    setPropertiesOrderInfo: Vous pouvez définir l'ordre d'affichage des propriétés par glisser-déposer dans la liste ci-dessous
</i18n>

