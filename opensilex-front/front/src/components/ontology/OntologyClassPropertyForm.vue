<template>
  <b-form>
    <!-- Parent -->
    <opensilex-SelectForm
      :selected.sync="form.property"
      :options="propertiesOptions"
      :required="true"
      label="OntologyClassPropertyForm.property"
      @update:selected="updateIsListProperty"
    ></opensilex-SelectForm>

    <!-- is abstract -->
    <opensilex-CheckboxForm
      :value.sync="form.is_required"
      title="OntologyClassDetail.required"
    ></opensilex-CheckboxForm>

    <!-- is abstract -->
    <opensilex-CheckboxForm
      :value.sync="form.is_list"
      :disabled="this.dataTypeProperties.indexOf(form.property) >= 0"
      title="OntologyClassDetail.list"
    ></opensilex-CheckboxForm>
  </b-form>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import OWL from "../../ontologies/OWL";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import {VueRDFTypePropertyDTO} from "../../lib";
import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";

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
        is_required: false,
        is_list: false,
      };
    },
  })
  form;

  getEmptyForm() {
    return {
      property: null,
      is_required: false,
      is_list: false,
    };
  }

  availableProperties = null;
  excludedProperties = [];
  dataTypeProperties = [];
  setProperties(properties: ResourceTreeDTO[], excludedProperties: VueRDFTypePropertyDTO[]) {

    let excludedUris = new Set<string>(
        excludedProperties.map(vueProperty => vueProperty.property)
    );
    console.log(excludedUris);
    console.log(properties);
    this.availableProperties = properties.filter(
        dto => ! excludedUris.has(dto.uri)
    );
    this.excludedProperties = [];

    this.dataTypeProperties = [];
    this.availableProperties.forEach((prop) => {
      if (prop.rdf_type == "owl:DatatypeProperty") {
        this.dataTypeProperties.push(prop.uri);
      }
    });
  }

  rdf_type = null;
  setClassURI(rdf_type) {
    this.rdf_type = rdf_type;
  }

  domain = null;
  setDomain(domain) {
    this.domain = domain;
  }

  get propertiesOptions() {
    return this.buildTreeListOptions(
      this.availableProperties,
      this.excludedProperties
    );
  }

  updateIsListProperty(){
    if(! this.form.property || ! this.dataTypeProperties){
      return;
    }

    // if the property is a data property then set is_list to false, since we don't actually handle generics list component for data-property
    if(this.dataTypeProperties.indexOf(this.form.property) >= 0){
      this.form.is_list = false;
    }
  }

  create(form) {
    let propertyForm = {
      rdf_type: this.rdf_type,
      property: form.property,
      required: form.is_required,
      list: form.is_list,
      domain: this.domain
    };

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
      rdf_type: this.rdf_type,
      property: form.property,
      required: form.is_required,
      list: form.is_list,
      domain: this.domain
    };

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
      children: [],
    };

    resourceTree.children.forEach((child) => {
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