import DeprecatedButton from '../components/common/buttons/DeprecatedButton.vue';

export default {
  title: 'Components/DeprecatedButton',
  component: DeprecatedButton,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DeprecatedButton },
  setup() { return { args }; },
  template: '<DeprecatedButton v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
