<template>
    <ValidationObserver ref="entityValidatorRef">

      <div class="row">
        <div class="col-lg-12">

          <opensilex-ExternalReferencesForm
            :references.sync="form"
            :displayInsertButton="false"
            :includeAgroportalSearch="true"
            :ontologiesToSelect="selectedOntologies">
          </opensilex-ExternalReferencesForm>

        </div>
      </div>

    </ValidationObserver>
</template>

<script lang="ts">

import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import {EntityCreationDTO} from "opensilex-core/index";
import {EntityAgroportalDTO} from "opensilex-core/model/entityAgroportalDTO";
import AgroportalEntityCreate from "./AgroportalEntityCreate.vue";


@Component
export default class AgroportalEntityExternalReferencesForm extends Vue {

  selectedOntologies: string[] = AgroportalEntityCreate.selectedOntologies;

  @PropSync("form")
  entityDto: EntityCreationDTO;

}
</script>

<style scoped>

</style>