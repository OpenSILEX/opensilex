import DetailButton from '../components/common/buttons/DetailButton.vue';

export default {
  title: 'Components/DetailButton',
  component: DetailButton,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DetailButton },
  setup() { return { args }; },
  template: '<DetailButton v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
