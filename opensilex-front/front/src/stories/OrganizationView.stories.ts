import OrganizationView from '../components/organizations/OrganizationView.vue';

export default {
  title: 'Components/OrganizationView',
  component: OrganizationView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OrganizationView },
  setup() { return { args }; },
  template: '<OrganizationView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
