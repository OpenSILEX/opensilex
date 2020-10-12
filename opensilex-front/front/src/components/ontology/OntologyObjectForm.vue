<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="OntologyObjectForm.uri-label"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- Name -->
    <opensilex-InputForm
      :value.sync="form.name"
      label="component.common.name"
      type="text"
      :required="true"
      placeholder="OntologyObjectForm.form-name-placeholder"
    ></opensilex-InputForm>

    <!-- Type -->
    <opensilex-TypeForm
      v-if="baseType"
      :type.sync="form.type"
      :baseType="baseType"
      :required="true"
      :disabled="editMode"
      placeholder="OntologyObjectForm.form-type-placeholder"
      @update:type="typeSwitch"
    ></opensilex-TypeForm>

    <slot v-bind:form="form"></slot>
    <div v-for="(v, index) in typeProperties" v-bind:key="index">
      <component
        :is="v.definition.inputComponent"
        :property="v.definition"
        :value.sync="v.property"
        @update:value="updateRelation($event, v.definition.property)"
        :context="context"
      ></component>
    </div>
  </b-form>
</template>

<script lang="ts">
import { Component, Prop, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import Oeso from "../../ontologies/Oeso";

@Component
export default class OntologyObjectForm extends Vue {
  $opensilex: any;

  uriGenerated = true;

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        uri: null,
        type: null,
        name: "",
        relations: []
      };
    }
  })
  form;

  reset() {
    this.uriGenerated = true;
  }

  updateRelation(value, property) {
    for (let i in this.form.relations) {
      let relation = this.form.relations[i];
      if (relation.property == property) {
        relation.value = value;
        return;
      }
    }

    this.form.relations.push({
      property: property,
      value: value
    });
  }

  getEmptyForm() {
    return {
      uri: null,
      type: null,
      name: "",
      relations: []
    };
  }

  setBaseType(baseType) {
    this.baseType = baseType;
    return this;
  }

  context = {};

  setContext(context) {
    this.context = context;
    return this;
  }

  baseType = null;

  typeModel = null;

  get typeProperties() {
    let internalTypeProperties = [];
    if (this.typeModel) {
      for (let i in this.typeModel.dataProperties) {
        let dataProperty = this.typeModel.dataProperties[i];
        if (dataProperty.property != "rdfs:label") {
          let propValue = this.valueByProperties[dataProperty.property];
          if (dataProperty.isList) {
            if (!propValue) {
              propValue = [];
            } else if (!Array.isArray(propValue)) {
              propValue = [propValue];
            }
          }
          internalTypeProperties.push({
            definition: dataProperty,
            property: propValue
          });
        }
      }

      for (let i in this.typeModel.objectProperties) {
        let objectProperty = this.typeModel.objectProperties[i];
        let propValue = this.valueByProperties[objectProperty.property];
        if (objectProperty.isList) {
          if (!propValue) {
            propValue = [];
          } else if (!Array.isArray(propValue)) {
            propValue = [propValue];
          }
        }
        internalTypeProperties.push({
          definition: objectProperty,
          property: propValue
        });
      }
    }

    return internalTypeProperties;
  }

  get valueByProperties(): Object {
    let valueByProperties = {};

    for (let i in this.form.relations) {
      let relation = this.form.relations[i];
      if (
        valueByProperties[relation.property] &&
        !Array.isArray(valueByProperties[relation.property])
      ) {
        valueByProperties[relation.property] = [
          valueByProperties[relation.property]
        ];
      }

      if (Array.isArray(valueByProperties[relation.property])) {
        valueByProperties[relation.property].push(relation.value);
      } else {
        valueByProperties[relation.property] = relation.value;
      }
    }

    return valueByProperties;
  }

  typeSwitch(type) {
    if (type) {
      return this.$opensilex
        .getService("opensilex.VueJsOntologyExtensionService")
        .getClassProperties(this.form.type, this.baseType)
        .then(http => {
          this.typeModel = http.response.result;
          if (!this.editMode) {
            let relations = [];
            for (let i in this.typeModel.dataProperties) {
              let dataProperty = this.typeModel.dataProperties[i];
              if (dataProperty.isList) {
                relations.push({
                  value: [],
                  property: dataProperty.property
                });
              } else {
                relations.push({
                  value: null,
                  property: dataProperty.property
                });
              }
            }

            for (let i in this.typeModel.objectProperties) {
              let objectProperty = this.typeModel.objectProperties[i];
              if (objectProperty.isList) {
                relations.push({
                  value: [],
                  property: objectProperty.property
                });
              } else {
                relations.push({
                  value: null,
                  property: objectProperty.property
                });
              }
            }

            this.form.relations = relations;
          }
        });
    } else {
      this.typeModel = null;
    }
  }

   loadProperties(properties, valueByProperties) {
    for (let i in properties) {
      let property = properties[i];
      if (valueByProperties[property.property]) {
        if (
          property.isList &&
          !Array.isArray(valueByProperties[property.property])
        ) {
          this.typeProperties.push({
            definition: property,
            property: [valueByProperties[property.property]]
          });
        } else {
          this.typeProperties.push({
            definition: property,
            property: valueByProperties[property.property]
          });
        }
      } else if (property.isList) {
        this.typeProperties.push({
          definition: property,
          property: []
        });
      }
    }
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  OntologyObjectForm:
    uri-label: Object URI
    form-name-placeholder: Enter object name
    form-type-placeholder: Select object type

fr:
  OntologyObjectForm:
    uri-label: URI de l'objet
    form-name-placeholder: Saisir le nom de l'objet
    form-type-placeholder: Sélectionner le type de l'objet
</i18n>


