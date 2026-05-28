import AssociatedVariableList from '../components/variables/AssociatedVariableList.vue';

export default {
  title: 'Components/AssociatedVariableList',
  component: AssociatedVariableList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AssociatedVariableList },
  setup() { return { args }; },
  template: '<AssociatedVariableList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
