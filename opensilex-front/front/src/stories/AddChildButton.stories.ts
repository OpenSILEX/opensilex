import AddChildButton from '../components/common/buttons/AddChildButton.vue';

export default {
  title: 'Components/AddChildButton',
  component: AddChildButton,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AddChildButton },
  setup() { return { args }; },
  template: '<AddChildButton v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
