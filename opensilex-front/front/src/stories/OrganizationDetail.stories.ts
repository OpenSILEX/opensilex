import OrganizationDetail from '../components/organizations/OrganizationDetail.vue';

export default {
  title: 'Components/OrganizationDetail',
  component: OrganizationDetail,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OrganizationDetail },
  setup() { return { args }; },
  template: '<OrganizationDetail v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
