<template>
    <ValidationObserver ref="validatorRef">

        <div class="row">

            <div class="col-lg-12">

              <b-form-group
                  label="component.skos.ontologies-references-label"
                  label-size="lg"
                  label-class="font-weight-bold pt-0"
                  class="mb-0"
              >
                <template v-slot:label>{{ $t('AgroportalEntityForm.search-for-ontology-term') }}</template>
              </b-form-group>

              <!-- Name -->
              <opensilex-AgroportalSearch
                  label="component.common.name"
                  type="text"
                  placeholder="search"
                  @change="onSearchTextChange"
              ></opensilex-AgroportalSearch>

              <div class="row">
                <opensilex-AgroportalResults
                    ref="searchResults"
                    :text.sync="text"
                    @import="importResult">
                </opensilex-AgroportalResults>
              </div>
            </div>

            <div class="col">

            </div>

        </div>
    </ValidationObserver>
</template>

<script lang="ts">
    import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import {ExternalOntologies} from "../../../models/ExternalOntologies";
import EntityCreate from "./AgroportalEntityCreate.vue";
// @ts-ignore
import { EntityCreationDTO } from "opensilex-core/index";
import AgroportalEntityCreate from "./AgroportalEntityCreate.vue";
    import {EntityAgroportalDTO} from "opensilex-core/model/entityAgroportalDTO";

@Component
export default class AgroportalEntityForm extends Vue {
    $opensilex: any;

    title = "";
    uriGenerated = true;

    text = "";

    @Prop()
    editMode;

    errorMsg: String = "";

    @PropSync("form")
    entityDto: EntityAgroportalDTO;

    externalOntologiesRefs: any[] = ExternalOntologies.getExternalOntologiesReferences(EntityCreate.selectedOntologies);

    handleErrorMessage(errorMsg: string) {
        this.errorMsg = errorMsg;
    }

    @Ref("validatorRef") readonly validatorRef!: any;

    onSearchTextChange(searchedText: string) {
      this.text = searchedText;
    }

    importResult(entity: EntityAgroportalDTO) {
      this.entityDto.name = entity.name;
      this.entityDto.definitions = entity.definitions;
    }

    reset() {
        this.uriGenerated = true;
        return this.validatorRef.reset();
    }

    validate() {
        return this.validatorRef.validate();
    }
}
</script>

<style scoped lang="scss">
    a {color: #007bff;}
</style>
