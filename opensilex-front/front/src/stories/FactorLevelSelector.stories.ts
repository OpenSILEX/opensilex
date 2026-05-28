import FactorLevelSelector from '../components/experiments/factors/FactorLevelSelector.vue';

export default {
  title: 'Components/FactorLevelSelector',
  component: FactorLevelSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FactorLevelSelector },
  setup() { return { args }; },
  template: '<FactorLevelSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
