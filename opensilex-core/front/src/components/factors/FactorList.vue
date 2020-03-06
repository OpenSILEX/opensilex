<template>
  <div>
    <b-input-group class="mt-3 mb-3" size="sm">
      <b-input-group>
        <label for="factorAlias" class="col-sm-1 col-form-label">Factor alias</label>
        <b-form-input v-model="aliasPattern" debounce="300" placeholder="Irrigation"  id="factorAlias"></b-form-input>
        <template v-slot:append>
          <b-btn :disabled="!aliasPattern" variant="primary" @click="aliasPattern = ''">
            <font-awesome-icon icon="times" size="sm" />
          </b-btn>
        </template>
      </b-input-group>
    </b-input-group>
    <b-table
      ref="tableRef"
      striped
      hover
      small
      :items="loadData"
      :fields="fields"
      :sort-by.sync="sortBy"
      :sort-desc.sync="sortDesc"
      no-provider-paging
    >
      <template v-slot:cell(alias)="data">
        {{ data.item.alias }}
      </template>

      <template v-slot:cell(uri)="data">
        <a class="uri-info">
          <small>{{ data.item.uri }}</small>
        </a>
      </template>
  
      <template v-slot:cell(comment)="data">
        <a class="comment-info">
          <small>{{ data.item.comment }}</small>
        </a>
      </template>
 
    </b-table>
    <b-pagination
      v-model="currentPage"
      :total-rows="totalRow"
      :per-page="pageSize"
      @change="refresh()"
    ></b-pagination>
  </div>
</template>

<script lang="ts">
// import { Component } from "vue-property-decorator";
// import Vue from "vue";
// import { FactorsService } from "../../lib/api/factors.service";
// import HttpResponse, { OpenSilexResponse } from "../../lib//HttpResponse";
// import { FactorGetDTO } from "../../lib//model/factorGetDTO";
// import VueRouter from "vue-router";

// @Component
// export default class FactorList extends Vue {
//   $opensilex: any;
//   $store: any;
//   $router: VueRouter;

//   get user() {
//     return this.$store.state.user;
//   }

//   currentPage: number = 1;
//   pageSize = 20;
//   totalRow = 0;
//   sortBy = "alias";
//   sortDesc = false;

//   private alias: any = "";
//   set aliasPattern(value: string) {
//     console.log(value)
//     this.alias = value;
//     let tableRef: any = this.$refs.tableRef;
//     tableRef.refresh();
//   }

//   get aliasPattern() {
//     return this.alias;
//   }

//   created() {
//     let query: any = this.$route.query;
//     if (query.aliasPattern) {
//       this.aliasPattern  = decodeURI(query.aliasPattern);
//     }
//     if (query.pageSize) {
//       this.pageSize = parseInt(query.pageSize);
//     }
//     if (query.currentPage) {
//       this.currentPage = parseInt(query.currentPage);
//     }
//     if (query.sortBy) {
//       this.sortBy = decodeURI(query.sortBy);
//     }
//     if (query.sortDesc) {
//       this.sortDesc = query.sortDesc == "true";
//     }
//   }

//   fields = [
//     {
//       key: "alias",
//       sortable: true
//     }, 
//     {
//       key: "uri",
//       sortable: true
//     },
//     {
//       key: "comment",
//       sortable: true
//     },
//     {
//       key: "actions"
//     }
//   ];

//   refresh() {
//     let tableRef: any = this.$refs.tableRef;
//     tableRef.refresh();
//   }

//   loadData() {
//     let service: FactorsService = this.$opensilex.getService(
//       "opensilex.FactorsService"
//     );

//     let orderBy = [];
//     if (this.sortBy) {
//       let orderByText = this.sortBy + "=";
//       if (this.sortDesc) {
//         orderBy.push(orderByText + "desc");
//       } else {
//         orderBy.push(orderByText + "asc");
//       }
//     }

//     return service
//       .searchFactors(
//         this.user.getAuthorizationHeader(),
//         this.aliasPattern,
//         orderBy,
//         this.currentPage - 1,
//         this.pageSize
//       )
//       .then((http: HttpResponse<OpenSilexResponse<Array<FactorGetDTO>>>) => {
//         console.log( http.response);
//         this.totalRow = http.response.metadata.pagination.totalCount;
//         this.pageSize = http.response.metadata.pagination.pageSize;
//         setTimeout(() => {
//           this.currentPage = http.response.metadata.pagination.currentPage + 1;
//         }, 0);

//         this.$router
//           .push({
//             path: this.$route.fullPath,
//             query: {
//               aliasPattern: encodeURI(this.aliasPattern),
//               sortBy: encodeURI(this.sortBy),
//               sortDesc: "" + this.sortDesc,
//               currentPage: "" + this.currentPage,
//               pageSize: "" + this.pageSize
//             }
//           })
//           .catch(function() {});

//         return http.response.result;
//       })
//       .catch(this.$opensilex.errorHandler);
//   }
// }
</script>

<style scoped lang="scss">
.uri-info {
  text-overflow: ellipsis;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
  max-width: 300px;
}
</style>
