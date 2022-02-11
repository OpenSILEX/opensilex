<template>
  <div class="container-fluid">
    <!-- <opensilex-PageHeader
      icon="ik#ik-settings"
      title="component.menu.security.profiles"
      description="component.profile.description"
    ></opensilex-PageHeader> -->

    <!-- <opensilex-PageActions
      class= "pageActions"
      v-if="
        user.hasCredential(
          credentials.CREDENTIAL_DEVICE_MODIFICATION_ID)
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
        <b-dropdown-item href="#"> -->
          <opensilex-CreateButton
            v-if="
            user.hasCredential(
              credentials.CREDENTIAL_PROFILE_MODIFICATION_ID)"
            @click="profileForm.showCreateForm()"
            label="component.profile.add"
            class="createButton">
          </opensilex-CreateButton>
        <!-- </b-dropdown-item>
      </b-dropdown>
    </opensilex-PageActions> -->

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-ProfileList
          ref="profileList"
          v-bind:credentialsGroups="credentialsGroups"
          @onEdit="showEditForm($event)"
        ></opensilex-ProfileList>
      </template>
    </opensilex-PageContent>

    <opensilex-ModalForm
      ref="profileForm"
      component="opensilex-ProfileForm"
      createTitle="component.profile.add"
      editTitle="component.profile.update"
      modalSize="lg"
      icon="ik#ik-settings"
      @onCreate="profileList.refresh()"
      @onUpdate="profileList.refresh()"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { SecurityService, CredentialsGroupDTO } from "opensilex-security/index";

@Component
export default class ProfileView extends Vue {
  $opensilex: any;
  service: SecurityService;
  $store: any;
  credentialsGroups: Array<CredentialsGroupDTO> = [];
  credentialsMapping: any = {};

  @Ref("profileForm") readonly profileForm!: any;
  @Ref("profileList") readonly profileList!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  showEditForm(dto){
    let copydto = JSON.parse(JSON.stringify(dto));
    this.profileForm.showEditForm(copydto);
  }
}
</script>

<style scoped lang="scss">
.pageActions {
    position: fixed;
    top: 8px;
    left: 375px;
    width: 10px;
    background: none;
    z-index: 1100;
}
.createButton{
  margin-bottom: 10px;
  margin-top: -15px
}

@media (min-width: 200px) and (max-width: 675px) {
  .pageActions {
   left: 285px
  }
}
</style>
