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

    <!-- type -->
    <opensilex-TypeForm
        :baseType="$opensilex.Oeso.AREA_TYPE_URI"
        :placeholder="$t('AreaForm.form-rdfType-placeholder')"
        :required="true"
        :type.sync="form.rdf_type"
    ></opensilex-TypeForm>

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
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
// @ts-ignore
import {OntologyService} from "opensilex-core/api/ontology.service";
// @ts-ignore
import {ObjectUriResponse} from "opensilex-core/model/objectUriResponse";

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
        uri: null,
        name: null,
        rdf_type: null,
        description: "",
        geometry: [],
      };
    },
  })
  form;

  get user() {
    return this.$store.state.user;
  }

  get languageCode(): string {
    return this.$opensilex.getLocalLangCode();
  }

  reset() {
    this.uriGenerated = true;
  }

  update(form) {
    return this.$opensilex
        .getService("opensilex.AreaService")
        .updateArea(form)
        .then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
          let uri = http.response.result;
          console.debug("Area updated", uri);

          return uri;
        })
        .catch(this.$opensilex.errorHandler);
  }

  getEmptyForm() {
    let names = {};
    let lang = this.languageCode;
    let defaultLang = "en";
    names[lang] = "";
    names[defaultLang] = "";
    return {
      uri: null,
      name: null,
      rdf_type: null,
      description: "",
      geometry: [],
    };
  }

  setUri(uri: string) {
    this.form.uri = uri;
  }

  create(form) {
    form.geometry = this.$store.state.zone.geometry;

    return this.$opensilex
        .getService("opensilex.AreaService")
        .createArea(form)
        .then((http: HttpResponse<OpenSilexResponse>) => {
          let uri = http.response.metadata.datafiles[0];
          console.debug("Area of Geometry created", uri);
          form.uri = uri;
          return uri;
        })
        .catch((error) => {
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
    description: description
    description-help: Description associated with this geometry (protocol quantity)
    description-placeholder: Protocol n°1289 - Amount of water 5 ml/Days.
    name-placeholder: Limestone Mineral Zone, Southern Clay Zone, ...
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

fr:
  AreaForm:
    uri: URI de Zone
    description: description
    description-help: Description associée à cette géométrie (protocole quantité)
    description-placeholder: Protocole n°1289 - Apport d'eau de 5 ml/jour.
    name-placeholder: Zone minérale calcaire, zone argileuse méridionale, ...
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
</i18n>
