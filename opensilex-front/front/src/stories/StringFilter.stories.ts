import StringFilter from '../components/common/filters/StringFilter.vue';

export default {
  title: 'Components/StringFilter',
  component: StringFilter,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { StringFilter },
  setup() { return { args }; },
  template: '<StringFilter v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
