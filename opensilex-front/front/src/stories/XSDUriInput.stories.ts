import XSDUriInput from '../components/ontology/XSDUriInput.vue';

export default {
  title: 'Components/XSDUriInput',
  component: XSDUriInput,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { XSDUriInput },
  setup() { return { args }; },
  template: '<XSDUriInput v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
