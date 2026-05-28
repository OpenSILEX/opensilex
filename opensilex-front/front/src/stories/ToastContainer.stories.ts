import ToastContainer from '../components/common/ToastContainer.vue';

export default {
  title: 'Components/ToastContainer',
  component: ToastContainer,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ToastContainer },
  setup() { return { args }; },
  template: '<ToastContainer v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
