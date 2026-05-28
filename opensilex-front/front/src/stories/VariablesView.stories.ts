import VariablesView from '../components/variables/VariablesView.vue';

export default {
  title: 'Components/VariablesView',
  component: VariablesView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { VariablesView },
  setup() { return { args }; },
  template: '<VariablesView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
