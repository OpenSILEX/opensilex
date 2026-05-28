import OrganizationList from '../components/organizations/OrganizationList.vue';

export default {
  title: 'Components/OrganizationList',
  component: OrganizationList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OrganizationList },
  setup() { return { args }; },
  template: '<OrganizationList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
