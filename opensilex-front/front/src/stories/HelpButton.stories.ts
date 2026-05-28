import HelpButton from '../components/common/buttons/HelpButton.vue';

export default {
  title: 'Components/HelpButton',
  component: HelpButton,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { HelpButton },
  setup() { return { args }; },
  template: '<HelpButton v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
