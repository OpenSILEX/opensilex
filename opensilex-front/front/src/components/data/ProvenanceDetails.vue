<template>
  <div class="container-fluid">
    <b-row>
      <opensilex-Card
        icon :label="$t(label)"
        :class="computedClass"
      >
        <template v-slot:rightHeader>
          <div class="ml-3"></div>
        </template>
        <template v-slot:body>

          <!-- uri -->
          <opensilex-UriView
            label="ProvenanceDetails.uri"
            :uri="provenance.uri"
            :value="provenance.uri"
            :to="{path: '/provenances/details/'+ encodeURIComponent(provenance.uri)}"
          ></opensilex-UriView>

          <!-- name -->
          <opensilex-StringView
            label="ProvenanceDetails.label"
            :value="provenance.name"
            class="provenanceDetails"
          ></opensilex-StringView>

          <!-- description -->
          <opensilex-StringView
            label="ProvenanceDetails.description"
            :value="provenance.description"
          ></opensilex-StringView>

          <!-- activity -->
          <opensilex-StringView 
            v-if="provenance.prov_activity != null && provenance.prov_activity.length>0"
            label="ProvenanceDetailsPage.activity"
            :value="provenance.prov_activity[0].rdf_type"
          ></opensilex-StringView>

          <!-- agent -->
          <opensilex-UriListView v-if="agentList.length>0"
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
    default: "ProvenanceDetails.title",
  })
  label: string;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get computedClass(){
    return this.dataImportResult === true ? 'dataImportProvenanceDetails' : 'ProvenanceDetailsVisible'
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

  @Prop({
    default: false
  })
  dataImportResult!: boolean;

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

.ProvenanceDetailsVisible{
  width: auto;
  min-width: auto;
  max-width: 340px;
}

.dataImportProvenanceDetails{
  width: auto;
  min-width: auto;
  max-width: 340px;
}

@media screen and (min-width: 1200px) {
  .ProvenanceDetailsVisible{
    min-width: 340px;
    max-width: 340px;
    margin-left: -20px;
    overflow: hidden;
  }
  .dataImportProvenanceDetails{
    min-width: 340px;
    max-width: 340px;
    margin-left: 0;
    overflow: hidden;
  }
}
</style>
<i18n>
en:
  ProvenanceDetails:
    uri : Uri
    label: Name
    description : Description
    no-provenance-found: No provenance found
    agent : Agent(s)
    title: Provenance description
fr:
  ProvenanceDetails:
    uri : Uri
    label: Nom
    description : Description
    no-provenance-found: Aucune provenance trouvée
    agent : Agent(s)
    title: Description de la provenance

</i18n>