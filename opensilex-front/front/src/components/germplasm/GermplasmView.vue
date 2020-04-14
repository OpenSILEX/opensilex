<template>
  <div>
    <b-button @click="showCreateForm" variant="success">{{$t('component.germplasm.add')}}</b-button>
    <opensilex-GermplasmForm
      ref="germplasmForm"
      @onCreate="callCreateGermplasmService"
    ></opensilex-GermplasmForm>
    <opensilex-GermplasmList 
      ref="germplasmList" >
    </opensilex-GermplasmList>

  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

import { GermplasmService, GermplasmCreationDTO } from "opensilex-core/index"

@Component
export default class GermplasmView extends Vue {
  $opensilex: any;
  $store: any;
  service: GermplasmService;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    console.debug("Loading form view...");
    this.service = this.$opensilex.getService("opensilex.GermplasmService");
  }

  showCreateForm() {
    let germplasmForm: any = this.$refs.germplasmForm;
    germplasmForm.showCreateForm();
  }

  callCreateGermplasmService(form: GermplasmCreationDTO, done) {
    done(
      this.service
        .createGermplasm(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("germplasm created", uri);
          let germplasmList: any = this.$refs.germplasmList;
          germplasmList.refresh();
        })
    );
  }

}
</script>

<style scoped lang="scss">
</style>

