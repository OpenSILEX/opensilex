<template>
  <div class="container-fluid">
    <opensilex-PageActions
      v-if="
        user.hasCredential(
          credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)
          ">
        <opensilex-CreateButton
          @click="experimentForm.showCreateForm()"
          label="component.experiment.search.buttons.create-experiment"
          class="createButton"
        ></opensilex-CreateButton>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-ExperimentList
          ref="experimentList"
          @onEdit="showEditForm($event)"
        ></opensilex-ExperimentList>
      </template>
    </opensilex-PageContent>

    <opensilex-ExperimentForm
      v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
      ref="experimentForm"
      @onCreate="redirectToCreatedExperiment"
      @onUpdate="experimentList.updateSelectedExperiment()"
    ></opensilex-ExperimentForm>

  </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import DTOConverter from "../../models/DTOConverter";

@Component
export default class ExperimentListView extends Vue {
  $opensilex: any;
  $store: any;

  @Ref("experimentForm") readonly experimentForm!: any;
  @Ref("experimentList") readonly experimentList!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  refresh() {
    this.experimentList.refresh();
  }

  showEditForm(uri: string) {
    this.$opensilex
      .getService("opensilex.ExperimentsService")
      .getExperiment(uri)
      .then(http => {

        this.experimentForm.showEditForm(DTOConverter.extractURIFromResourceProperties(http.response.result));
      });
  }

  redirectToCreatedExperiment(experiment) {
    this.$router.push({
      path: '/experiment/details/' + encodeURIComponent(experiment.uri)
    })
  }

}
</script>


<style scoped lang="scss">
.createButton, .helpButton{
  margin-bottom: 10px;
  margin-top: -15px;
  margin-left: 0;
  margin-right: 5px;
}
</style>

<i18n>
en:
  ExperimentList:
    filter-label: Search by name
    label-filter-placeholder: Enter a name
    filter-year: Search by year
    year-filter-placeholder: Enter a year
    filter-species: Search by species

fr:
  ExperimentList:
    filter-label: Filtrer par nom
    label-filter-placeholder: Saisir un nom
    filter-year: Filtrer par année
    year-filter-placeholder: Saisir une année
    filter-species: Filtrer par espèces
</i18n>