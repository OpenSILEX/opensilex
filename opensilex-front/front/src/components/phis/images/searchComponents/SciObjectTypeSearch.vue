<template>
  <div>
    <b-form-group
      id="input-group-3"
      label="Scientific Object Type:"
      label-for="sciObjectTypeSearch"
      label-class="required"
    >
      <b-form-select
        id="sciObjectTypeSearch"
        v-model="type"
        :options="soTypes"
        @input="update"
        @focus.native="userAction"
      >
        <template v-slot:first>
          <b-form-select-option :value="null">--no object type selected--</b-form-select-option>
        </template>
      </b-form-select>
    </b-form-group>
  </div>
</template>


<script lang="ts">
// import { Component } from "vue-property-decorator";
// import Vue from "vue";
// import { EventBus } from "./../event-bus";
// import HttpResponse, { OpenSilexResponse } from "opensilex-phis/HttpResponse";

// @Component
// export default class SciObjectTypeSearch extends Vue {
//   $opensilex: any;
//   $store: any;
//   get user() {
//     return this.$store.state.user;
//   }

//   // the way to see if it is select by a user or by the change of an other element of the form
//   // In this case the images will be asynch load for nothing
//   userFocus = false;

//   soTypes: any = [];
//   type: string = null;
//   allTypes: any = [];
//   scientificObjectService;

//   sciObjectUri: string =
//     "http://www.opensilex.org/vocabulary/oeso#ScientificObject";

//   update() {
//     if (this.userFocus) {
//       // i don't want the spread
//       EventBus.$emit("soTypeHasChanged", this.type);
//     } else {
//       EventBus.$emit("soTypeIsInitialized");
//     }
//   }
//   userAction() {
//     this.userFocus = true;
//   }

//   created() {
//     this.scientificObjectService = this.$opensilex.getService(
//       "opensilex.ScientificObjectsService"
//     );
//     let service = this.$opensilex.getService(
//       "opensilex.UriService"
//     );
//     const result = service
//       .getDescendants(
//         this.sciObjectUri,
//         100,
//         0
//       )
//       .then((http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
//         const res = http.response.result as any;
//         res.data.forEach(element => {
//           this.soTypes.push({
//             value: element.uri,
//             text: element.uri.split("#")[1]
//           });
//         });
//         this.allTypes = this.soTypes;
//       })
//       .catch(this.$opensilex.errorHandler);

//     // EventBus.$on("imageTypeSelected", type => {
//     //   this.userFocus = false;
//     //   this.soTypes = this.allTypes;
//     //   this.type = null;
//     // });

//     EventBus.$on("experienceHasChanged", experience => {
//       this.userFocus = false;
//       if (experience === null) {
//         this.soTypes = this.allTypes;
//         this.type = null;
//       } else {
//         this.findTypeByExperimentObjects(experience);
//         this.type = null;
//       }
//     });
//   }

//   findTypeByExperimentObjects(experience) {
//     const result = this.scientificObjectService
//       .getScientificObjectsBySearch(
//         8000,
//         0,
//         undefined,
//         experience,
//         undefined,
//         undefined
//       )
//       .then(
//         (http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
//           const res = http.response.result as any;
//           const data = res.data;
//           let uriTypes = [];
//           data.forEach(element => {
//             uriTypes.push(element.rdfType);
//           });
//           uriTypes = [...new Set(uriTypes)];
//           const types = [];
//           uriTypes.forEach(element => {
//             types.push({
//               value: element,
//               text: element.split("#")[1]
//             });
//           });
//           this.soTypes = types;
//         }
//       )
//       .catch(error => {
//         console.log(error);
//       });
//   }
// }
</script>

<style scoped >
</style>
