<template>
  <div class="container-fluid">
    <!-- <opensilex-PageHeader
      icon="ik#ik-folder"
      title="component.menu.projects"
      description="component.project.search-description"
    ></opensilex-PageHeader> -->
    <opensilex-PageActions
        v-if="
          user.hasCredential(
            credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)
      ">
      <!-- <b-dropdown
        id="AddDropdown"
        class="top-menu-add-btn"
        :title="user.getAddMessage()"
        variant="link"
      >
      <template v-slot:button-content>
        <i class="icon ik ik-plus header-plus"></i>
      </template>
        <b-dropdown-item href="#"> -->
          <opensilex-CreateButton
          @click="projectForm.showCreateForm()"
          label="component.project.add"
          class="createButton"
          ></opensilex-CreateButton>
        <!-- </b-dropdown-item>
      </b-dropdown> -->
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-ProjectList ref="projectList" @onEdit="projectForm.showEditForm($event)"></opensilex-ProjectList>
      </template>
    </opensilex-PageContent>
    <opensilex-ProjectForm
      v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)"
      ref="projectForm"
      @onCreate="redirectToCreatedProject"
      @onUpdate="projectList.refresh()"
    ></opensilex-ProjectForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
@Component
export default class ProjectView extends Vue {
  $opensilex: any;
  $store: any;

  @Ref("projectForm") readonly projectForm!: any;
  @Ref("projectList") readonly projectList!: any;

  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }

  redirectToCreatedProject(project) {
    this.$router.push({
      path: '/project/details/' + encodeURIComponent(project.uri)
    })
    
  }
}
</script>

<style scoped lang="scss">
// .pageActions {
//     position: fixed;
//     top: 8px;
//     left: 380px;
//     width: 10px;
//     background: none;
//     z-index: 1100;
// }

// @media (min-width: 200px) and (max-width: 675px) {
//   .pageActions {
//    left: 295px
//   }
// }
.createButton {
  margin-bottom: 10px;
  margin-top: -15px;
  margin-left: 0;
}
</style>