import ForgotPassword from '../components/layout/ForgotPassword.vue';

export default {
  title: 'Components/ForgotPassword',
  component: ForgotPassword,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ForgotPassword },
  setup() { return { args }; },
  template: '<ForgotPassword v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
