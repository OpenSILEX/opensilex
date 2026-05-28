import AddressView from '../components/common/views/AddressView.vue';

export default {
  title: 'Components/AddressView',
  component: AddressView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AddressView },
  setup() { return { args }; },
  template: '<AddressView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
