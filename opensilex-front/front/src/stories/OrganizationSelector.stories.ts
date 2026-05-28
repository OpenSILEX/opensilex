import OrganizationSelector from '../components/organizations/OrganizationSelector.vue';

export default {
  title: 'Components/OrganizationSelector',
  component: OrganizationSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OrganizationSelector },
  setup() { return { args }; },
  template: '<OrganizationSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
