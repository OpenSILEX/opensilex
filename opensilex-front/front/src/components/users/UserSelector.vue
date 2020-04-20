<template>
  <multiselect
    v-model="selectedUserObjects"
    id="ajax"
    label="name"
    track-by="uri"
    :placeholder="$t('component.user.filter-placeholder')"
    open-direction="bottom"
    :options="users"
    :multiple="true"
    :searchable="true"
    :loading="isLoading"
    :internal-search="false"
    :options-limit="300"
    :max-height="600"
    :hide-selected="true"
    @search-change="asyncFind"
    @input="updateURIList"
  >
    <span slot="noResult">{{$t('component.user.filter-search-no-result')}}</span>
  </multiselect>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { SecurityService, UserGetDTO } from "opensilex-security/index";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";

@Component
export default class UserSelector extends Vue {
  $opensilex: any;
  service: SecurityService;

  selectedUserObjects = [];

  selectUsers(values) {
    this.selectedUserObjects = [];
    if (values && values.length > 0) {
      this.service
        .getUsersByURI(values)
        .then((http: HttpResponse<OpenSilexResponse<Array<UserGetDTO>>>) => {
          http.response.result.forEach(userDTO => {
            this.selectedUserObjects.push(this.getUserSearchModel(userDTO));
          });
        })
        .catch(this.$opensilex.errorHandler);
    }
  }

  users = [];
  isLoading = false;

  created() {
    this.service = this.$opensilex.getService("opensilex.SecurityService");
  }

  asyncFind(query) {
    this.$opensilex.disableLoader();
    this.service
      .searchUsers(query)
      .then((http: HttpResponse<OpenSilexResponse<Array<UserGetDTO>>>) => {
        let newUsers = [];
        http.response.result.forEach(userDTO => {
          newUsers.push(this.getUserSearchModel(userDTO));
        });
        this.users = newUsers;
        this.$opensilex.enableLoader();
      })
      .catch(this.$opensilex.errorHandler);
  }

  getUserSearchModel(dto: UserGetDTO) {
    let userLabel = dto.firstName + " " + dto.lastName + " <" + dto.email + ">";
    return {
      name: userLabel,
      uri: dto.uri
    };
  }

  updateURIList(values) {
    let uris = [];
    values.forEach(element => {
      uris.push(element.uri);
    });
    this.$emit("updateUsers", uris);
  }
}
</script>

<style scoped lang="scss">
</style>
