<template>
  <b-form>
    <!-- Parent -->
    <opensilex-SelectForm
      :selected.sync="form.property"
      :options="propertiesOptions"
      :required="true"
      label="OntologyClassPropertyForm.property"
    ></opensilex-SelectForm>

    <!-- is abstract -->
    <opensilex-CheckboxForm :value.sync="form.isRequired" title="OntologyClassDetail.required"></opensilex-CheckboxForm>

    <!-- is abstract -->
    <opensilex-CheckboxForm :value.sync="form.isList" title="OntologyClassDetail.list"></opensilex-CheckboxForm>
  </b-form>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import OWL from "../../ontologies/OWL";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class OntologyClassPropertyForm extends Vue {
  $opensilex: any;
  OWL = OWL;

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        property: null,
        isRequired: false,
        isList: false
      };
    }
  })
  form;

  getEmptyForm() {
    return {
      property: null,
      isRequired: false,
      isList: false
    };
  }

  availableProperties = null;
  excludedProperties = [];
  setProperties(properties, excludedProperties) {
    this.availableProperties = properties;
    this.excludedProperties = [];
    excludedProperties.forEach(prop => {
      this.excludedProperties.push(prop.property);
    });
  }

  classURI = null;
  setClassURI(classURI) {
    this.classURI = classURI;
  }

  get propertiesOptions() {
    return this.buildTreeListOptions(this.availableProperties, this.excludedProperties);
  }

  create(form) {
    let propertyForm = {
      classURI: this.classURI,
      property: form.property,
      required: form.isRequired,
      list: form.isList
    }

    return this.$opensilex
      .getService("opensilex.OntologyService")
      .addClassPropertyRestriction(propertyForm)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Object type property added", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }

  update(form) {
     let propertyForm = {
      classURI: this.classURI,
      property: form.property,
      required: form.isRequired,
      list: form.isList
    }

    return this.$opensilex
      .getService("opensilex.OntologyService")
      .updateClassPropertyRestriction(propertyForm)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Object type property updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }

   buildTreeListOptions(resourceTrees: Array<any>, excludeProperties) {
        let options = [];

        if (resourceTrees != null) {
            resourceTrees.forEach((resourceTree: any) => {
                let subOption = this.buildTreeOptions(resourceTree, excludeProperties);
                options.push(subOption);
            });
        }

        return options;
    }

    buildTreeOptions(resourceTree: any, excludeProperties: Array<string>) {

        let option = {
            id: resourceTree.uri,
            label: resourceTree.name,
            isDefaultExpanded: true,
            isDisabled: excludeProperties.indexOf(resourceTree.uri) >= 0,
            children: []
        };

        resourceTree.children.forEach(child => {
            let subOption = this.buildTreeOptions(child, excludeProperties);
            option.children.push(subOption);
        });

        if (resourceTree.disabled) {
            option.isDisabled = true;
        }

        if (option.children.length == 0) {
            delete option.children;
        }

        return option;
    }
}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
    OntologyClassPropertyForm:
        property: Property

fr:
    OntologyClassPropertyForm:
        property: Propriété
</i18n>