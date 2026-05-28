import PositionView from '../components/positions/list/view/PositionView.vue';

export default {
  title: 'Components/PositionView',
  component: PositionView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { PositionView },
  setup() { return { args }; },
  template: '<PositionView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
