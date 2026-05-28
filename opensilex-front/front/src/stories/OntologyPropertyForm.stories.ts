import OntologyPropertyForm from '../components/ontology/property/OntologyPropertyForm.vue';

export default {
  title: 'Components/OntologyPropertyForm',
  component: OntologyPropertyForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OntologyPropertyForm },
  setup() { return { args }; },
  template: '<OntologyPropertyForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
