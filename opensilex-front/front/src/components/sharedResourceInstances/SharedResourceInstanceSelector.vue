<template>
  <opensilex-FormSelector
      :label="label"
      :selected.sync="selectedSriId"
      :multiple="false"
      :optionsLoadingMethod="loadSharedResourceInstances"
      placeholder="component.sharedResourceInstances.selector-placeholder"
      @clear="$emit('clear')"
      @select="select"
      @deselect="deselect"
  ></opensilex-FormSelector>
</template>

<script lang="ts">
import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {SharedResourceInstanceDTO} from "opensilex-core/model/sharedResourceInstanceDTO";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OntologyService} from "opensilex-core/api/ontology.service";

export const LOCAL_INSTANCE_ID = "local_instance";

@Component
export default class SharedResourceInstanceSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;

  /**
   * Represents the URI of the selected SRI.
   */
  @PropSync("sharedResourceInstance", {
    default: undefined
  })
  selectedSriUri: string;

  @Prop()
  label: string;

  sharedResourceInstanceDTOs: Array<SharedResourceInstanceDTO>;

  get selectedSriId(): string {
    if (this.selectedSriUri === undefined) {
      return LOCAL_INSTANCE_ID;
    }
    return this.selectedSriUri;
  }

  // This property may be marked as unused on some IDEs, but it is actually used as the PropSync "selected" for the
  // SelectForm needs both a getter and a setter. Vue consider a getter & setter with the same name as a unique computed
  // prop, but the IDE treat them as separate symbols.
  // noinspection JSUnusedGlobalSymbols
  set selectedSriId(newId: string) {
    if (newId === LOCAL_INSTANCE_ID) {
      this.selectedSriUri = undefined;
    } else {
      this.selectedSriUri = newId;
    }
  }

  loadSharedResourceInstances() {
    return this.$opensilex
        .getService<OntologyService>("opensilex.OntologyService")
        .getSharedResourceInstances()
        .then(
            (http: HttpResponse<OpenSilexResponse<Array<SharedResourceInstanceDTO>>>) => {
              this.sharedResourceInstanceDTOs = http.response.result;

              let options = this.sharedResourceInstanceDTOs.map(dto => {
                return {
                  id: dto.uri,
                  label: dto.label
                };
              });
              options.push({
                id: LOCAL_INSTANCE_ID,
                label: this.$t("component.sharedResourceInstances.local-instance").toString()
              });

              return options;
            }
        );
  }

  select(value) {
    for (let resourceDTO of this.sharedResourceInstanceDTOs) {
      if (resourceDTO.uri === value.id) {
        this.$emit("select", resourceDTO);
        return;
      }
    }
    this.$emit("select", undefined);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }
}
</script>

<style scoped lang="scss">
</style>
