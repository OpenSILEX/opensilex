import DateView from '../components/common/views/DateView.vue';

export default {
  title: 'Components/DateView',
  component: DateView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DateView },
  setup() { return { args }; },
  template: '<DateView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
