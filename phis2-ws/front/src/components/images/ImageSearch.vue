<template>
  <div>
    <b-form>
      <phis2ws-TimeSearch></phis2ws-TimeSearch>
      <div class="row">
        <div class="col-sm">
          <phis2ws-ExperimentSearch></phis2ws-ExperimentSearch>
          <phis2ws-SciObjectTypeSearch></phis2ws-SciObjectTypeSearch>
          <phis2ws-SciObjectAliasSearch></phis2ws-SciObjectAliasSearch>
          <phis2ws-SciObjectURISearch></phis2ws-SciObjectURISearch>
        </div>
        <div class="col-sm">
          <phis2ws-ImageTypeSearch></phis2ws-ImageTypeSearch>
        </div>
      </div>
    </b-form>
  </div>
</template>


<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { ScientificObjectsService } from "../../lib/api/scientificObjects.service";
import { ScientificObjectDTO } from "../../lib/model/scientificObjectDTO";
import { EventBus } from "./event-bus";

@Component
export default class ImageSearch extends Vue {
  $opensilex: any;
  $store: any;
  get user() {
    return this.$store.state.user;
  }

  aliasObjectList = [];
  uriObjectList = [];

  form: any = {
    rdfType: null,
    startDate: undefined,
    endDate: undefined,
    objectList: [],
    objectType: null,
    experiment: null
  };

  getObjectList() {
    if (this.form.experiment === null && this.form.objectType === null) {
      this.form.objectList = [];
      this.$emit("onSearchFormSubmit", this.form);
    } else {
      let service: ScientificObjectsService = this.$opensilex.getService(
        "opensilex.ScientificObjectsService"
      );

      //the fields of rest service must be undefined to be ignored
      let selectedExperimentField: string;
      let selectedSoTypeField: string;
      if (this.form.objectType === null) {
        selectedSoTypeField = undefined;
      } else {
        selectedSoTypeField = this.form.objectType;
      }
      if (this.form.experiment === null) {
        selectedExperimentField = undefined;
      } else {
        selectedExperimentField = this.form.experiment;
      }

      const result = service
        .getScientificObjectsBySearch(
          this.user.getAuthorizationHeader(),
          8000,
          0,
          undefined,
          selectedExperimentField,
          undefined,
          selectedSoTypeField
        )
        .then(
          (
            http: HttpResponse<OpenSilexResponse<Array<ScientificObjectDTO>>>
          ) => {
            const res = http.response.result as any;
            const data = res.data;
            this.form.objectList = [];
            console.log("data ?");
            console.log(data);
            data.forEach(element => {
              this.form.objectList.push(element.uri);
            });
            this.$emit("onSearchFormSubmit", this.form);
          }
        )
        .catch(error => {
          console.log(error);
          this.form.objectList = [];
          this.$emit("onSearchFormSubmit", this.form);
        });
    }
  }

  created() {
    EventBus.$on("experienceHasChanged", experience => {
      console.log("experienceHasChanged" + ": Event In ImageSearch");
      this.form.objectType = null;
      this.form.experiment = experience;
      this.form.objectList = [];
      this.aliasObjectList = [];
      this.uriObjectList = [];
      if (experience === null) {
        this.$emit("onSearchFormSubmit", this.form);
      } else {
        this.getObjectList();
      }
    });

    EventBus.$on("soTypeHasChanged", type => {
      console.log("soTypeHasChanged" + ": Event In ImageSearch");
      this.form.objectType = type;
      this.form.objectList = [];
      this.aliasObjectList = [];
      this.uriObjectList = [];
      this.getObjectList();
    });

    EventBus.$on("soTypeIsInitialized", () => {
      console.log("soTypeIsInitialized" + ": Event In ImageSearch");

      this.form.objectType = null;
    });

    EventBus.$on("imageTypeSelected", type => {
      console.log("imageTypeSelected" + ": Event In ImageSearch");
      // this.form.objectList = [];
      // this.aliasObjectList = [];
      // this.uriObjectList = [];
      // this.form.objectType = null;
      this.form.rdfType = type;
      this.$emit("onSearchFormSubmit", this.form);
    });

    EventBus.$on("startDateHasChanged", startDate => {
      console.log("startDateHasChanged" + ": Event In ImageSearch");
      this.form.startDate = startDate;
      this.$emit("onSearchFormSubmit", this.form);
    });

    EventBus.$on("endDateHasChanged", endDate => {
      console.log("endDateHasChanged" + ": Event In ImageSearch");
      this.form.endDate = endDate;
      this.$emit("onSearchFormSubmit", this.form);
    });

    EventBus.$on("aliasObjectSelected", sciObjects => {
      this.aliasObjectList = sciObjects;
      if (
        this.aliasObjectList.length === 0 && this.uriObjectList.length === 0
      ) {
        this.getObjectList();
      } else {
        if (this.uriObjectList.length !== 0) {
          this.form.objectList = this.aliasObjectList.concat(this.uriObjectList);
        } else {
          this.form.objectList = this.aliasObjectList;
        }

        this.$emit("onSearchFormSubmit", this.form);
      }
    });

    EventBus.$on("URIObjectSelected", sciObjects => {
      this.uriObjectList = sciObjects;
      if ( this.aliasObjectList.length === 0 && this.uriObjectList.length === 0
      ) {
        this.getObjectList();
      } else {
        if (this.aliasObjectList.length !== 0) {
          this.form.objectList = this.aliasObjectList.concat(this.uriObjectList);
        } else {
          this.form.objectList = this.uriObjectList;
        }
         this.$emit("onSearchFormSubmit", this.form);
      }
     
    });
  }
}
</script>

<style scoped lang="scss">
.btn-phis {
  background-color: #00a38d;
  border: 1px solid #00a38d;
  color: #ffffff !important;
}
.btn-phis:hover,
.btn-phis:focus,
.btn-phis.active {
  background-color: #00a38d;
  border: 1px solid #00a38d;
  color: #ffffff !important;
}
.btn-phis:focus {
  outline: 0;
  -webkit-box-shadow: none;
  box-shadow: none;
}
</style>
