<!--
  - ******************************************************************************
  -                         ExperimentSimpleList.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 14/06/2024 15:36
  - Contact: yvan.roux@inrae.fr
  - ******************************************************************************
  -->

<template>
  <div>

    <opensilex-StringFilter
        :filter.sync="nameFilter"
        :lazy="false"
        placeholder="ExperimentList.filter-label"
    ></opensilex-StringFilter>

    <b-table
        striped
        hover
        responsive
        sort-icon-left
        sort-by="name"
        :selectable="false"
        :items="displayableExperiments"
        :fields="fields"
        class="scrollable-container"
    >

      <template v-slot:cell(name)="data">
        <opensilex-UriLink
            :uri="data.item.uri"
            :value="data.item.name"
            :to="{path: '/experiment/details/' + encodeURIComponent(data.item.uri),}"
        ></opensilex-UriLink>
      </template>

      <template v-slot:cell(start_date)="data">
        <opensilex-DateView :value="data.item.start_date"></opensilex-DateView>
      </template>
    </b-table>

  </div>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import VueI18n from "vue-i18n";
import {ExperimentGetListDTO} from "opensilex-core/model/experimentGetListDTO";

@Component
export default class ExperimentSimpleList extends Vue {
  //#region Plugin and services
  public $t: typeof VueI18n.prototype.t;
  //#endregion

  //#region Props

  /**
   * List of experiments to display, if not provided, the list is loaded from the server.
   */
  @Prop({default: null})
  private readonly experimentList: Array<ExperimentGetListDTO>;
  //#endregion

  //#region Computed properties
  private get displayableExperiments() {
    return !this.nameFilter
        ? this.experimentList
        : this.experimentList.filter(experiment => experiment.name.match(new RegExp(this.nameFilter, "i")));
  }

  //#endregion

  //#region Data
  private nameFilter = "";

  private fields = [
    {
      key: "name",
      label: this.$t("component.common.name"),
      sortable: true,
    },
    {
      key: "start_date",
      label: this.$t("component.experiment.startDate"),
      sortable: true,
    }
  ];
  //#endregion
}
</script>


<style scoped lang="scss">
.help {
  font-size: 1.9em;
  background: #f1f1f1;
  color: #00A38D;
  border-radius: 50%;
  margin-left: 10px;
}

.scrollable-container {
  width: 100%;
  height: 600px;
  overflow-y: auto; /* Enables vertical scrolling */
}
</style>