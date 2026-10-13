import type { Meta, StoryObj } from "@storybook/vue3-vite";
import OVLIcon from "../components/OVLIcon.vue";

const meta = {
  title: "Library/OVLIcon",
  component: OVLIcon,
  tags: ["common"],
  argTypes: {
    icon: { control: "text", description: "`fa#<name>` (FontAwesome) or `bi#bi-<name>` (Bootstrap Icons)" },
    size: {
      control: "select",
      options: ["xs", "sm", "lg", "2x", "3x"],
      description: "Size for FontAwesome only",
    },
  },
} satisfies Meta<typeof OVLIcon>;

export default meta;
type Story = StoryObj<typeof meta>;

export const FontAwesome: Story = {
  args: {
    icon: "fa#pencil-alt",
  },
};

export const FontAwesomeLarge: Story = {
  args: {
    icon: "fa#pencil-alt",
    size: "lg",
  },
};

export const BootstrapIcon: Story = {
  args: {
    icon: "bi#bi-clipboard",
  },
};

export const Default: Story = {
  args: {},
};
