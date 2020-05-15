<template>
  <ValidationObserver ref="validatorRef">
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="component.germplasm.table.fields.uri"
      helpMessage="component.experiment.uri-help"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- rdfType -->
    <opensilex-TypeForm
      :type.sync="form.rdfType"
      :baseType="$opensilex.Oeso.GERMPLASM_TYPE_URI"
      :required="true"
      helpMessage="component.germplasm.form.rdfType-help"
      disabled
    ></opensilex-TypeForm>
    
    <!-- label -->
    <opensilex-InputForm
      :value.sync="form.label"
      label="component.germplasm.table.fields.label"
      type="text"
      :required="true"
      helpMessage="component.germplasm.form.labelHelp"
    ></opensilex-InputForm>
    
    <!-- fromSpecies -->
    <opensilex-InputForm
      :value.sync="form.fromSpecies"
      label="component.germplasm.table.fields.fromSpecies"
      type="text"
      helpMessage="component.germplasm.form.species-help"
    ></opensilex-InputForm>
 
    <!-- fromVariety -->
    <opensilex-InputForm
      :value.sync="form.fromVariety"
      label="component.germplasm.table.fields.fromVariety"
      type="text"
      helpMessage="component.germplasm.form.variety-help"
    ></opensilex-InputForm>
    
    <!-- fromAccession -->
    <opensilex-InputForm
      :value.sync="form.fromAccession"
      label="component.germplasm.table.fields.fromAccession"
      type="text"
      helpMessage="component.germplasm.form.accession-help"
    ></opensilex-InputForm>
    
    <!-- institute -->
    <opensilex-InputForm
      :value.sync="form.institute"
      label="component.germplasm.table.fields.institute"
      type="text"
      helpMessage="component.germplasm.form.institute-help"
    ></opensilex-InputForm>
    
    <!-- year -->
    <opensilex-InputForm
      :value.sync="form.productionYear"
      label="component.germplasm.table.fields.year"
      type="text"
      helpMessage="component.germplasm.form.year-help"
    ></opensilex-InputForm>
    
    <!-- comment -->
    <opensilex-InputForm
      :value.sync="form.comment"
      label="component.germplasm.table.fields.comment"
      type="text"
      helpMessage="component.germplasm.form.comment-help"
    ></opensilex-InputForm>
  </ValidationObserver>
</template>

<script lang="ts">
import { Component, Prop, Ref  } from "vue-property-decorator";
import Vue from "vue";
import { GermplasmCreationDTO, GermplasmGetDTO, GermplasmSearchDTO, GermplasmService, OntologyService, ResourceTreeDTO } from "opensilex-core/index"; 
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import Oeso from "../../ontologies/Oeso";

@Component
export default class GermplasmForm extends Vue {
  $opensilex: any;
  service: GermplasmService;


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

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        uri: null,
        rdfType: null,
        label: null,
        fromSpecies: null,
        fromVariety: null,
        fromAccession: null,
        institute: null,
        productionYear: null,
        comment: null
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
      rdfType: null,
      label: null,
      fromSpecies: null,
      fromVariety: null,
      fromAccession: null,
      institute: null,
      productionYear: null,
      comment: null
    };
  }

  update(form) {
    return this.$opensilex
      .getService("opensilex.GermplasmService")
      .updateGermplasm(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Germplasm updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }

}
</script>

<style scoped lang="scss">
</style>

