<template>
    <div>
        <opensilex-PageContent>
            <template>
                <opensilex-TableAsyncView
                    ref="tableRef"
                    :searchMethod="searchVariablesWithAttribute"
                    :fields="fields"
                    defaultSortBy="name"
                    :isSelectable="isSelectable"
                    :maximumSelectedRows="maximumSelectedRows"
                    labelNumberOfSelectedRow="VariableList.selected"
                    :iconNumberOfSelectedRow="iconNumberOfSelectedRow"
                    :defaultPageSize="pageSize">

                    <template v-slot:cell(name)="{data}">
                        <opensilex-UriLink
                            :uri="data.item.uri"
                            :value="data.item.name"
                            :url="'/variable/details/'+ encodeURIComponent(data.item.uri)"
                        ></opensilex-UriLink>
                    </template>
                    <template v-slot:row-details="{data}">
                        <div v-if="variableGroupsList[data.item.uri] && variableGroupsList[data.item.uri].length > 0">
                            <div>{{ $t("VariableList.variablesGroup") }}:</div>
                            <ul>
                                <li v-for="(vg, index) in variableGroupsList[data.item.uri]" :key="index">{{vg.name}}
                                </li>
                            </ul>
                        </div>
                        <div v-else> {{ $t("VariableList.not-used-in-variablesGroup") }}</div>
                    </template>
                    <template v-slot:cell(_entity_name)="{data}">{{ data.item.entity.name }}</template>
                    <template v-slot:cell(_interest_entity_name)="{data}">
                        {{ data.item.entity_of_interest ? data.item.entity_of_interest.name : "" }}
                    </template>
                    <template v-slot:cell(_characteristic_name)="{data}">{{ data.item.characteristic.name }}</template>
                    <template v-slot:cell(_method_name)="{data}">{{ data.item.method.name }}</template>
                    <template v-slot:cell(_unit_name)="{data}">{{data.item.unit.name }}</template>
                </opensilex-TableAsyncView>

            </template>
        </opensilex-PageContent>

    </div>
</template>

<script lang="ts">
import {Vue, Component, Prop} from "vue-property-decorator";
import VariableList from "./VariableList.vue";

@Component
export default class VariableListWithoutFilter extends VariableList {

}
</script>

<style scoped lang="scss">
</style>