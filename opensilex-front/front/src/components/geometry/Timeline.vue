<template>
    <ul v-if="items.length > 0" class="timeline">
        <div class="arrowhead"></div>
        <li v-for="(item, key) in items" :key="key" class="timeline-item" :class="isItemSelected(item)" @click="selectEvent(item)">
            <div class="timeline-badge" :class="item.icon_status"><opensilex-Icon :icon="getIconFromType(item.rdf_type)"></opensilex-Icon></div>
            <div class="timeline-panel grow" :class="[getElementStatus(item.start), item.element_day_marker]">
                <div class="timeline-heading">
                    <h5 class="timeline-title" :class="item.text_status">{{ item.rdf_type_name }}</h5>
                    <div class="timeline-panel-controls">
                        <div class="controls">
                            <a 
                            v-for="(control, key_ctrl) in item.controls"
                            :key="key_ctrl"
                            :control="control">
                            </a>
                        </div>
                        <div class="timestamp">
                            <small v-if="item.start" class="">{{ $t('Timeline.startDate') }}: {{ getDateFormat(item.start) }}</small>
                            <br v-if="item.start">
                            <small v-if="item.end" class="">{{ $t('Timeline.endDate') }}: {{ getDateFormat(item.end) }}</small>
                        </div>
                    </div>
                </div>
                <div class="timeline-body">
                    <div class="timeline-description">
                        {{ item.description ? " - " + item.description : '' }}
                    </div>
                </div>
            </div>
        </li>
    </ul>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

/*
Scientific object management - (ScientificObjectManagement) - fa#vial
    Treatment - (Treatment)
        Preventive treatment - (PreventativeTreatment)
        Curative treatment - (CurativeTreatment)
    Observed phenology - (PhenologyScoring)
    Irrigation - (Irrigation) - fa#water
    Potting - (Potting) - fa#seedling
    Sampling - (Sampling)
    Loading - (Loading) - fa#spinner
    Adding product - (AddingProduct)
        Fertilization - (Fertilization)
    Detasseling - (Detasseling)
    Sowing - (Sowing)
    Staking - (Staking)
    Thinning - (Thinning)
    Clipping - (Clipping)
    Harvesting - (Harvesting)
Facility management - (FacilityManagement) - fa#tasks
    Start - (Start) - fa#play
    Installation of devices - (Installation)
    Calibration - (Calibration)
        Manual calibration - (ManualCalibration)
        Automatic calibration - (AutomaticCalibration)
    Association - (Association)
        Association with a scientific object - (AssociationWithScientificObject)
        Association with a sensing device - (AssociationWithSensingDevice)
    Restart - (Restart) - fa#redo
    Stop - (Stop) - fa#stop
    Operability - (Operability) - fa#hammer
        Corrective maintenance - (CorrectiveMaintenance) - fa#hammer
            Repair - (Reparation)
        Servicing - (Servicing) - fa#hammer
            Checking - (Checking)
            Cleaning - (Cleaning)
        Evolution maintenance - (EvolutiveMaintenance)
Trouble - (Trouble) - fa#exclamation-triangle
    Incident - (Incident)
        Stuck plant - (StuckPlant)
        Pest attack - (PestAttack)
        Lodging - (Lodging)
        Pot fall - (PotFall)
    Breakdown - (Breakdown)
    Dysfunction - (Dysfunction)
Move - fa#chevron-circle-right
*/

