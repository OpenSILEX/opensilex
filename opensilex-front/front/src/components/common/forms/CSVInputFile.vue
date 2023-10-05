<template>
  <div>
    <b-button @click="importCsv" class="mb-2 mr-2" variant="outline-success">{{
      $t(buttonLabel)
    }}</b-button>
    <slot name="error">
      <span v-if="errors" class="error-message alert alert-danger mb-2 mr-2">{{
        this.errors[0]
      }}</span>
    </slot>
    <b-form-file
      ref="inputFile"
      accept="text/csv, .csv"
      @input="fileUpdated"
      hidden
      style="display: none"
    ></b-form-file>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref, PropSync } from "vue-property-decorator";
import Vue from "vue";
import * as CSV from "csv-string";

@Component
export default class CSVInputFile extends Vue {
  $opensilex: any;
  $papa: any;
  $store: any;

  @PropSync("csvData")
  data: any[];

  @Prop()
  headersExactMatch: string[];

  @Prop()
  headersPresent: string[];

  /**
   * To check some headers don't show up more that once.
   * Only gets checked if we are returning an array of arrays in data.
   */
  @Prop()
  duplicatableHeaders: string[];

  /**
   * Data's first array will be headers, allows for duplicated headers but this changes the output format.
   */
  @Prop({ default: false })
  returnDataAsArrayOfArrays: boolean;

  @Prop()
  config: any;

  errors: any = [];

  @Prop({
    default: "component.common.import-files.csv-file",
  })
  buttonLabel: string;

  @Ref("inputFile") readonly inputFile!: any;

  importCsv() {
    this.inputFile.$el.childNodes[0].click();
  }
  fileUpdated(file): any {
    this.$opensilex.showLoader();
    this.$nextTick(() => {
      this.readUploadedFileAsText(file).then((text) => {
        console.debug("Input file text", text);
        let delimiter =  CSV.detect(text.toString());

        //Parsing with header set to false with make result.data be an Array of Arrays instead of an Array of jsons
        //The first array contains the headers, this allows us to keep track of duplicated columns.
        let result = this.$papa.parse(text, {
          header: !this.returnDataAsArrayOfArrays,
          delimiter: delimiter,
        });
        console.debug("result.data", result.data);
        console.debug("result.errors", result.errors);
        console.debug("result.meta", result.meta);

        this.errors = [];
        if (result.data == null || result.data.length == 0) {
          this.errors.push(
            "Unable to parse csv, delimiter used : '" + delimiter + "'"
          );
        } else {
          let objectToCheck : Array<string> = (this.returnDataAsArrayOfArrays ? result.data[0] : Object.keys(result.data[0]));

          //Check non duplicatable headers if we are returning array of arrays
          if(this.returnDataAsArrayOfArrays){
            let visitedHeaders = [];
            for(let header of objectToCheck){
              if(visitedHeaders.includes(header)){
                if(!this.duplicatableHeaders.includes(header)){
                  this.errors.push(
                      "This header can't be duplicated: " +
                      header
                  );
                  break;
                }
              }else{
                visitedHeaders.push(header);
              }
            }
          }
          //Check that some headers are present
          if(this.headersPresent && !CSVInputFile.containsAll(objectToCheck, this.headersPresent)){
            this.errors.push(
                "Some of these headers are missing: [" +
                this.headersPresent + "]"
            );
          }
          //Check the headers have exactly the same quantity and content as headersToCheck
          if (this.headersExactMatch != null && this.headersExactMatch.length > 0) {
            if (
              !CSVInputFile.equalArrays(
                objectToCheck,
                this.headersExactMatch
              )
            ) {
              this.errors.push(
                "Bad data : [" +
                  this.headersExactMatch.toString() +
                  "] is expected. [" +
                  objectToCheck +
                  "] has been found."
              );
            }
          }
        }

        //Emit if no errors
        if (this.errors.length == 0) {
          this.data = result.data;
          this.$emit("updated", result.data);
        }
        this.$opensilex.hideLoader();
      });
    });
  }

  static containsAll(container, headersToContain){
    return headersToContain.every((header) => container.includes(header));
  }

  static equalArrays(arr1, arr2): boolean {
    return CSVInputFile.containsAll(arr1, arr2) && CSVInputFile.containsAll(arr2, arr1);
  }

  async readUploadedFileAsText(inputFile) {
    console.debug("inputFile", inputFile);
    const temporaryFileReader = new FileReader();

    return new Promise((resolve, reject) => {
      temporaryFileReader.onerror = () => {
        temporaryFileReader.abort();
        reject(new DOMException("Problem parsing input file."));
      };

      temporaryFileReader.onload = () => {
        resolve(temporaryFileReader.result);
      };
      temporaryFileReader.readAsText(inputFile);
    });
  }
}
</script>

<style scoped lang="scss">
</style>
