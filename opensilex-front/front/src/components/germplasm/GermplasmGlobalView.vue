
<template>
  <div class="container-fluid">

    <opensilex-PageHeader
        icon="fa#seedling"
        title="GermplasmView.title"
        description="GermplasmView.description"
        class="detail-element-header"
    ></opensilex-PageHeader>
    <opensilex-PageActions :tabs="true" :returnButton="false">
      <template v-slot>
        <b-nav-item
            :active="isGermplasmTab()"
            :to="{ path: '/germplasm' }"
        >{{ $t("GermplasmView.title") }}
        </b-nav-item>
        <b-nav-item
            :active="isGroupGermplasmTab()"
            :to="{ path: '/germplasm/group' }"
        >{{ $t("GermplasmGroupView.title") }}
        </b-nav-item>
      </template>
    </opensilex-PageActions>
    <opensilex-PageContent>
      <template v-slot>
        <opensilex-GermplasmView
            v-if="isGermplasmTab()"
        ></opensilex-GermplasmView>
        <opensilex-GermplasmGroup
            v-else-if="isGroupGermplasmTab()"
        ></opensilex-GermplasmGroup>
      </template>
    </opensilex-PageContent>
  </div>
</template>


<script lang="ts">

import {Component} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
@Component
export default class GermplasmGlobalView extends Vue {

  $route: any;
  $opensilex: OpenSilexVuePlugin;


  isGermplasmTab() {
    return this.$route.path === "/germplasm";
  }

  isGroupGermplasmTab() {
    return this.$route.path.startsWith("/germplasm/group");
  }


}
</script>

<style scoped>

</style>