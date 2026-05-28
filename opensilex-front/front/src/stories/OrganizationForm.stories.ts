import OrganizationForm from '../components/organizations/OrganizationForm.vue';

export default {
  title: 'Components/OrganizationForm',
  component: OrganizationForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OrganizationForm },
  setup() { return { args }; },
  template: '<OrganizationForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
