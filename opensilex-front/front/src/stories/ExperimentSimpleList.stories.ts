import ExperimentSimpleList from '../components/experiments/ExperimentSimpleList.vue';

export default {
  title: 'Components/ExperimentSimpleList',
  component: ExperimentSimpleList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ExperimentSimpleList },
  setup() { return { args }; },
  template: '<ExperimentSimpleList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
