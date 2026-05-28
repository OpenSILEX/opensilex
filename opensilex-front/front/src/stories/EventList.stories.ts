import EventList from '../components/events/list/EventList.vue';

export default {
  title: 'Components/EventList',
  component: EventList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { EventList },
  setup() { return { args }; },
  template: '<EventList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
