import GDPR from '../components/tools/GDPR.vue';

export default {
  title: 'Components/GDPR',
  component: GDPR,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { GDPR },
  setup() { return { args }; },
  template: '<GDPR v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
