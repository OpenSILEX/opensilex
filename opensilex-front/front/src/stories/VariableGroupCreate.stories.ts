import VariableGroupCreate from '../components/variables/form/VariableGroupCreate.vue';

export default {
  title: 'Components/VariableGroupCreate',
  component: VariableGroupCreate,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { VariableGroupCreate },
  setup() { return { args }; },
  template: '<VariableGroupCreate v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
