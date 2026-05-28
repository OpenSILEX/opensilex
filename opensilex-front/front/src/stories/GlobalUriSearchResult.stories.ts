import GlobalUriSearchResult from '../components/globalUriSearch/GlobalUriSearchResult.vue';

export default {
  title: 'Components/GlobalUriSearchResult',
  component: GlobalUriSearchResult,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { GlobalUriSearchResult },
  setup() { return { args }; },
  template: '<GlobalUriSearchResult v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
