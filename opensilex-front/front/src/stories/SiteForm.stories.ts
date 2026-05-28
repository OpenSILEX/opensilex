import SiteForm from '../components/organizations/site/SiteForm.vue';

export default {
  title: 'Components/SiteForm',
  component: SiteForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { SiteForm },
  setup() { return { args }; },
  template: '<SiteForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