const objectsIcons: Array<any> = [
    {
        fa: "fa#vial",
        objs: ["ScientificObjectManagement", "Treatment", "PreventativeTreatment", "CurativeTreatment",
        "PhenologyScoring", "Sampling", "AddingProduct", "Fertilization", "Detasseling", "Sowing", "Staking",
        "Thinning", "Clipping", "Harvesting"]
    },
    {
        fa: "fa#water",
        objs: ["Irrigation", "Flooding"]
    },
    {
        fa: "fa#seedling",
        objs: ["Potting"]
    },
    {
        fa: "fa#spinner",
        objs: ["Loading"]
    },
    {
        fa: "fa#tasks",
        objs: ["FacilityManagement", "Installation", "Calibration", "ManualCalibration", "AutomaticCalibration",
        "Association", "AssociationWithScientificObject", "AssociationWithSensingDevice"]
    },
    {
        fa: "fa#play",
        objs: ["Start"]
    },
    {
        fa: "fa#redo",
        objs: ["Restart"]
    },
    {
        fa: "fa#stop",
        objs: ["Stop"]
    },
    {
        fa: "fa#hammer",
        objs: ["Operability", "CorrectiveMaintenance", "Reparation", "Servicing", "Checking",
        "Cleaning", "EvolutiveMaintenance"]
    },
    {
        fa: "fa#exclamation-triangle",
        objs: ["Trouble", "Incident", "StuckPlant", "PestAttack", "Lodging",
        "PotFall", "Breakdown", "Dysfunction"]
    },
    {
        fa: "fa#chevron-circle-right",
        objs: ["Move"]
    }
];


@Component
export default class Timeline extends Vue {
    @Prop()
    items: any;

    @Prop()
    selectedFeatures: any;

    getIconFromType(type: string): String {
        if (type) {
            for (let item of objectsIcons) {
                for (let obj of item.objs) {
                    if (obj == this.getType(type)) {
                        return item.fa;
                    }
                }
            }
        }
        return "ik#ik-activity";
    }

    getType(type: string): String {
        const res = new URL(type);
        if (res.hash != '') { // long type (example: http://opensilex.dev/set/area#youpi)
        return res.hash;
        } else { // short type (example: Vocabulary#Area)
        return res.pathname;
        }
    }

    getDateFormat(dateStr: string): String {
        return new Date(dateStr).toLocaleString();
    }

    getElementStatus(is_instant: boolean): String {
        if (is_instant) {
            return "main_element"
        } else {
            return "selected_past"
        }
    }

    isItemSelected(item) {
        for (let selected of this.selectedFeatures) {
            if (selected.properties.uri == item.targets[0]) {
                return "pulse_wrap";
            }
        }
        return "";
    }

    selectEvent(event) {
        this.$emit('onClick', event.targets[0]);
    }
}

</script>

<style scoped lang="scss">
@import 'https://fonts.googleapis.com/css?family=Libre+Franklin';

body {
    font-family: 'Libre Franklin', sans-serif;
}

#timeline-header {
    font-size: 26px;
}

.cancelled {
    text-decoration: line-through;
}

.timeline-panel.today {
  height: 5px !important;
  padding-top: 0px !important;
  padding-bottom: 0px !important;
  margin-top: 0px;
  margin-bottom: 10px;
  background: #000;
  &:before {
    visibility: hidden !important;
    display: none !important;
  }
  &:after {
    visibility: hidden !important;
    display: none !important;
  }
}

.timeline-badge.warning {
  top: -20px !important;
}

.timeline-panel.past {
  background: #eee;
  &:after {
    border-right: 14px solid #eee !important;
  }
}

.timeline-panel.main_element {
    font-weight: bolder;
    color: #FFFFFF !important;
    background: #00a38d;
    border-color: #00a38d !important;
  &:after {
    border-right: 14px solid #00a38d !important;
  }
}

.timeline-panel.selected_past {
    font-weight: bolder;
    color: #FFFFFF !important;
    background: #333;
    border-color: #333 !important;
  &:after {
    border-right: 14px solid #333 !important;
  }
}

