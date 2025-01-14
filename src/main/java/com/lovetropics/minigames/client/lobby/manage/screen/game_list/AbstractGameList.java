package com.lovetropics.minigames.client.lobby.manage.screen.game_list;

import com.google.common.collect.ImmutableList;
import com.lovetropics.minigames.client.lobby.state.ClientGameDefinition;
import com.lovetropics.minigames.client.screen.TrimmedText;
import com.lovetropics.minigames.client.screen.flex.Layout;
import com.lovetropics.minigames.client.screen.list.AbstractLTList;
import com.lovetropics.minigames.client.screen.list.LTListEntry;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public abstract class AbstractGameList extends AbstractLTList<AbstractGameList.Entry> {
	private final Component title;

	public AbstractGameList(Screen screen, Layout layout, Component title) {
		super(screen, layout, Entry.HEIGHT);
		this.setRenderHeader(true, this.minecraft.font.lineHeight + 4);
		this.setLeftPos(layout.background().left());

		// disable background
		this.setRenderBackground(false);
		this.setRenderTopAndBottom(false);

		this.setRenderSelection(false);

		this.title = title;
	}

	@Override
	protected void renderHeader(PoseStack matrixStack, int x, int y, Tesselator tessellator) {
		this.minecraft.font.draw(
				matrixStack, this.title,
				x + (this.width - this.minecraft.font.width(this.title)) / 2.0F,
				Math.min(this.y0 + 3, y),
				0xFFFFFF
		);
	}
	
	@Override
	public boolean isSelectedItem(int index) {
		return index >= 0 && index < this.getItemCount() && super.isSelectedItem(index);
	}

	public static final class Entry extends LTListEntry<Entry> {
		public static final int HEIGHT = 32;
		static final int PADDING = 4;

		private final int id;
		TrimmedText title = TrimmedText.of("");
		TrimmedText subtitle = null;

		private int backgroundColor = -1;
		private int selectedColor = 0xFF000000;
		private int hoveredColor = 0xFF202020;
		private int outlineColor = 0xFF808080;

		boolean banner;
		public Entry(AbstractLTList<Entry> list, int id) {
			super(list, list.screen);
			this.id = id;
		}

		public static Entry game(AbstractLTList<Entry> list, int id, ClientGameDefinition game) {
			Component playerRange = GameTexts.Ui.playerRange(game.minimumParticipants, game.maximumParticipants);

			Component subtitle;
			if (game.subtitle != null) {
				subtitle = game.subtitle.copy().append(" | ").append(playerRange);
			} else {
				subtitle = playerRange;
			}

			return new Entry(list, id)
					.setTitle(game.name)
					.setSubtitle(subtitle);
		}
		
		@Override
		public void render(PoseStack matrixStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTicks) {
			Font font = screen.getMinecraft().font;
			int fontHeight = font.lineHeight;
		
			boolean selected = ((AbstractGameList)list).isSelectedItem(index);
			boolean outline = banner || selected;
		
			this.fillEntry(matrixStack, left, top, width, height, hovered, selected, outline);
		
			int maxTextWidth = getMaxTextWidth(width);
		
			if (subtitle != null) {
				font.draw(matrixStack, title.forWidth(font, maxTextWidth), left + PADDING, top + PADDING + 1, 0xFFFFFF);
				font.draw(matrixStack, subtitle.forWidth(font, maxTextWidth), left + PADDING, top + height - PADDING - fontHeight, 0x555555);
			} else {
				font.draw(matrixStack, title.forWidth(font, maxTextWidth), left + PADDING, top + (height - fontHeight) / 2, 0xFFFFFF);
			}
		}

		@Override
		public void renderTooltips(PoseStack matrixStack, int width, int mouseX, int mouseY) {
			super.renderTooltips(matrixStack, width, mouseX, mouseY);
			TrimmedText subtitle = this.subtitle;
			int maxTextWidth = getMaxTextWidth(width);
			if (subtitle != null && subtitle.isTrimmedForWidth(screen.getMinecraft().font, maxTextWidth)) {
				screen.renderComponentTooltip(matrixStack, ImmutableList.of(subtitle.text()), mouseX, mouseY);
			}
		}

		public Entry setTitle(Component title) {
			this.title = TrimmedText.of(title);
			return this;
		}

		public Entry setSubtitle(Component subtitle) {
			this.subtitle = TrimmedText.of(subtitle);
			return this;
		}

		public Entry setBackgroundColor(int color) {
			this.backgroundColor = color;
			return this;
		}

		public Entry setHoveredColor(int color) {
			this.hoveredColor = color;
			return this;
		}

		public Entry setSelectedColor(int color) {
			this.selectedColor = color;
			return this;
		}

		public Entry setOutlineColor(int color) {
			this.outlineColor = color;
			return this;
		}

		public Entry setBanner(boolean banner) {
			this.banner = banner;
			return this;
		}

		public Entry setDraggable(Reorder reorder) {
			this.reorder = reorder;
			return this;
		}

		static int getMaxTextWidth(int width) {
			return width - 2 * PADDING;
		}

		void fillEntry(PoseStack matrixStack, int left, int top, int width, int height, boolean hovered, boolean selected, boolean outline) {
			if (banner) {
				top += 4;
				height -= 8;
			}

			int fillColor = getFillColor(hovered, selected);

			if (outline) {
				fillEntry(matrixStack, left, top, width, height, outlineColor);
				if (fillColor != -1) {
					fillEntry(matrixStack, left + 1, top + 1, width - 2, height - 2, fillColor);
				}
			} else {
				if (fillColor != -1) {
					fillEntry(matrixStack, left, top, width, height, fillColor);
				}
			}
		}

		private int getFillColor(boolean hovered, boolean selected) {
			if (selected) {
				return selectedColor;
			} else if (hovered) {
				return hoveredColor;
			} else {
				return backgroundColor;
			}
		}

		private void fillEntry(PoseStack matrixStack, int left, int top, int width, int height, int color) {
			AbstractGameList.fill(matrixStack, left, top, left + width, top + height, color);
		}

		public int getId() {
			return id;
		}

		@Override
		public Component getNarration() {
			return TextComponent.EMPTY;
		}
	}
}
