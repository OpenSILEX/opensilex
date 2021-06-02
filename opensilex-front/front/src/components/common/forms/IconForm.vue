<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
    :validationDisabled="validationDisabled"
  >
    <template v-slot:field="field">
      <input :id="field.id" type="hidden" :value="stringValue" />
      <v-select
        :options="paginated"
        :filterable="false"
        v-model="stringValue"
        @open="onOpen"
        @close="onClose"
        @search="query => search = query"
        :reduce="option => option.id"
        append-to-body
        :calculate-position="withPopper"
      >
        <template #open-indicator="{ attributes }">
          <span v-bind="attributes" class="open-indicator">
            <opensilex-Icon icon="fa#caret-down"></opensilex-Icon>
          </span>
        </template>

        <template v-slot:option="option">
          <opensilex-Icon :icon="option.id"></opensilex-Icon>
          &nbsp;({{option.label}})
        </template>

        <template v-slot:selected-option="option">
          <opensilex-Icon v-if="option.id" :icon="option.id"></opensilex-Icon>
          <opensilex-Icon v-else :icon="stringValue"></opensilex-Icon>
          &nbsp;({{option.label}})
        </template>
        <template #list-footer>
          <li ref="load" class="loader" v-show="hasNextPage">Loading more options...</li>
        </template>
      </v-select>
    </template>
  </opensilex-FormField>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync,
  Ref
} from "vue-property-decorator";
import Vue from "vue";
import { createPopper } from "@popperjs/core";

@Component
export default class IconForm extends Vue {
  $opensilex: any;

  @PropSync("value")
  stringValue: string;

  @Prop()
  label: string;

  @Prop()
  helpMessage: string;

  @Prop()
  placeholder: string;

  @PropSync("required")
  isRequired: boolean;

  @Prop()
  disabled: boolean;

  @Prop({
    default: false
  })
  rules: string | Function;

  @Prop({
    default: false
  })
  validationDisabled: boolean;

  observer = null;
  limit = 20;
  search = "";

  icons = [];

  @Ref("load") readonly load!: any;

  mounted() {
    this.icons = this.$opensilex.getSelectIconIDs();
    let self = this;
    this.observer = new IntersectionObserver(function([
      { isIntersecting, target }
    ]) {
      let t: any = target;
      if (isIntersecting) {
        const ul = t.offsetParent;
        const scrollTop = t.offsetParent.scrollTop;
        self.limit += 10;
        self.$nextTick().then(() => {
          ul.scrollTop = scrollTop;
        });
      }
    });
  }

  get filtered() {
    return this.icons.filter(icon => icon.id.includes(this.search));
  }

  get paginated() {
    return this.filtered.slice(0, this.limit);
  }

  get hasNextPage() {
    return this.paginated.length < this.filtered.length;
  }

  async onOpen() {
    if (this.hasNextPage) {
      await this.$nextTick();
      this.observer.observe(this.load);
    }
  }

  onClose() {
    this.observer.disconnect();
  }

  withPopper(dropdownList, component, { width }) {
    /**
     * We need to explicitly define the dropdown width since
     * it is usually inherited from the parent with CSS.
     */
    dropdownList.style.width = width;

    /**
     * Here we position the dropdownList relative to the $refs.toggle Element.
     *
     * The 'offset' modifier aligns the dropdown so that the $refs.toggle and
     * the dropdownList overlap by 1 pixel.
     *
     * The 'toggleClass' modifier adds a 'drop-up' class to the Vue Select
     * wrapper so that we can set some styles for when the dropdown is placed
     * above.
     */
    const popper = createPopper(component.$refs.toggle, dropdownList, {
      placement: "top",
      modifiers: [
        {
          name: "offset",
          options: {
            offset: [0, -1]
          }
        },
        {
          name: "toggleClass",
          enabled: true,
          phase: "write",
          fn({ state }) {
            component.$el.classList.toggle(
              "drop-up",
              state.placement === "top"
            );
          }
        }
      ]
    });

    /**
     * To prevent memory leaks Popper needs to be destroyed.
     * If you return function, it will be called just before dropdown is removed from DOM.
     */
    return () => popper.destroy();
  }
}
</script>

<style scoped lang="scss">
::v-deep .vs__selected {
  white-space: break-spaces;
}

.open-indicator {
  color: rgb(204, 204, 204);
}

.open-indicator:hover {
  cursor: pointer;
  color: rgb(97, 97, 97);
}

::v-deep .open-indicator svg {
  font-size: 16px;
}

::v-deep .v-select-unselect {
  color: rgb(204, 204, 204);
}

::v-deep .v-select-unselect:hover {
  color: #e53935;
  cursor: pointer;
}
</style>

