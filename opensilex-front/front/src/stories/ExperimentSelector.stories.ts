import ExperimentSelector from '../components/experiments/ExperimentSelector.vue';

export default {
  title: 'Components/ExperimentSelector',
  component: ExperimentSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ExperimentSelector },
  setup() { return { args }; },
  template: '<ExperimentSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
