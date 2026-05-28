import Button from '../components/common/buttons/Button.vue';

export default {
  title: 'Components/Button',
  component: Button,

  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { Button },
  setup() { return { args }; },
  template: '<div><Button v-bind="args" /></div>',
});

export const Default = Template.bind({});
Default.args = {
  label: 'save',
  variant: 'primary',
  small: false,
  disabled: false,
  icon: 'fa#save',
  helpMessage: 'save_help',
  href: ''
};

export const LinkButton = Template.bind({});
LinkButton.args = {
  label: 'documentation',
  href: 'https://example.com/docs',
  variant: 'secondary',
  icon: 'fa#book',
  helpMessage: 'open_documentation'
};
