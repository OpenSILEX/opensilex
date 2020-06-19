<template>
  <ValidationObserver ref="validatorRef">
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="component.variable.uri"
      helpMessage="component.common.uri.help-message"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <div class="row">
      <div class="col-lg-6">
        <!-- Name -->
        <opensilex-InputForm
          :value.sync="form.label"
          label="component.variable.label"
          type="text"
          :required="true"
          placeholder="component.variable.label-placeholder"
        ></opensilex-InputForm>
      </div>

      <div class="col-lg-6" style="display:none">
        <!-- longname -->
        <opensilex-InputForm
          :value.sync="form.longName"
          label="component.variable.longname"
          type="text"
        ></opensilex-InputForm>
      </div>
    </div>

    <div class="row">
      <div class="col-lg-6">
        <!-- Entity -->
        <opensilex-SelectForm
          label="component.variable.entity"
          :selected.sync="form.entity"
          :multiple="false"
          :optionsLoadingMethod="loadEntities"
          :conversionMethod="objectToSelectNode"
          placeholder="component.variable.form.placeholder.entity"
          @select="updateName(form)"
        ></opensilex-SelectForm>
      </div>

      <div class="col-lg-6">
        <!-- Quality -->
        <opensilex-SelectForm
          label="component.variable.quality"
          :selected.sync="form.quality"
          :multiple="false"
          :options="qualities"
          :conversionMethod="objectToSelectNode"
          placeholder="component.variable.form.placeholder.entity"
          @select="updateName(form)"
          :actionHandler="showQualityCreateForm"
        ></opensilex-SelectForm>
        <opensilex-QualityCreate ref="qualityForm" @onCreate="loadQualities"></opensilex-QualityCreate>
      </div>
    </div>

    <div class="row">
      <div class="col-lg-6">
        <!-- Method uri -->
        <opensilex-SelectForm
          label="component.variable.method"
          :selected.sync="form.method"
          :multiple="false"
          :options="methods"
          :conversionMethod="objectToSelectNode"
          placeholder="component.variable.form.placeholder.method"
          @select="updateLongName(form)"
          :actionHandler="showMethodCreateForm"
        ></opensilex-SelectForm>
        <opensilex-MethodCreate ref="methodForm" @onCreate="loadMethods"></opensilex-MethodCreate>
      </div>
      <div class="col-lg-6">
        <opensilex-SelectForm
          label="component.variable.unit"
          :selected.sync="form.unit"
          :multiple="false"
          :options="units"
          :conversionMethod="objectToSelectNode"
          placeholder="component.variable.form.placeholder.unit"
          @select="updateLongName(form)"
          :actionHandler="showUnitCreateForm"
        ></opensilex-SelectForm>
        <opensilex-UnitCreate ref="unitForm" @onCreate="loadUnits"></opensilex-UnitCreate>
      </div>
    </div>

    <!-- description -->
    <opensilex-TextAreaForm :value.sync="form.description" label="component.variable.comment"></opensilex-TextAreaForm>
  </ValidationObserver>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import { VariableCreationDTO, ResourceTreeDTO } from "opensilex-core/index";
import { VariablesService } from "opensilex-core/api/variables.service";
import { OntologyService } from "opensilex-core/api/ontology.service";
import { EntityCreationDTO } from "opensilex-core/model/entityCreationDTO";
import { EntityGetDTO } from "opensilex-core/model/entityGetDTO";
import { QualityGetDTO } from "opensilex-core/model/qualityGetDTO";
import { MethodGetDTO } from "opensilex-core/model/methodGetDTO";
import { UnitGetDTO } from "opensilex-core/model/unitGetDTO";
import { QualityCreationDTO } from "opensilex-core/model/qualityCreationDTO";
import { UnitCreationDTO } from "opensilex-core/model/unitCreationDTO";
import { MethodCreationDTO } from "opensilex-core/model/methodCreationDTO";

