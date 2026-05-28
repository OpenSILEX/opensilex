import GermplasmSelector from '../components/germplasm/GermplasmSelector.vue';

export default {
  title: 'Components/GermplasmSelector',
  component: GermplasmSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { GermplasmSelector },
  setup() { return { args }; },
  template: '<GermplasmSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
