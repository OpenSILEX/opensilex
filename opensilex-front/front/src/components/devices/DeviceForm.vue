<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri" 
      label="DeviceForm.uri"
      :editMode="editMode"
      :generated.sync="uriGenerated"
      helpMessage="DeviceForm.uri-help"
    ></opensilex-UriForm>

    <!-- rdfType -->
    <opensilex-TypeForm
      :type.sync="form.rdf_type"
      :baseType="$opensilex.Oeso.DEVICE_TYPE_URI"
      helpMessage="DeviceForm.type-help"    
      :required="true"
    ></opensilex-TypeForm>

    <!-- name -->
    <opensilex-InputForm
      :value.sync="form.name"
      label="DeviceForm.name"
      type="text"
      helpMessage="DeviceForm.name-help"
      :required="true"
    ></opensilex-InputForm>

    <!-- brand -->
    <opensilex-InputForm
      :value.sync="form.brand"
      label="DeviceForm.brand"
      type="text"
      helpMessage="DeviceForm.brand-help"
    ></opensilex-InputForm>

    <!-- constructor_model -->
    <opensilex-InputForm
      :value.sync="form.constructor_model"
      label="DeviceForm.constructor_model"
      type="text"
      helpMessage="DeviceForm.constructor_model-help"
    ></opensilex-InputForm>

    <!-- serial_number -->
    <opensilex-InputForm
      :value.sync="form.serial_number"
      label="DeviceForm.serial_number"
      type="number"
      helpMessage="DeviceForm.serial_number-help"
    ></opensilex-InputForm>

   <!--person_in_charge -->
    <opensilex-UserSelector
      :users.sync="form.person_in_charge"
      label="DeviceForm.person_in_charge"
      helpMessage="DeviceForm.person_in_charge-help"
    ></opensilex-UserSelector>

    <!-- start_up -->
    <opensilex-InputForm
      :value.sync="form.start_up"
      label="DeviceForm.start_up"
      type="date"
      helpMessage="DeviceForm.start_up-help"
    ></opensilex-InputForm>

    <!-- removal -->
    <opensilex-InputForm
      :value.sync="form.removal"
      label="DeviceForm.removal"
      type="date"
      helpMessage="DeviceForm.removal-help"
    ></opensilex-InputForm>

    <!-- description -->
    <opensilex-TextAreaForm
      :value.sync="form.description"
      label="DeviceForm.description"
      type="text"
      helpMessage="DeviceForm.description-help"
    ></opensilex-TextAreaForm>

    <!-- metadata -->
    <opensilex-DeviceAttributesTable
      ref="deviceAttributesTable"
      :attributesArray='attributesArray'
    ></opensilex-DeviceAttributesTable>

    <!-- relation -->
    <!-- <opensilex-VariableSelector 
      :editMode="editMode"
      label="DeviceForm.variable"
      helpMessage="DeviceForm.variable-help"
      :variables.sync="form.relations"
    ></opensilex-VariableSelector> -->

    <opensilex-DeviceVariablesTable
      ref="deviceVariablesTable"
      :editMode="editMode"
      :relations.sync="form.relations"
    ></opensilex-DeviceVariablesTable>


  </b-form>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import {DevicesService} from "opensilex-core/index"; 
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class DeviceForm extends Vue {
  $opensilex: any;
  service: DevicesService;
  $store: any;
  $t: any;

  uriGenerated = true;

  @Ref("deviceAttributesTable") readonly attTable!: any;
  @Ref("deviceVariablesTable") readonly varTable!: any;

  get user() {
    return this.$store.state.user;
  } 

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        device: {
            uri: undefined,
            name: undefined,
            rdf_type: undefined,
            brand: undefined,
            constructor_model: undefined,
            serial_number: undefined,
            person_in_charge: undefined,
            start_up: undefined,
            removal: undefined,
            description: undefined,
            metadata: undefined,
            relations: [
              {
                property: null,
                value: null
              }
            ]
        }
      }
    }
  })
  form: any;

  getEmptyForm() {
    return {
      uri: undefined,
      name: undefined,
      rdf_type: undefined,
      brand: undefined,
      constructor_model: undefined,
      serial_number: undefined,
      person_in_charge: undefined,
      start_up: undefined,
      removal: undefined,
      description: undefined,
      metadata: undefined,
      relations: [
        {
          property: null,
          value: null
        }
      ]
    };
  }

  reset() {
    this.uriGenerated = true;
  }

  update(form) {
    form.metadata = this.attTable.pushAttributes();   
    return this.$opensilex
      .getService("opensilex.DevicesService")
      .updateDevice(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("device updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }

  attributesArray = [];
  getAttributes(form) {
    this.attributesArray = [];
    if (form.metadata != null) {   
      for (const property in form.metadata) {
        let att = {
          attribute: property,
          value: form.metadata[property]
        }
        this.attributesArray.push(att);
      } 
    }
  }

  addEmptyRow() {
    this.form.relations.unshift({
      property: "vocabulary:measures",
      value: null,
    });
  }

}

</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  DeviceForm:
    uri: URI 
    uri-help: Unique device identifier autogenerated 
    type: Type
    type-help: Device Type
    name: Name
    name-help: A name given to the device
    brand: Brand
    brand-help: A brand of the device
    constructor_model: Constructor model
    constructor_model-help: A constructor model of the device
    serial_number: Serial number
    serial_number-help: A serial number of the device
    person_in_charge: Person in charge
    person_in_charge-help: Person in charge of the device
    start_up: Start up
    start_up-help: Date of start up
    removal: Removal
    removal-help: Date of removal
    description: Description
    description-help: Description associated of the device
    variable: Variable
    variable-help: Insert one or several URI's variables

fr:
  DeviceForm:
    uri: URI 
    uri-help: Identifiant unique du dispositif généré automatiquement 
    type: Type
    type-help: Type de dispositif 
    name: Nom
    name-help: Nom du dispositif 
    brand: Marque du dispositif
    brand-help: Marque du dispositif
    constructor_model: Modèle constructeur 
    constructor_model-help: Modèle constructeur du dispositif
    serial_number: Numéro de série 
    serial_number-help: Numéro de série du dispositif  
    person_in_charge: Personne responsable
    person_in_charge-help: Personne responsable du dispositif
    start_up: Date d'obtention
    start_up-help: Date d'obtention du dispositif
    removal: Date de mise hors service
    removal-help: Date de mise hors service du dispositif
    description: Description
    description-help: Description associée au dispositif
    variable: Variable
    variable-help: Insérer une ou plusieurs URI de variables

</i18n>
