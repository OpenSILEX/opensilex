<template>
  <div class="container-fluid">
    <opensilex-PageHeader
        icon="ik#ik-globe"
        title="test page"
        class= "detail-element-header"
    ></opensilex-PageHeader>

    <opensilex-PageActions :tabs="true" :returnButton="true">
    </opensilex-PageActions>

    <opensilex-PageContent>
      <!--
          <input
              type="text"
              v-model="searchText"
              placeholder="search"
              v-on:change="foo"/>
          -->

      <div class="row">

        <div class="col-sm-4">
          <opensilex-AgroportalSearch
            @change="onSearchTextChange"
          >
          </opensilex-AgroportalSearch>
          <opensilex-AgroportalResults
              ref="searchResults"
              :text.sync="text"
              @selected="selectResult">
          </opensilex-AgroportalResults>
        </div>

        <div class="col">

        </div>

      </div>
    </opensilex-PageContent>
  </div>
</template>


<script lang="ts">

import Component from "vue-class-component";
import Vue from 'vue';
import {Ref} from "vue-property-decorator";
import AgroportalResults from "./variables/AgroportalResults.vue";
import OpenSilexVuePlugin from "../models/OpenSilexVuePlugin";
import {EntityAgroportalDTO} from "opensilex-core/model/entityAgroportalDTO";

@Component
export default class TestPage extends Vue {

  $opensilex: OpenSilexVuePlugin;

  text = "";

  @Ref("searchResults") readonly searchResults!: AgroportalResults;

  onSearchTextChange(searchedText: string) {
    this.text = searchedText;
  }

  selectResult(result: EntityAgroportalDTO) {
    console.debug(result);
  }

}
</script>

<style scoped>

</style>