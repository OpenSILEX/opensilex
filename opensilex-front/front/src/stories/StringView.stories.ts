import StringView from '../components/common/views/StringView.vue';

export default {
  title: 'Components/StringView',
  component: StringView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { StringView },
  setup() { return { args }; },
  template: '<StringView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