import Vue from "vue";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";

@Component
export default class VariableForm1 extends Vue {
  $opensilex: any;

  @Prop()
  editMode;

  @Prop({ default: true })
  uriGenerated;

  @Prop({ default: true })
  traitClassUriGenerated;

  areFielsRequired() {
    console.log(this.uriGenerated);
    return this.uriGenerated;
  }

  getConcatName(form): string {
    let label: string = form.entity ? form.entity.name : "";
    if (label.length) {
      label += "_";
    }
    if (form.quality) {
      label += form.quality.label;
    }
    return label.replace(/\s+/g, "_").toLowerCase();
  }

  getConcatLongName(form): string {
    let label: string = form.method ? form.method.label : "";
    if (label.length) {
      label += "_";
    }
    if (form.unit) {
      label += form.unit.label;
    }
    return label.replace(/\s+/g, "_").toLowerCase();
  }

  updateName(form) {
    form.label = this.getConcatName(form);
    form.trait.traitLabel = form.label;
    form.longName = form.label;
    if (form.longName.length) {
      form.longName += "_";
    }
    form.longName += this.getConcatLongName(form);
  }

  updateLongName(form) {
    form.longName = this.getConcatName(form);
    if (form.longName.length > 0) {
      form.longName += "_";
    }
    form.longName += this.getConcatLongName(form);
  }

  @Ref("validatorRef") readonly validatorRef!: any;

  get user() {
    return this.$store.state.user;
  }

  @PropSync("form")
  variable: VariableCreationDTO;

  reset() {
    this.traitClassUriGenerated = true;
    return this.validatorRef.reset();
  }

  validate() {
    return this.validatorRef.validate();
  }

  service: VariablesService;
  ontologyService: OntologyService;

  dimensionList: Array<String> = ["volume", "surface", "time", "distance"];

  traitClassUri: string = "http://www.opensilex.org/vocabulary/oeso#Trait";
  entityClassUri: string = "http://www.opensilex.org/vocabulary/oeso#Entity";

  @Ref("qualityForm") readonly qualityForm!: any;
  @Ref("methodForm") readonly methodForm!: any;
  @Ref("unitForm") readonly unitForm!: any;

  units = [];
  methods = [];
  qualities = [];

  showQualityCreateForm() {
    this.qualityForm.showCreateForm();
  }

  showMethodCreateForm() {
    this.methodForm.showCreateForm();
  }

  showUnitCreateForm() {
    this.unitForm.showCreateForm();
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.VariablesService");
    this.ontologyService = this.$opensilex.getService(
      "opensilex.OntologyService"
    );

    this.loadMethods();
    this.loadQualities();
    this.loadUnits();
  }

  loadEntities() {
    return this.ontologyService
      .getSubClassesOf(this.entityClassUri, true)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<any>>>) =>
          http.response.result
      );
  }

  loadQualities() {
    return this.service
      .searchQualities(undefined, undefined, 0, 100)
      .then((http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
        this.qualities = this.objectListToSelect(http.response.result);
      });
  }

  loadMethods() {
    return this.service
      .searchMethods(undefined, undefined, 0, 100)
      .then((http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
        this.methods = this.objectListToSelect(http.response.result);
      });
  }

  loadUnits() {
    return this.service
      .searchUnits(undefined, undefined, 0, 100)
      .then((http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
        this.units = this.objectListToSelect(http.response.result);
      });
  }

  objectListToSelect(list) {
    let itemList = [];
    if (list) {
      for (let i in list) {
        let baseItem: any = this.objectToSelectNode(list[i]);
        let children = this.objectListToSelect(list[i].children);
        if (children.length > 0) {
          baseItem.children = children;
        }
        itemList.push(baseItem);
      }
    }
    return itemList;
  }

  objectToSelectNode(dto) {
    return {
      id: dto.uri,
      label: dto.label
    };
  }
}
</script>
<style scoped lang="scss">
</style>
