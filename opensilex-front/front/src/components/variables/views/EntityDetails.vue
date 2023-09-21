<template>
  <b-card v-if="selected && selected.uri">
    <template v-slot:header>
      <h3>
        <opensilex-Icon icon="ik#ik-clipboard" />
        {{$t("component.common.details-label")}}
      </h3>
    </template>
    <opensilex-UriView :uri="selected.uri"></opensilex-UriView>

    <opensilex-TableView
          ref="tableRef"
          :fields="fields"
          :items="labelDTOList"
          defaultSortBy="prefLabel"
          iconNumberOfSelectedRow="fa#vials"
          class="modalLabelsList"
      >
        <template v-slot:cell(prefLabel)="item">
          {{ item.data.value }}
        </template>

      <template v-slot:cell(shortLabel)="item">
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
  </b-card>
</template>

<script lang="ts">
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import {EntityDetailsDTO} from "opensilex-core/model/entityDetailsDTO";
import {EntityGetDTO} from "opensilex-core/model/entityGetDTO";

@Component
export default class EntityDetails extends Vue {

  $opensilex: any;

  @Prop()
  selected ;

  labelDTOList: Array<any>;

  created() {

    this.initLabelDTOList();

  }

  @Watch("selected")
  onSelectedChange() {
    this.initLabelDTOList();
  }

  initLabelDTOList() {
    this.labelDTOList = []

    const keys = Object.keys(this.selected.multiLabelsDTO.prefLabels);

    keys.forEach((key) => {
      const labelDTO = {
        prefLabel: this.selected.multiLabelsDTO.prefLabels[key],
        shortLabel: this.selected.multiLabelsDTO.shortLabels[key],
        altLabels: this.selected.multiLabelsDTO.altLabels[key],
        definition: this.selected.multiLabelsDTO.definitions[key],
        lang: key,
      };

      this.labelDTOList.push(labelDTO);

    });
    console.log(JSON.stringify(this.labelDTOList))

  }

  get fields() {

    return [
      {
        key: "prefLabel",
        label: this.$t("component.common.prefLabel"),
        sortable: true,
      },
      {
        key: "shortLabel",
        label: this.$t("component.common.shortLabel"),
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
</style>

