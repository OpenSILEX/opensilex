<template>
    <opensilex-SelectForm
        :label="label"
        :selected.sync="dataTypeURI"
        :options="datatypesNodes"
        :itemLoadingMethod="loadDataType"
        placeholder="VariableForm.datatype-placeholder"
    ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import {VariableDatatypeDTO} from "opensilex-core/model/variableDatatypeDTO";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";

@Component
export default class VariableDataTypeSelector extends Vue {
    $opensilex: any;
    $store: any;
    
    @PropSync("datatype")
    dataTypeURI;

    @Prop()
    label;

    @Prop()
    placeholder;

    datatypes: Array<VariableDatatypeDTO> = [];
    datatypesNodes: Array<any> = [];

    mounted() {
        this.$store.watch(
            () => this.$store.getters.language,
            () => this.loadDatatypes()
        );
    }

    created(){
        this.loadDatatypes();
    }

    loadDatatypes(){

        if(this.datatypes.length == 0){
            this.$opensilex.getService("opensilex.VariablesService")
            .getDatatypes().then((http: HttpResponse<OpenSilexResponse<Array<VariableDatatypeDTO>>>) => {
                this.datatypes = http.response.result;
                this.updateDatatypeNodes();
            });
        }else{
            this.updateDatatypeNodes();
        }
    }

    updateDatatypeNodes(){
        this.datatypesNodes = [];
        for (let dto of this.datatypes) {
            let label: any = this.$t(dto.name);
            this.datatypesNodes.push({
                id: dto.uri,
                label: label.charAt(0).toUpperCase() + label.slice(1)
            });
        }
    }

    loadDataType(dataTypeUri: string){
        if(! dataTypeUri){
            return undefined;
        }
        let dataType = this.datatypesNodes.find(datatypeNode => datatypeNode.id == dataTypeUri);
        return [dataType];
    }
}
</script>