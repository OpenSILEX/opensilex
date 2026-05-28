import VariableList from '../components/variables/VariableList.vue';

export default {
  title: 'Components/VariableList',
  component: VariableList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { VariableList },
  setup() { return { args }; },
  template: '<VariableList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
