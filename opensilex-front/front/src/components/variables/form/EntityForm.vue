<template>
  <ValidationObserver ref="validatorRef">

    <div class="row">

      <div class="col">
        <opensilex-UriForm
            :uri.sync="form.uri"
            label="component.common.uri"
            :generated.sync="uriGenerated"
            :required="true"
            helpMessage="EntityForm.uri-help"
            :editMode="editMode"
        ></opensilex-UriForm>
      </div>
    </div>

    <div v-if="dataLoaded">
      <template>
        <opensilex-TableView
            ref="tableRef"
            :fields="fields"
            :items="labelDTOList"
            :per-pag="pageSize"
            defaultSortBy="prefLabel"
            iconNumberOfSelectedRow="fa#vials"
            class="modalLabelsList"
        >
          <template v-slot:cell(prefLabel)="item">
            {{ item.data.value }}
          </template>
          <template v-slot:cell(altLabels)="item">
            <span v-for="(altLabel, index) in item.data.value" :key="index">
              {{ altLabel }}
              <br>
            </span>
          </template>


          <template v-slot:cell(definition)="item">
            {{ item.data.value }}
          </template>


        </opensilex-TableView>
      </template>
    </div>
    <!--onSubmitSubForm-->

    <opensilex-LabelCreationSubForm ref="labelCreationSubForm" @onSubmitSubForm="getLabelDTOListFromSubForm" @subFormValid="subFormValid = $event" />
    <div class="col">
      <b-form-group
          label="component.skos.semantic-resources-label" label-size="lg"
          label-class="font-weight-bold pt-0"
          class="mb-0"
      >
        <template v-slot:label>{{ $t('component.skos.semantic-resources-label') }}</template>
      </b-form-group>

      <div class="row">
        <div class="col">
          <b-card-text>
            <ul>
              <li
                  v-for="externalOntologyRef in externalOntologiesRefs"
                  :key="externalOntologyRef.name"
              >
                <a
                    target="_blank"
                    v-bind:title="externalOntologyRef.name"
                    v-bind:href="externalOntologyRef.link"
                    v-b-tooltip.v-info.hover.left="externalOntologyRef.description"
                >{{ externalOntologyRef.name }}</a>
              </li>
            </ul>
          </b-card-text>
        </div>

        <div class="col-lg-7">
          <p> {{ $t("EntityForm.ontologies-help") }}</p>
        </div>
      </div>
    </div>
  </ValidationObserver>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import {ExternalOntologies} from "../../../models/ExternalOntologies";
import EntityCreate from "./EntityCreate.vue";
import {VueConstructor} from 'vue';

// @ts-ignore
import {EntityCreationDTO, LabelDTO} from "opensilex-core/index";

import DefaultHeaderComponent from "../../layout/DefaultHeaderComponent.vue";

import VueI18n from "vue-i18n";
import en from '../../../lang/message-en.json';
import fr from '../../../lang/message-fr.json';

@Component({
  computed: {
    DefaultHeaderComponent() {
      return DefaultHeaderComponent
    }
  }
})

export default class EntityForm extends Vue {

  $opensilex: any;
  labelDTOList: Array<LabelDTO> = [];

  labelDTO: Array<LabelDTO> = [];

  dataLoaded: boolean = false;

  tableData: Array<string>;

  title = "";
  uriGenerated = true;

  emits: ['labelDTOs'];

  subFormValid: boolean = false;

  key = 0;

  @Prop({
    default: 1
  })


  pageSize: number;
  created() {

    this.initAttributes();

  }

  initAttributes() {


  }


  getLabelDTOListFromSubForm(labelDTO: LabelDTO) {

    this.labelDTOList.push(labelDTO);
    this.entityDto.labelDTOs.push(labelDTO);
    this.dataLoaded = true;
    this.$emit('labelDTOs', this.labelDTOList);
    this.key += 1;
    console.log("**********************************  pageSize  *************************************",this.pageSize);


  }


  @Ref("tableRef") readonly tableRef!: any;

  getLabelDTOsFromEntityForm(labelDTOs: Array<LabelDTO>) {

    this.labelDTOList = labelDTOs;

  }

  get fields() {
    return [
      {
        key: "prefLabel",
        label: this.$t("component.common.prefLabel"),
        sortable: true,
      },
      {
        key: "altLabels",
        label: this.$t("component.common.altLabels"),
        sortable: false,
      },
      {
        key: "definition",
        label: this.$t("component.common.definition"),
        sortable: false,
      },
      {
        key: "lang",
        label: this.$t("component.common.lang"),
        sortable: true,
      },
    ];
  }

  @Prop()
  editMode;

  errorMsg: String = "";

  @PropSync("form")
  entityDto: EntityCreationDTO;

  externalOntologiesRefs: any[] = ExternalOntologies.getExternalOntologiesReferences(EntityCreate.selectedOntologies);

  handleErrorMessage(errorMsg: string) {
    this.errorMsg = errorMsg;
  }

  @Ref("modalRef") readonly modalRef!: any;
  @Ref("validatorRef") readonly validatorRef!: any;

  handleSubFormValid(subFormValid: boolean) {
    this.subFormValid = subFormValid;
  }

  reset() {

    this.uriGenerated = true;
    return this.validatorRef.reset();

  }

  validate() {

    if (this.labelDTOList.length > 0 || this.subFormValid) {

      console.log("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
      return this.validatorRef.validate();

    }

  }

}
</script>

<style scoped lang="scss">
a {
  color: #007bff;
}

.modalLabelsList {
  overflow: hidden;
}

.variablesCheckboxMarginHighSize {
  margin-left: 15px;
}

@media (min-width: 200px) and (max-width: 1199px) {
  .lowSize {
    margin-left: 15px;
  }
}

</style>