<template>
    <div>
        <template>
            <div class="refineSearchMessage" @click="refine($event)">
                <i class="more-results-info">
                    {{
                        $t("TreeselectRefineSearchMessage.refineSearchMessage", {resultCount, totalCount})
                    }}
            
                </i>
            </div>
        </template>
    </div>
</template>


<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class CustomTreeselectRefineSearchMessage extends Vue {
    $opensilex: any;

    @Prop()
    resultCount: number;

    @Prop()
    totalCount: number;

    refine($event){
        this.$emit("loadMoreItems")
    }
}
</script>

<style lang="scss" scoped>
.refineSearchMessage {
  font-weight: bold;
  background-color: #00A28C;
  color: #FFFFFF ;
  cursor:pointer;
}

i.more-results-info {
  margin-left: 10px;
  margin-right: 10px;
}
</style>

<i18n>
fr:
    TreeselectRefineSearchMessage:
        refineSearchMessage: "{resultCount} / {totalCount} résultats affichés, merci de préciser votre recherche ou de cliquer ICI pour afficher tous les résultats"

en: 
    TreeselectRefineSearchMessage:
        refineSearchMessage: "{resultCount} / {totalCount} results displayed, please refine your search or click HERE to display all results"
</i18n>