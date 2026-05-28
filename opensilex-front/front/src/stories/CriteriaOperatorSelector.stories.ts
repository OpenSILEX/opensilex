import CriteriaOperatorSelector from '../components/scientificObjects/CriteriaOperatorSelector.vue';

export default {
  title: 'Components/CriteriaOperatorSelector',
  component: CriteriaOperatorSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { CriteriaOperatorSelector },
  setup() { return { args }; },
  template: '<CriteriaOperatorSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
