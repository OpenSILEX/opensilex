<template>
  <ValidationObserver ref="validatorRef" v-if="form.type">
    
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="DeviceForm.uri"
      helpMessage="DeviceForm.uri-help"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- type -->
    <opensilex-TypeForm
      :type.sync="form.type"
      :baseType="$opensilex.Oeso.DEVICE_TYPE_URI"
      :required="true"
      helpMessage="DeviceForm.type-help"
      disabled
    ></opensilex-TypeForm>
    
    <!-- label -->
    <opensilex-InputForm
      :value.sync="form.name"
      label="DeviceForm.name"
      type="text"
      :required="true"
      helpMessage="DeviceForm.name-help"
    ></opensilex-InputForm>

    <!-- synonyms -->
    <opensilex-TagInputForm
      v-if= 'form.type.endsWith("Accession") || form.type.endsWith("Variety")'
      :value.sync="form.synonyms"
      label="DeviceForm.subtaxa"
      helpMessage="DeviceForm.subtaxa-help"
      variant="primary"
    ></opensilex-TagInputForm>

    <!-- synonyms -->
    <opensilex-TagInputForm
      v-else
      :value.sync="form.synonyms"
      label="DeviceForm.synonyms"
      helpMessage="DeviceForm.synonyms-help"
      variant="primary"
    ></opensilex-TagInputForm>
    <!-- <label for="tags-basic">Type a new tag and press enter</label>
    <b-form-tags
        v-model="value"        
    ></b-form-tags> -->

    <!-- code -->
    <opensilex-InputForm
      v-if= '!form.type.endsWith("Species")'
      :value.sync="form.code"
      label="DeviceForm.code"
      type="text"
      helpMessage="DeviceForm.code-help"
    ></opensilex-InputForm>
    
    <!-- species -->
    <opensilex-InputForm
      v-if= '!form.type.endsWith("Species")'
      :value.sync="form.species"
      label="DeviceForm.species"
      type="text"
      helpMessage="DeviceForm.species-help"
    ></opensilex-InputForm>
 
    <!-- variety -->
    <opensilex-InputForm
      v-if= '! (form.type.endsWith("Species") || form.type.endsWith("Variety"))'
      :value.sync="form.variety"
      label="DeviceForm.variety"
      type="text"
      helpMessage="DeviceForm.variety-help"
    ></opensilex-InputForm>
    
    <!-- accession -->
    <opensilex-InputForm
      v-if= '! (form.type.endsWith("Species") || form.type.endsWith("Variety") || form.type.endsWith("Accession"))'
      :value.sync="form.accession"
      label="DeviceForm.accession"
      type="text"
      helpMessage="DeviceForm.accession-help"
    ></opensilex-InputForm>
    
    <!-- institute -->
    <opensilex-InputForm
      v-if= '!form.type.endsWith("Species")'
      :value.sync="form.institute"
      label="DeviceForm.institute"
      type="text"
      helpMessage="DeviceForm.institute-help"
    ></opensilex-InputForm>
    
    <!-- year -->
    <opensilex-InputForm
      v-if= '!form.type.endsWith("Species")'
      :value.sync="form.productionYear"
      label="DeviceForm.year"
      type="text"
      helpMessage="DeviceForm.year-help"
    ></opensilex-InputForm>
    
    <!-- comment -->
    <opensilex-InputForm
      :value.sync="form.comment"
      label="DeviceForm.comment"
      type="text"
      helpMessage="DeviceForm.comment-help"
    ></opensilex-InputForm>

    <opensilex-DeviceAttributesTable
      ref="deviceAttributesTable"
      :editMode="editMode"
      :attributesArray='attributesArray'
    ></opensilex-DeviceAttributesTable>

  </ValidationObserver>
</template>

<script lang="ts">
import { Component, Prop, Ref  } from "vue-property-decorator";
import Vue from "vue";
import { DeviceDTO, DevicesService, OntologyService } from "opensilex-core/index"; 
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import Oeso from "../../ontologies/Oeso";

@Component
export default class DeviceForm extends Vue {
  $opensilex: any;
  $store: any;
  service: DevicesService;


  get user() {
    return this.$store.state.user;
  }

  get languages() {
    let langs = [];
    Object.keys(this.$i18n.messages).forEach(key => {
      langs.push({
        id: key,
        label: this.$i18n.t("component.header.language." + key)
      });
    });
    return langs;
  }

  uriGenerated = true;

  attributesArray = [];

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        uri: null,
        type: null,
        name: null,
        code: null,
        species: null,
        variety: null,
        accession: null,
        institute: null,
        productionYear: null,
        comment: null,
        synonyms:[],
        attributes: null
      };
    }
  })
  form;

  reset() {
    this.uriGenerated = true;
  }

  getEmptyForm() {
    return {
      uri: null,
      type: null,
      name: null,
      code: null,
      species: null,
      variety: null,
      accession: null,
      institute: null,
      productionYear: null,
      comment: null,
      synonyms:[],
      attributes: null
    };
  }
  @Ref("deviceAttributesTable") readonly table!: any;

  update(form) {
    form.attributes = this.table.pushAttributes();
    return this.$opensilex
      .getService("opensilex.DeviceService")
      .updateDevice(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Device updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }
  
  getAttributes(form) {
    this.attributesArray = [];
    if (form.attributes != null) {   
      for (const property in form.attributes) {
        let att = {
          attribute: property,
          value: form.attributes[property]
        }
        this.attributesArray.push(att);
      } 
    }
  }
    


}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  DeviceForm:
    name: Name
    name-help: Name used to define this device
    uri: URI
    uri-help: Unique device identifier
    type: Type
    type-help: Device Type
    species : Species URI
    species-help: Species URI of the device
    variety : Variety URI
    variety-help: Variety URI of the device
    accession: Accession URI
    accession-help: Accession URI of the device
    institute: Institute
    institute-help: The code of the institute which the device comes from
    comment: Comment
    comment-help: Description associated to the device 
    year: Production Year
    year-help: Year when the ressource has been produced
    synonyms: Synonyms
    synonyms-help: Fill with a synonym and press Enter
    subtaxa: Subtaxa
    subtaxa-help: Fill with a subtaxa and press Enter
    code: Code
    code-help: The code of the device

fr:
  DeviceForm:
    name: Nom
    name-help: Nom du device
    uri: URI
    uri-help: Identifiant unique du device
    type: Type
    type-help: Type du device
    species : URI de l'espèce
    species-help: URI de l'espèce
    variety : URI de variété
    variety-help: URI de la variété
    accession: URI d'accession
    accession-help: Accession URI of the device
    institute: institut
    institute-help: Code de l'institut dont provient le device
    comment: Commentaire
    comment-help: Description associée au device
    year: Année de production
    year-help: Year when the ressource has been produced
    synonyms: Synonymes
    synonyms-help: Entrer un synonyme et appuyer sur Entrée
    subtaxa: Subtaxa
    subtaxa-help: Entrer un subtaxa et appuyer sur Entrée
    code: Code
    code-help: Code de la ressource génétique
</i18n>

