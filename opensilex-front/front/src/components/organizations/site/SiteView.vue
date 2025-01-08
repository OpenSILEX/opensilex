<!--
  - ******************************************************************************
  -                         SiteView.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 14/06/2024 13:48
  - Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
  - ******************************************************************************
  -->
<template>
  <div class="container-fluid">
    <opensilex-CreateButton
        id="createSiteButton"
        @click="OnCreateClick()"
        label="SiteView.create"
        class="createButton">
    </opensilex-CreateButton>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-SiteList
            ref="siteList"
            @onEdit="OnSiteListEdit($event)"
            :organizationsForFilter="organizationsForFilter"
        ></opensilex-SiteList>
      </template>
    </opensilex-PageContent>

    <opensilex-ModalForm
        v-if="user.hasCredential(credentials.CREDENTIAL_ORGANIZATION_MODIFICATION_ID)"
        ref="SiteForm"
        lazy="true"
        component="opensilex-SiteForm"
        createTitle="SiteView.create"
        editTitle="SiteView.update"
        icon="ik#ik-map-pin"
        @onCreate="siteList.refresh()"
        @onUpdate="siteList.refresh()"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import {OpenSilexStore} from "../../../models/Store";
import {Component, Prop, Ref} from "vue-property-decorator";
import SiteForm from "../../../components/organizations/site/SiteForm.vue";
import SiteList from "../../../components/organizations/site/SiteList.vue";

@Component
export default class SiteView extends Vue {
  //#region Plugins and services
  public $store: OpenSilexStore;
  //#endregion

  //#region Refs
  @Ref("SiteForm") private readonly siteForm
  @Ref("siteList") private readonly siteList!: SiteList;
  //#endregion

  //#region Props
  @Prop({default: null})
  private organizationsForFilter: Array<string>;
  //#endregion

    //#region Computed
    private get user() {
        return this.$store.state.user;
    }

  private get credentials() {
    return this.$store.state.credentials;
  }

  //#endregion

  //#region Events handlers
  private OnSiteListEdit(dto) {
    this.siteForm.showEditForm(dto);
  }

  private OnCreateClick() {
    this.siteForm.showCreateForm();
  }

  //#endregion


}
</script>

<style scoped lang="scss">
#createSiteButton {
  margin-bottom: 10px;
  margin-top: -15px;
}

</style>

<i18n>
en:
  SiteView:
    title: "Sites"
    description: "Manage and configure sites"
    create: "Add site"
    update: "Edit site"
fr:
  SiteView:
    title: "Sites"
    description: "Gérer et configurer les sites"
    create: "Ajouter un site"
    update: "Modifier un site"
</i18n>