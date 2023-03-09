<template>
  <opensilex-Card
      class="stats-card"
      :no-footer="true"
      :no-header="true"
      icon="fa#sort-numeric-down"
      label="DataMonitoring.title"
  >

    <template v-slot:body>
      <div class="globalStatsContainer">

        <!-- show component icon and title -->
        <span class="title">
          {{ $t("DataMonitoring.title") }}
        </span>

        <!-- button group to select time period for which entity counts should be shown -->
        <span class="button-group">
             <div class="btn-group btn-group-toggle btnsGroup" data-toggle="buttons" :options="periods">
              <!-- day -->
              <label class="btn periodBtn btn-toggle greenThemeColor"
                     :class="{
                  active: period === 'day'
                }"
              >
                <input
                    type="radio"
                    name="options"
                    id="option1"
                    value="day"
                    checked
                    v-model=period
                >
                {{ $t('DataMonitoring.day') }}
              </label>

               <!-- week -->
              <label class="btn periodBtn btn-toggle greenThemeColor"
                     :class="{
                  active: period === 'week'
                }"
              >
                <input
                    type="radio"
                    name="options"
                    id="option2"
                    value="week"
                    v-model=period
                >
                {{ $t('DataMonitoring.week') }}
              </label>

               <!-- month -->
              <label class="btn periodBtn btn-toggle greenThemeColor"
                     :class="{
                  active: period === 'month'
                }"
              >
                <input
                    type="radio"
                    name="options"
                    id="option3"
                    value="month"
                    v-model=period
                >
                {{ $t('DataMonitoring.month') }}
              </label>

               <!-- year -->
              <label class="btn periodBtn btn-toggle greenThemeColor"
                     :class="{
                  active: period === 'year'
                }"
              >
                <input
                    type="radio"
                    name="options"
                    id="option4"
                    value="year"
                    v-model=period
                >
                {{ $t('DataMonitoring.year') }}
              </label>
          </div>
        </span>
        
        <!-- show entity counts for a specific time period (added since last day or week) -->
        <span class="stats-values">
           <!-- show experiment count -->
           <span id="popover-experiments" class="expe">
            <opensilex-Icon icon="ik#ik-layers"/>
            <span>{{ nbExperiments }} ({{ deltaExperiments }}) {{ $t("component.menu.experiments") }}</span>
           </span>

          <!-- show scientific object count -->
           <span v-if="deltaScientificObjects > 0" id="popover-so" class="so">
             <opensilex-Icon icon="ik#ik-target"/>
              <span class="stats-underline">{{ nbScientificObjects }} ({{ deltaScientificObjects }}) {{ $t("component.menu.scientificObjects") }}</span>
             <!-- handle mouse-over events on "os" -->
               <b-popover target="popover-so" triggers="hover" placement="bottom">
                <template #title>{{ $t("DataMonitoring.scientificObjetcTypes") }}</template>
                <ul style="padding-left: 10px">
                  <li v-for="item in scientificObjetcTypes" :key="item.index">
                    {{ item }}
                  </li>
                </ul>
               </b-popover>
           </span>
           <span v-else>
             <opensilex-Icon icon="ik#ik-target"/><span>{{ nbScientificObjects }} ({{ deltaScientificObjects }}) {{ $t("component.menu.scientificObjects") }}</span>
           </span>

          <!-- show data count -->
           <span class="data">
             <opensilex-Icon icon="ik#ik-bar-chart"/>
             <span>{{ nbData }} ({{ deltaData }}) {{ $t("component.menu.data.label") }}</span>
           </span>
        </span>
      </div>
    </template>
  </opensilex-Card>
</template>

<script lang="ts">
import {Component, PropSync, Watch} from "vue-property-decorator";
import Vue from "vue";
import {MetricsService} from "opensilex-core/api/metrics.service";
import {MetricPeriodDTO} from "opensilex-core/model/metricPeriodDTO";
import {CountItemPeriodDTO} from "opensilex-core/model/countItemPeriodDTO";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";


