import PositionList from '../components/positions/list/PositionList.vue';

export default {
  title: 'Components/PositionList',
  component: PositionList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { PositionList },
  setup() { return { args }; },
  template: '<PositionList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
