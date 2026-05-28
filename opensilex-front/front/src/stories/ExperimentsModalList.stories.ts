import ExperimentsModalList from '../components/experiments/ExperimentsModalList.vue';

export default {
  title: 'Components/ExperimentsModalList',
  component: ExperimentsModalList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ExperimentsModalList },
  setup() { return { args }; },
  template: '<ExperimentsModalList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
