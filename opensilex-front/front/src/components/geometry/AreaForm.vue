<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
        :editMode="editMode"
        :generated.sync="uriGenerated"
        :uri.sync="form.uri"
        helpMessage="component.common.uri-help-message"
        label="AreaForm.uri"
    ></opensilex-UriForm>

    <div class="row">
      <div class="col-lg-6">
        <!-- name -->
        <opensilex-InputForm
            :required="true"
            :value.sync="form.name"
            label="component.experiment.label"
            placeholder="AreaForm.name-placeholder"
            type="text"
        ></opensilex-InputForm>
      </div>
    </div>

    <!-- area type -->
    <b-form-group :label="$t('AreaForm.form-areaType')" v-slot="{ ariaDescribedby }" :disabled="editMode">
      <b-form-radio v-model="toggleAreaType" :aria-describedby="ariaDescribedby" name="structural-area" :value="true" >{{ $t('AreaForm.form-areaType-structural-area') }}</b-form-radio>
      <b-form-radio v-model="toggleAreaType" :aria-describedby="ariaDescribedby" name="temporal-area" :value="false" >{{ $t('AreaForm.form-areaType-temporal-area') }}</b-form-radio>
    </b-form-group>


    <!-- rdf type structural area-->
    <opensilex-TypeForm
        v-if ="toggleAreaType === true"
        :baseType="$opensilex.Oeso.STRUCTURAL_AREA_TYPE_URI"
        :placeholder="$t('AreaForm.form-rdfType-placeholder')"
        :ignoreRoot="false"
        :type.sync="form.rdf_type"
        :disabled="editMode"
    ></opensilex-TypeForm>
    <!-- temporal area = partial event form-->
    <opensilex-EventForm
        v-else
        :linkedToAreaForm="link"
        :form.sync="form"
        :editMode="editMode"
        @change="checkIsInstant"
    ></opensilex-EventForm>

    <!-- description -->
    <opensilex-TextAreaForm
        :value.sync="form.description"
        helpMessage="AreaForm.description-help"
        label="AreaForm.description"
        placeholder="AreaForm.description-placeholder"
    ></opensilex-TextAreaForm>
  </b-form>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {AreaService} from "opensilex-core/api/area.service";

import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class AreaForm extends Vue {
  $opensilex: OpenSilexVuePlugin;
  areaService: AreaService;
  $store: any;
  $i18n: any;
  uriGenerated = true;
  baseType: string = "";
  link: boolean = true;

  @Prop()
  editMode;
  @Prop()
  title: string;
  @Prop({
    default: () => {
      return {
        uri: null,
        name: null,
        relations: [],
        is_structural_area: true,
        rdf_type: null,
        start: null,
        end: null,
        is_instant: false,
        description: "",
        geometry: [],
        targets_positions: [{
          target: undefined,
          position: {
            point: undefined,
            x: undefined,
            y: undefined,
            z: undefined,
            text: undefined,
          }
        }],
        minDate:null,
        maxDate:null
      };
    },
  })
  form ;

  get user() {
    return this.$store.state.user;
  }

  get languageCode(): string {
    return this.$opensilex.getLocalLangCode();
  }

  get toggleAreaType() {
    return this.form.is_structural_area;
  }

  set toggleAreaType(value: boolean) {
    this.form.is_structural_area = value;
  }

  reset() {
    this.uriGenerated = true;
  }

  created() {
    this.baseType = this.$opensilex.Oeev.EVENT_TYPE_URI;
    this.areaService = this.$opensilex.getService("opensilex.AreaService");
  }

  setUri(uri: string) {
    this.form.uri = uri;
  }

  update(form) {

    if (form.is_instant) {
      form.start = null
    }

    //Formatting event
    form.event = {
      rdf_type : form.rdf_type,
      start: form.start,
      end: form.end,
      is_instant : form.is_instant,
      description : form.description,
    }

    console.debug(form);
    return this.areaService
        .updateArea(form)
        .then((http: HttpResponse<OpenSilexResponse>) => {
          let uri = http.response.result;
          console.debug("Area updated", uri);
        })
        .catch(this.$opensilex.errorHandler);
  }

  getEmptyForm() { 
    return {
      uri: null,
      name: null,
      relations: [],
      is_structural_area: true,
      rdf_type: null,
      start: null,
      end: null,
      is_instant: false,
      description: "",
      geometry: [],
      targets_positions: [{
          target: undefined,
          position: {
            point: undefined,
            x: undefined,
            y: undefined,
            z: undefined,
            text: undefined,
          }
        }],
      minDate:null,
      maxDate:null
    };
  }

  create(form) {
    form.geometry = this.$store.state.zone.geometry;

    if (form.description.length == 0) {
      form.description = null;
    }

    if (form.is_instant) {
      form.start = null
    }

    //Formatting event
    form.event = {
      rdf_type : form.rdf_type,
      start: form.start,
      end: form.end,
      is_instant : form.is_instant,
      description : form.description,
    }
    return this.areaService
        .createArea(form)
        .then((http: HttpResponse<OpenSilexResponse>) => {
          let uri = http.response.result;
          console.debug("Area of Geometry created", uri);
        })
        .catch((error) => {
          if (error.status == 409) {
            console.error("Area of Geometry already exists", error);
            this.$opensilex.errorHandler(
                error,
                this.$i18n.t("component.account.errors.user-already-exists")
            );
          } else {
            this.$opensilex.errorHandler(error);
          }
        });
  }
  //When formArea is updated re-bindind is_instant data with vue
  checkIsInstant(event){
    if(this.editMode){
      Vue.set(this.form,this.form.is_instant,event);
      //delete the last prop created when is_instant is set each switch
      let last = Object.keys(this.form)[Object.keys(this.form).length-1];
      Vue.delete(this.form,last);
    }
  }

}
</script>

<style lang="scss" scoped>
P {
  text-align: center;
}
</style>

<i18n>
en:
  AreaForm:
    uri: Area URI
    description: description
    description-help: Description associated with this geometry (protocol quantity)
    description-placeholder: Protocol n°1289 - Amount of water 5 ml/Days.
    name-placeholder: Limestone Mineral Zone, area invaded by pests, ...
    area: annotation
    area-help: Annotation associated with this geometry
    area-placeholder: Description of the nature of the event.
    eventArea: Event area
    structuralArea: Structural area
    selectFile: Select a file
    confidential: Confidential
    confidential-help: The data is confidential
    trial: Trial
    or: Or
    problemDate: problem (end date starts before start date)
    form-rdfType-placeholder: Please select a type
    form-areaType-structural-area: Structural area
    form-areaType-temporal-area: Temporal area
    form-areaType: Type of area

fr:
  AreaForm:
    uri: URI de Zone
    description: description
    description-help: Description associée à cette géométrie (protocole quantité)
    description-placeholder: Protocole n°1289 - Apport d'eau de 5 ml/jour.
    name-placeholder: Zone minérale calcaire, zone envahie par des parasites, ...
    area: Annotation
    area-help: Zone associée à cette géométrie
    area-placeholder: Description de la nature de l'événement.
    eventArea: Zone d'événements
    structuralArea: Zone structurelle
    selectFile: Sélectionner un fichier
    confidential: Confidentiel
    confidential-help: Les données sont confidentielles
    trial: Essai
    or: Ou
    problemDate: problème (la date de fin commence avant la date de début)
    form-rdfType-placeholder: Veuillez sélectionner un type de zone
    form-areaType-structural-area: Zone structurelle
    form-areaType-temporal-area: Zone temporaire
    form-areaType: Type de zone
</i18n>
