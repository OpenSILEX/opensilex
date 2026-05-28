import ProvenanceForm from '../components/data/form/ProvenanceForm.vue';

export default {
  title: 'Components/ProvenanceForm',
  component: ProvenanceForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ProvenanceForm },
  setup() { return { args }; },
  template: '<ProvenanceForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
