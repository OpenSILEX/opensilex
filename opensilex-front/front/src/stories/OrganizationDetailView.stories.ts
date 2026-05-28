import OrganizationDetailView from '../components/organizations/OrganizationDetailView.vue';

export default {
  title: 'Components/OrganizationDetailView',
  component: OrganizationDetailView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OrganizationDetailView },
  setup() { return { args }; },
  template: '<OrganizationDetailView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
