import VariableView from '../components/variables/views/VariableView.vue';

export default {
  title: 'Components/VariableView',
  component: VariableView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { VariableView },
  setup() { return { args }; },
  template: '<VariableView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
