<template>
  <div class="container-fluid">
    <b-row>
      <opensilex-Card icon :label="$t(label)">
        <template v-slot:rightHeader>
          <div class="ml-3"></div>
        </template>
        <template v-slot:body>
          <opensilex-UriView
            label="ProvenanceDetails.uri"
            :uri="provenance.uri"
          ></opensilex-UriView>
          <opensilex-StringView
            label="ProvenanceDetails.label"
            :value="provenance.name"
          ></opensilex-StringView>

          <opensilex-StringView
            label="ProvenanceDetails.description"
            :value="provenance.description"
          ></opensilex-StringView>
          <opensilex-UriListView
            label="ProvenanceDetails.agent"
            :list="agentList"
          ></opensilex-UriListView>
        </template>
      </opensilex-Card>
    </b-row>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref, PropSync } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ProvenanceDetails extends Vue {
  $opensilex: any;
  $store: any;
  $route: any;
  $t: any;
  $i18n: any;

  @Prop({
    default: "DataImportForm.provenance.description",
  })
  label: string;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Prop({
    default: () => {
      return {
        uri: null,
        name: null,
        description: null,
        experiments: [],
        prov_activity: [],
        prov_agent: [],
      };
    },
  })
  provenance: any;

  update(provenance) {
    this.$emit("onUpdate", provenance);
  }

  get agentList() {
    if (
      this.provenance.prov_agent != null &&
      this.provenance.prov_agent != undefined
    ) {
      return this.provenance.prov_agent.map((item) => {
        return {
          uri: item.uri,
          url: null,
          value: item.uri,
        };
      });
    } else {
      return [];
    }
  }

  successMessage(provenance) {
    return this.$i18n.t("component.provenance.name") + " " + provenance.name;
  }
}
</script>

<style scoped lang="scss">
.details-actions-row {
  margin-top: -35px;
  margin-left: -15px;
  margin-right: 15px;
}
</style>
<i18n>
en:
  ProvenanceDetails:
    uri : Uri
    label: Name
    description : Description
    provenanceLevels: Levels
    no-provenance-found: No provenance found
    agent : Agent(s)
fr:
  ProvenanceDetails:
    uri : Uri
    label: Name
    description : Description
    provenanceLevels: Niveaux de facteurs associés
    no-provenance-found: Aucun provenance trouvée
    agent : Agent(s)

</i18n>