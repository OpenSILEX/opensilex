import XSDDecimalInput from '../components/ontology/XSDDecimalInput.vue';

export default {
  title: 'Components/XSDDecimalInput',
  component: XSDDecimalInput,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { XSDDecimalInput },
  setup() { return { args }; },
  template: '<XSDDecimalInput v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
