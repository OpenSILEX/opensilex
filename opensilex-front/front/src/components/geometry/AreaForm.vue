<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
        :editMode="editMode"
        :generated.sync="uriGenerated"
        :uri.sync="form.uri"
        helpMessage="component.common.uri.help-message"
        label="AreaForm.uri"
    ></opensilex-UriForm>

    <div class="row">
      <div class="col-lg-6">
        <!-- name -->
        <opensilex-InputForm
            :required="true"
            :value.sync="form.name"
            label="component.experiment.label"
            type="text"
        ></opensilex-InputForm>
      </div>
    </div>

    <opensilex-SelectForm
        :label="$t('AreaForm.areaType')"
        :multiple="false"
        :options="optionsArea"
        :required="false"
        :selected.sync="form.optionAreaType"
    ></opensilex-SelectForm>

    <!-- description -->
    <opensilex-TextAreaForm
        :value.sync="form.description"
        helpMessage="AreaForm.comment-help"
        label="AreaForm.comment"
        placeholder="AreaForm.comment-placeholder"
    ></opensilex-TextAreaForm>
  </b-form>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";
import {OntologyService} from "opensilex-core/api/ontology.service";
import Oeso from "../../ontologies/Oeso";

@Component
export default class AreaForm extends Vue {
  $opensilex: any;
  service: OntologyService;
  $store: any;
  $i18n: any;
  uriGenerated = true;
  optionsArea: { label: string; id: string }[] = [];

  @Prop()
  editMode;
  @Prop()
  title: string;
  @Prop({
    default: () => {
      let names = {};
      let defaultLang = "en";
      names[defaultLang] = "";
      return {
        author: null,
        uri: null,
        name: null,
        subject: null,
        optionAreaType: null,
        description: "",
        type: "",
        geometry: [],
        properties: [],
        exactMatch: [],
        closeMatch: [],
        broader: [],
        narrower: [],
      };
    }
  })
  form;

  get user() {
    return this.$store.state.user;
  }

  get languageCode(): string {
    this.loadTypesArea()
    return this.$opensilex.getLocalLangCode();
  }

  // retrieves zone subclasses in ontology
  loadTypesArea() {
    this.service = this.$opensilex.getService(
        "opensilex.OntologyService"
    );

    let optionsArea: { label: string; id: string }[] = [];

    this.service.getSubClassesOf(Oeso.PERENNIAL_AREA, true)
        .then(
            (http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
              const res = http.response.result;
              res.forEach(({name, uri}) => {
                optionsArea.push({id: uri, label: name});
              });
            }
        )
        .catch(this.$opensilex.errorHandler);

    this.optionsArea = optionsArea;
  }

  reset() {
    this.uriGenerated = true;
  }

  getEmptyForm() {
    let names = {};
    let lang = this.languageCode;
    let defaultLang = "en";
    names[lang] = "";
    names[defaultLang] = "";
    return {
      author: null,
      uri: null,
      name: null,
      subject: null,
      optionAreaType: null,
      description: "",
      type: "",
      geometry: [],
      properties: [],
      exactMatch: [],
      closeMatch: [],
      broader: [],
      narrower: [],
    };
  }

  setUri(uri: string) {
    this.form.uri = uri;
  }

  create(form) {
    form.geometry = this.$store.state.zone.geometry;
    if (form.optionAreaType !== null)
      form.type = form.optionAreaType;
    else
      form.type = "vocabulary:Zone";

    return this.$opensilex
        .getService("opensilex.AreaService")
        .createArea(form)
        .then((http: HttpResponse<OpenSilexResponse>) => {
          let uri = http.response.metadata.datafiles[0];
          console.debug("Area of Geometry created", uri);
          form.uri = uri;
          return uri;
        })
        .catch(error => {
          if (error.status == 409) {
            console.error("Area of Geometry already exists", error);
            this.$opensilex.errorHandler(
                error,
                this.$i18n.t("component.user.errors.user-already-exists")
            );
          } else {
            this.$opensilex.errorHandler(error);
          }
        });
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
      comment: comment
      comment-help: Description associated with this geometry (protocol quantity)
      comment-placeholder: Protocol n°1289 - Amount of water 5 ml/Days.
      area: annotation
      area-help: Annotation associated with this geometry
      area-placeholder: Description of the nature of the event.
      eventArea: Event area
      perennialArea: Perennial area
      selectFile: Select a file
      confidential: Confidential
      confidential-help: The data is confidential
      trial: Trial
      or: Or
      problemDate: problem (end date starts before start date)
      areaType: type

  fr:
    AreaForm:
      uri: URI de Zone
      comment: description
      comment-help: Description associée à cette géométrie (protocole quantité)
      comment-placeholder: Protocole n°1289 - Apport d'eau de 5 ml/jour.
      area: Annotation
      area-help: Zone associée à cette géométrie
      area-placeholder: Description de la nature de l'événement.
      eventArea: Zone d'événements
      perennialArea: Zone pérenne
      selectFile: Sélectionner un fichier
      confidential: Confidentiel
      confidential-help: Les données sont confidentielles
      trial: Essai
      or: Ou
      problemDate: problème (la date de fin commence avant la date de début)
      areaType: type
</i18n>
