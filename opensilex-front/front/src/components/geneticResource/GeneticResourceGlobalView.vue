
<template>
  <div class="container-fluid">

    <opensilex-PageHeader
        icon="fa#seedling"
        title="GeneticResourceView.title"
        description="GeneticResourceView.description"
        class="detail-element-header"
    ></opensilex-PageHeader>
    <opensilex-PageActions :tabs="true" :returnButton="false">
      <template v-slot>
        <b-nav-item
            :active="isGeneticResourceTab()"
            :to="{ path: '/geneticResource' }"
        >{{ $t("GeneticResourceView.title") }}
        </b-nav-item>
        <b-nav-item
            :active="isGroupGeneticResourceTab()"
            :to="{ path: '/geneticResource/group' }"
        >{{ $t("GeneticResourceGroupView.title") }}
        </b-nav-item>
      </template>
    </opensilex-PageActions>
    <opensilex-PageContent>
      <template v-slot>
        <opensilex-GeneticResourceView
            v-if="isGeneticResourceTab()"
        ></opensilex-GeneticResourceView>
        <opensilex-GeneticResourceGroup
            v-else-if="isGroupGeneticResourceTab()"
        ></opensilex-GeneticResourceGroup>
      </template>
    </opensilex-PageContent>
  </div>
</template>


<script lang="ts">

import {Component} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
@Component
export default class GeneticResourceGlobalView extends Vue {

  $route: any;
  $opensilex: OpenSilexVuePlugin;


  isGeneticResourceTab() {
    return this.$route.path === "/geneticResource";
  }

  isGroupGeneticResourceTab() {
    return this.$route.path.startsWith("/geneticResource/group");
  }


}
</script>

<style scoped>

</style>