@Component
export default class DataMonitoring extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: any;

  nbScientificObjects: string = "0";
  nbExperiments: string = "0";
  nbData: string = "0";
  deltaScientificObjects: string = "0";
  deltaExperiments : string =  "0";
  deltaData: string =  "0";
  lastUpdate: string;
  scientificObjetcTypes: Array<string> = [];
  page: number = 0;
  pageSize: number = 20;
  day: string = "day";
  week: string = "week";
  month: string = "month";
  year: string = "year";

  /*
   * initialize time period indicator for data counts (i.e. day, week, month or year)
   */
  period: string = this.day;
  lang: string ;

  created() {
    this.loadMetrics();
  }

  lastUpdate_date() {
    return this.lastUpdate;
  }

  get periods() {
    return [
      {text: this.$i18n.t("DataMonitoring.day"), value: this.day},
      {text: this.$i18n.t("DataMonitoring.week"), value: this.week},
      {text: this.$i18n.t("DataMonitoring.month"), value: this.month},
      {text: this.$i18n.t("DataMonitoring.year"), value: this.year},
    ];
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
        () => this.$store.getters.language,
        () => {
         this.loadMetrics();
        }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  @Watch("period")
  onPeriodChanged()
  {
    this.loadMetrics();
  }

  loadMetrics() {
    this.$opensilex.enableLoader();
    let service: MetricsService = this.$opensilex.getService("opensilex.MetricsService");
    service
            .getSystemMetricsSummary(
              this.period,
              this.page,
              this.pageSize
              )
            .then((http: HttpResponse<OpenSilexResponse<MetricPeriodDTO>>) => {
              let result: MetricPeriodDTO = http.response.result;
              this.nbScientificObjects = this.splitIntegerByThousands(result.scientific_object_list.total_items_count);
              this.deltaScientificObjects = this.getCountDeltaWthAlgebraicSign(result.scientific_object_list.total_difference_item_count);
              if(result.scientific_object_list.total_difference_item_count > 0) {
                this.scientificObjetcTypes = this.getAddedTypes(result.scientific_object_list.difference_items);
              }
              this.nbExperiments = this.splitIntegerByThousands(result.experiment_list.total_items_count);
              this.deltaExperiments = this.getCountDeltaWthAlgebraicSign(result.experiment_list.total_difference_item_count);
              this.nbData = this.splitIntegerByThousands(result.data_list.total_items_count);
              this.deltaData = this.getCountDeltaWthAlgebraicSign(result.data_list.total_difference_item_count);
            })
            .catch(this.$opensilex.errorHandler);
  }

  /**
   * add a '+' or '-' sign to the count difference of added or deleted  entities
   */
  getCountDeltaWthAlgebraicSign(count_delta : number) {
    var deltaWithSign = this.splitIntegerByThousands(Math.abs(count_delta));

    if(count_delta > 0){
      deltaWithSign = "+" + deltaWithSign;
    }
    else if(count_delta < 0) {
      deltaWithSign = "-" + deltaWithSign;
    }
    return deltaWithSign;
  }

 /**
  * list the names and counts of all Scientific Object types contained in the entry array
  */
  getAddedTypes(listItems?: Array<CountItemPeriodDTO>) {
    let addedTypes = [];
    if (Array.isArray(listItems)) {
      for (let item of listItems) {
        if (item.count > 0) {
          if (this.$store.getters.language == "fr") {
            addedTypes.push(`${item.name.toLowerCase()} : ${item.count}`);
          } else {
            addedTypes.push(`${item.name.toLowerCase()}: ${item.count}`);
          }
        }
      }
      addedTypes.sort(function (a, b) {
        return a.localeCompare(b);
      });
    }
    return addedTypes;
  }


  /**
   * adds a thousands separator (represented by 'space' character) using a regular expression to an input Integer
   * @param n integer number
   */
  splitIntegerByThousands(n : number) {
    return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, " ");
  }
}

</script>

<style scoped lang="scss">

.periodBtn {
  border-color: #018371;
  background: #fff;
  color: #018371;
}

.active {
  background-color: #00A38D;
  border-color: #00A38D;
  color: #fff;
}

.title {
  font-size: 1.5em;
  margin-right: 10px;
}

.stats-values {
  font-size: 1.6em;
  margin-left: 5%;
  margin-right: 10%;
  display:flex;
  justify-content: space-evenly;
  list-style-type: none;

  span {
    margin-left: 5px;
    margin-right: 10px;
  }
}

.stats-underline {
  text-decoration-line: underline;
  text-decoration-style: dotted;
}

.button-group {
  float: right;
}

.so, .expe {
  margin: 0 10px 0 10px;
}

@media only screen and (max-width: 1451px){
  .button-group {
    float: left;
    display: inline-flex;
  }

  .stats-values {
    float: right;
    font-size: 1.3em;
    justify-items: center;
    list-style-type: none;
    margin-left: 0%;
    margin-right: 0%;
  }

  .globalStatsContainer {
    display: inline-flex;
  }
}
</style>


<i18n>
en:
  DataMonitoring:
    title: Data monitoring
    settings: Settings
    lastUpdated: update
    scientificObjetcTypes: Types
    day: Day
    week: Week
    month: Month
    year: Year


fr:
  DataMonitoring:
    title: Suivi de données
    settings: Paramètres
    lastUpdated: statut
    scientificObjetcTypes: Types
    day: Jour
    week: Semaine
    month: Mois
    year: Année
</i18n>
