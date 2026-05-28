import OntologyRelationsForm from '../components/ontology/OntologyRelationsForm.vue';

export default {
  title: 'Components/OntologyRelationsForm',
  component: OntologyRelationsForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OntologyRelationsForm },
  setup() { return { args }; },
  template: '<OntologyRelationsForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
