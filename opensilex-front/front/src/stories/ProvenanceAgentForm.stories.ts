import ProvenanceAgentForm from '../components/data/form/ProvenanceAgentForm.vue';

export default {
  title: 'Components/ProvenanceAgentForm',
  component: ProvenanceAgentForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ProvenanceAgentForm },
  setup() { return { args }; },
  template: '<ProvenanceAgentForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
