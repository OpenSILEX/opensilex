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

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import {ExternalOntologies} from "../../../models/ExternalOntologies";
import EntityCreate from "../form/EntityCreate.vue";
import {VueConstructor} from 'vue';

// @ts-ignore
import {EntityCreationDTO, LabelDTO, MultiLabelsDTO} from "opensilex-core/index";

import DefaultHeaderComponent from "../../layout/DefaultHeaderComponent.vue";

import VueI18n from "vue-i18n";
import en from '../../../lang/message-en.json';
import fr from '../../../lang/message-fr.json';
import LabelsCreationSubForm from "../form/LabelsCreationSubForm.vue";

@Component({
  computed: {
    DefaultHeaderComponent() {
      return DefaultHeaderComponent
    }
  }
})

export default class LabelsDetails extends Vue {

  $opensilex: any;

  @Ref("labelDTOList")
  labelDTOList: Array<LabelDTO>;

  @Ref("tableRef") readonly tableRef!: any;

  pageSize: number;

  created() {

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