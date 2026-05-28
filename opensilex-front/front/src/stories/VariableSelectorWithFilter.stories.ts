import VariableSelectorWithFilter from '../components/variables/views/VariableSelectorWithFilter.vue';

export default {
  title: 'Components/VariableSelectorWithFilter',
  component: VariableSelectorWithFilter,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { VariableSelectorWithFilter },
  setup() { return { args }; },
  template: '<VariableSelectorWithFilter v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
