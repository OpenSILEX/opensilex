<template>
  <div>
    <opensilex-StringFilter
        :filter.sync="filter"
        @update="updateFilter()"
        placeholder="component.account.filter-placeholder"
        :debounce="300"
        :lazy="false"
    ></opensilex-StringFilter>

    <opensilex-TableAsyncView
        ref="tableRef"
        :searchMethod="searchAccounts"
        :fields="fields"
        defaultSortBy="email"
    >
      <template v-slot:cell(uri)="{data}">
        <opensilex-UriLink
            :uri="data.item.uri"
            :value="data.item.uri"
            :noExternalLink="true"
            :isClickable="false"
        ></opensilex-UriLink>
      </template>

      <template v-slot:cell(last_name)="{data}">
        <opensilex-PersonContact
            v-if="personByAccountUri[data.item.uri]"
            :personContact="personByAccountUri[data.item.uri]"
        />
        <div v-else/>
      </template>

      <template v-slot:cell(email)="{data}">
        <a :href="'mailto:' + data.item.email">{{ data.item.email }}</a>
      </template>

      <template v-slot:cell(admin)="{data}">
        <span class="capitalize-first-letter" v-if="data.item.admin">{{ $t("component.common.yes") }}</span>
        <span class="capitalize-first-letter" v-if="!data.item.admin">{{ $t("component.common.no") }}</span>
      </template>

      <template v-slot:row-details="{data}">
        <strong class="capitalize-first-letter">{{ $t("component.account.user-groups") }}:</strong>
        <ul>
          <li
              v-for="groupDetail in  groupDetailsByAccountUri[data.item.uri]"
              v-bind:key="groupDetail.uri"
          >{{ groupDetail.name }}
          </li>
        </ul>
      </template>

      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <div class="checkEnable"
               :title="data.item.enable ? $t('component.account.enable') : $t('component.account.disable')">
            <b-check
                @change="changeEnable(data.item)"
                v-if="displayEnableButton(data.item)"
                :checked=" data.item.enable "
                variant="outline-success"
                switch
            ></b-check>
          </div>
          <opensilex-DetailButton
              @click="showUsersGroups(data)"
              label="component.account.details"
              :detailVisible="data.detailsShowing"
              :small="true"
          ></opensilex-DetailButton>
          <opensilex-EditButton
              v-if="user.hasCredential(credentials.CREDENTIAL_ACCOUNT_MODIFICATION_ID)"
              @click="$emit('onEdit', data.item)"
              label="component.account.update"
              :small="true"
          ></opensilex-EditButton>
          <opensilex-DeleteButton
              v-if="user.hasCredential(credentials.CREDENTIAL_ACCOUNT_DELETE_ID)"
              @click="deleteAccount(data.item.uri)"
              label="component.account.delete"
              :small="true"
          >
          </opensilex-DeleteButton>
        </b-button-group>
      </template>
    </opensilex-TableAsyncView>
  </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import {SecurityService} from "opensilex-security/index";
import {PersonDTO} from "opensilex-security/model/personDTO";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import {SlotDetails} from "../common/views/TableAsyncView.vue";
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";
import {AccountUpdateDTO} from "opensilex-security/model/accountUpdateDTO";
import {AccountGetDTO} from "opensilex-security/model/accountGetDTO";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class AccountList extends Vue {
  $opensilex: OpenSilexVuePlugin;
  service: SecurityService;
  $store: any;
  $route: any;

  fields = [
    {
      key: "uri",
      label: "component.common.uri"
    },
    {
      key: "last_name",
      label: "component.account.linked-person"
    },
    {
      key: "email",
      label: "component.account.email",
      sortable: true
    },
    {
      key: "admin",
      label: "component.account.admin",
      sortable: true
    },
    {
      label: "component.common.actions",
      key: "actions",
      class: "table-actions"
    }
  ];

  personByAccountUri :{
    [id: string]: PersonDTO;
  } =  {}

  groupDetailsByAccountUri :{
    [id: string]: NamedResourceDTO[];
  } =  {}

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  private filter: any = "";
  @Ref("tableRef") readonly tableRef!: any;
  currentURI = null;
  groupDetails = [];

  created() {
    let query: any = this.$route.query;
    if (query.filter) {
      this.filter = decodeURIComponent(query.filter);
    }
    this.service = this.$opensilex.getService("opensilex.SecurityService");
  }

  updateFilter() {
    this.$opensilex.updateURLParameter("filter", this.filter, "");
    this.refresh();
  }

  refresh() {
    this.tableRef.refresh();
  }

  async searchAccounts(options) {
    let accountsResponse = await this.service.searchAccounts(
        this.filter,
        options.orderBy,
        options.currentPage,
        options.pageSize
    )

    let key_personUri_value_accountUri : {[id: string]: string} = {}

    accountsResponse.response.result.forEach( account => {
      this.personByAccountUri[account.uri] = null
      if (account.linked_person) {
        key_personUri_value_accountUri[account.linked_person] = account.uri
      }
    });

    await this.mapPersonsWithAccount(key_personUri_value_accountUri)

    return accountsResponse
  }

  deleteAccount(uri: string) {
    this.$opensilex.showLoader()
    this.service.deleteAccount(uri)
        .then( () => this.refresh() )
        .catch( (error) => this.$opensilex.errorHandler(error))
        .finally( () => this.$opensilex.hideLoader())
  }

  async mapPersonsWithAccount(key_personUri_value_accountUri : {[id: string]: string}){
    if ( Object.keys(key_personUri_value_accountUri).length !== 0 ) {
      let personsResponse = await this.service.getPersonsByURI(Object.keys(key_personUri_value_accountUri))
      personsResponse.response.result.forEach(person => {
        let accountUri = key_personUri_value_accountUri[this.$opensilex.getLongUri(person.uri)]
        this.personByAccountUri[accountUri] = person
      })
    }
  }

  changeEnable(dto: AccountUpdateDTO) {
    dto.enable = !dto.enable;
    this.service
        .updateAccount(dto)
        .catch(this.$opensilex.errorHandler);
  }

  displayEnableButton(accountRow) {
    let isUserConnected = accountRow.email === this.user.email
    return this.user.hasCredential(this.credentials.CREDENTIAL_ACCOUNT_MODIFICATION_ID)
        && !accountRow.admin
        && !isUserConnected
  }

  async showUsersGroups(data: SlotDetails<AccountGetDTO>) {

    let accountUri :string = data.item.uri

    if ( !this.groupDetailsByAccountUri[accountUri] ) {
      let groupResponse :HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>> = await this.service.getUserGroups(accountUri)
      let groups :NamedResourceDTO[] = groupResponse.response.result
      this.groupDetailsByAccountUri[accountUri] = groups
    }

    data.toggleDetails()
  }

}

</script>

<style scoped lang="scss">

/* without this rules the checkbox is not center on the height of the div*/
.checkEnable {
  display: flex;
  align-items: center;
}
</style>
