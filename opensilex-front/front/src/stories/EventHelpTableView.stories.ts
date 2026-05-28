import EventHelpTableView from '../components/events/form/csv/EventHelpTableView.vue';

export default {
  title: 'Components/EventHelpTableView',
  component: EventHelpTableView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { EventHelpTableView },
  setup() { return { args }; },
  template: '<EventHelpTableView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