.timeline {
    list-style: none;
    padding: 10px 0 10px;
    margin-left: -10px;
    position: relative;
    width: 380px;
    margin-top: 20px;
    padding-top: 30px;

    &:before {
        background-color: rgb(95, 95, 95);
        bottom: 0;
        content: " ";
        left: 48px;
        margin-left: -1.5px;
        position: absolute;
        top: 0;
        width: 6px;
    }
    .arrowhead{
        width: 0;
        position: absolute;
        top: 0;
        left: 50px;
        margin-top: -3px;
        margin-left: -20px;
        
        border-top: 0 solid transparent;
        border-left: 20px solid transparent;
        border-right: 20px solid transparent;
        border-bottom: 20px solid rgb(95, 95, 95);
    }
    .arrowhead{
        width: 0;
        position: absolute;
        top: 0;
        left: 50px;
        margin-top: -3px;
        margin-left: -20px;
        
        border-top: 0 solid transparent;
        border-left: 20px solid transparent;
        border-right: 20px solid transparent;
        border-bottom: 20px solid rgb(95, 95, 95);
    }

    > li {
        margin-bottom: 15px;
        position: relative;

        &:before,
        &:after {
            content: " ";
            display: table;
        }

        &:after {
            clear: both;
        }

        > .timeline-panel {
            border-radius: 2px;
            border: 1px solid #d4d4d4;
            box-shadow: 0 1px 2px rgba(100, 100, 100, 0.2);
            margin-left: 100px;
            padding: 20px;
            position: relative;
          
          .timeline-heading {
            .timeline-panel-controls {
              position: absolute;
              right: 8px;
              top: 5px;
              
              .timestamp {
                display: inline-block;
              }
              
              .controls {
                display: inline-block;
                padding-right: 5px;
                border-right: 1px solid #aaa;
                
                a {
                  color: #999;
                  font-size: 11px;
                  padding: 0 5px;
                  
                  &:hover {
                    color: #333;
                    text-decoration: none;
                    cursor: pointer;
                  }
                }
              }
              
              .controls + .timestamp {
                padding-left: 5px;
              }
            }
          }
        }

        .timeline-badge {
            background-color: #999;
            border-radius: 50%;
            color: #fff;
            font-size: 1.4em;
            height: 50px;
            left: 50px;
            line-height: 52px;
            margin-left: -25px;
            position: absolute;
            text-align: center;
            top: 16px;
            width: 50px;
            z-index: 100;
        }

        .timeline-badge + .timeline-panel {
            &:before {
                border-bottom: 15px solid transparent;
                border-left: 0 solid #ccc;
                border-right: 15px solid #ccc;
                border-top: 15px solid transparent;
                content: " ";
                display: inline-block;
                position: absolute;
                left: -15px;
                right: auto;
                top: 26px;
            }

            &:after {
                border-bottom: 14px solid transparent;
                border-left: 0 solid #fff;
                border-right: 14px solid #fff;
                border-top: 14px solid transparent;
                content: " ";
                display: inline-block;
                position: absolute;
                left: -14px;
                right: auto;
                top: 27px;
            }
        }
    }
}

.timeline-badge {
    &.primary {
        background-color: #2e6da4 !important;
    }

    &.success {
        background-color: #00a38d !important;
    }

    &.warning {
        background-color: #000000 !important;
    }

    &.danger {
        background-color: #d9534f !important;
    }

    &.info {
        background-color: #5bc0de !important;
    }
  
    &.planning {
        background-color: #00629c !important;
    }
}

.timeline-title {
  margin-right: 112px;
  margin-top: 0;
  color: inherit;
}

.timeline-body {
    word-break: break-all;
    > p,
    > ul {
        margin-bottom: 0;
    }

    > p + p {
        margin-top: 5px;
    }
}

.timeline-description {
    font-style: italic;
}

.copy {
  position: absolute; 
  top: 5px;
  right: 5px;
  color: #aaa;
  font-size: 11px;
  > * { color: #444; }
}


/*============================
				ANIMATIONS
=============================*/
.pulse_wrap {
	animation: pulse 1.5s ease-in-out alternate infinite;

}

@keyframes pulse {
	0% {
		opacity: 0.8;
		transform: scale(0.95);
    //margin-left: -20px;
	}
  
  100% {
		opacity: 1;
		transform: scale(1);
	}
  
}

.grow {
  transition: all .2s ease-in-out;
}

.grow:hover {
  transform: scale(1.04);
}

</style>

<i18n>
en:
    Timeline:
        startDate: Start
        endDate: End
fr:
    Timeline:
        startDate: Début
        endDate: Fin
</i18n>
