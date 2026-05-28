import MoveView from '../components/events/view/MoveView.vue';

export default {
  title: 'Components/MoveView',
  component: MoveView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { MoveView },
  setup() { return { args }; },
  template: '<MoveView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
