import GlobalUriSearchBox from '../components/globalUriSearch/GlobalUriSearchBox.vue';

export default {
  title: 'Components/GlobalUriSearchBox',
  component: GlobalUriSearchBox,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { GlobalUriSearchBox },
  setup() { return { args }; },
  template: '<GlobalUriSearchBox v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
