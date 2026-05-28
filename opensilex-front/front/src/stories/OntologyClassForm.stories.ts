import OntologyClassForm from '../components/ontology/class/OntologyClassForm.vue';

export default {
  title: 'Components/OntologyClassForm',
  component: OntologyClassForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OntologyClassForm },
  setup() { return { args }; },
  template: '<OntologyClassForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
