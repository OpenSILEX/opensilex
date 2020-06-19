<template>
  <ValidationObserver ref="unitValidatorRef">
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="component.variable.unit-uri"
      helpMessage="component.common.unit-uri.help"
      :editMode="editMode"
      :generated.sync="uriGenerated"
      :required="true"
    ></opensilex-UriForm>

    <div class="row">
      <!-- Name -->
      <div class="col-lg-4">
        <opensilex-InputForm
          :value.sync="form.label"
          label="component.variable.unit-name"
          type="text"
          :required="true"
          placeholder="component.variable.unit-name-placeholder"
        ></opensilex-InputForm>
      </div>

      <!-- symbol -->
      <div class="col-lg-3">
        <opensilex-InputForm
          :value.sync="form.symbol"
          label="component.variable.unit-symbol"
          type="text"
          :required="true"
          placeholder="component.variable.unit-symbol-placeholder"
        ></opensilex-InputForm>
      </div>

      <!-- alternative symbol -->
      <div class="col-lg-3">
        <opensilex-InputForm
          :value.sync="form.alternativeSymbol"
          label="component.variable.unit-alternative-symbol"
          type="text"
          :required="false"
          placeholder="component.variable.unit-alternative-symbol-placeholder"
        ></opensilex-InputForm>
      </div>
    </div>

    <!-- <div class="row">
            <div class="col-lg-6">
                
                <opensilex-FormInputLabelHelper
                        label=component.variable.unit-class
                        helpMessage="component.variable.unit-class-help">
                </opensilex-FormInputLabelHelper>

                <multiselect
                        :limit="1"
                        :closeOnSelect=true
                        :placeholder="$t('component.variable.class-placeholder')"
                        v-model="form.type"
                        :options="classList"
                        :custom-label="treeDto => treeDto.name"
                        deselectLabel="You must select one element"
                        track-by="uri"
                        :allow-empty=true
                        :limitText="count => $t('component.common.multiselect.label.x-more', {count: count})"
                />
            </div>
    </div>-->

    <opensilex-TextAreaForm
      :value.sync="form.comment"
      label="component.variable.unit-description"
      placeholder="component.variable.unit-description-placeholder"
    ></opensilex-TextAreaForm>
  </ValidationObserver>
</template>

<script lang="ts">
import { Component, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import { ResourceTreeDTO } from "opensilex-core/model/resourceTreeDTO";
import { OntologyService } from "opensilex-core/api/ontology.service";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";
import { UnitCreationDTO } from "opensilex-core/model/unitCreationDTO";

@Component
export default class UnitForm extends Vue {
  $opensilex: any;

  title = "";
  uriGenerated = true;
  editMode = false;

  errorMsg: String = "";

  @PropSync("form")
  unitDto: UnitCreationDTO;

  classList: Array<ResourceTreeDTO> = [];

  created() {
    let ontologyService: OntologyService = this.$opensilex.getService(
      "opensilex.OntologyService"
    );
    let classUri: string = "http://www.opensilex.org/vocabulary/oeso#Unit";

    ontologyService
      .getSubClassesOf(classUri, true)
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        for (let i = 0; i < http.response.result.length; i++) {
          let dto: ResourceTreeDTO = http.response.result[i];
          this.classList.push(dto);

          if (dto.children) {
            dto.children.forEach(subDto => this.classList.push(subDto));
          }
        }
      })
      .catch(this.$opensilex.errorHandler);
  }

  handleErrorMessage(errorMsg: string) {
    this.errorMsg = errorMsg;
  }

  @Ref("modalRef") readonly modalRef!: any;
  @Ref("unitValidatorRef") readonly unitValidatorRef!: any;

  selectedClass: ResourceTreeDTO = null;

  reset() {
    this.uriGenerated = true;
    return this.unitValidatorRef.reset();
  }

  validate() {
    return this.unitValidatorRef.validate();
  }
}
</script>

<style scoped lang="scss">
</style>