import OntologyClassPropertyForm from '../components/ontology/class/OntologyClassPropertyForm.vue';

export default {
  title: 'Components/OntologyClassPropertyForm',
  component: OntologyClassPropertyForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OntologyClassPropertyForm },
  setup() { return { args }; },
  template: '<OntologyClassPropertyForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
