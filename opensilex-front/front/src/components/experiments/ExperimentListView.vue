<template>
  <div class="container-fluid">

    <!-- <opensilex-PageHeader
      icon="ik#ik-layers"
      title="component.menu.experiments"
      description="component.experiment.search.description"
    ></opensilex-PageHeader> -->

    <opensilex-PageActions
    class= "pageActions"
      v-if="
        user.hasCredential(
          credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)
          ">
    <b-dropdown
      id="AddDropdown"
      class="top-menu-add-btn"
      :title="user.getAddMessage()"
      variant="link"
    >    
    <template v-slot:button-content>
      <i class="icon ik ik-plus header-plus"></i>
    </template>
      <b-dropdown-item href="#">  
        <opensilex-CreateButton
          @click="experimentForm.showCreateForm()"
          label="component.experiment.search.buttons.create-experiment"
          class="createButton"
        ></opensilex-CreateButton>
      </b-dropdown-item>
    </b-dropdown>
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
      @onUpdate="refresh()"
    ></opensilex-ExperimentForm>

  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ExperimentListView extends Vue {
  $opensilex: any;
  $store: any;

  @Ref("experimentForm") readonly experimentForm!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("experimentList") readonly experimentList!: any;
  refresh() {
    this.experimentList.refresh();
  }

  showEditForm(uri: string) {
    this.$opensilex
      .getService("opensilex.ExperimentsService")
      .getExperiment(uri)
      .then(http => {

        this.experimentForm.showEditForm(this.convertDtoBeforeEditForm(http.response.result));
      });
  }

  convertDtoBeforeEditForm(experiment) {  //update experiment don't need detailled list attributs
    let convertedExperiment= experiment;
    
    if (
     experiment.projects &&
     experiment.projects.length>0
    ) {

     convertedExperiment.projects = experiment.projects.map(project => {
        return project.uri;
      });
    }

    if (
      experiment.organisations &&
      experiment.organisations.length >0 
    ) {
     convertedExperiment.organisations = experiment.organisations.map(
        organisation => {
          return organisation.uri;
        }
      );
    }
    return convertedExperiment;
  }

  redirectToCreatedExperiment(experiment) {
    this.$router.push({
      path: '/experiment/details/' + encodeURIComponent(experiment.uri)
    })
  }

}
</script>


<style scoped lang="scss">
.pageActions {
    position: fixed;
    top: 8px;
    left: 440px;
    width: 10px;
    background: none;
    z-index: 1100;
}

@media (min-width: 200px) and (max-width: 675px) {
  .pageActions {
   left: 330px
  }
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