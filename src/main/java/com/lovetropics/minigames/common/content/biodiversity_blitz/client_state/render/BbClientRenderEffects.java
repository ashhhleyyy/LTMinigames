package com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.render;

import com.google.common.base.Suppliers;
import com.lovetropics.minigames.Constants;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitz;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.ClientBbGlobalState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.ClientBbSelfState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.CurrencyTargetState;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Constants.MODID, value = Dist.CLIENT)
public final class BbClientRenderEffects {
	private static final Minecraft CLIENT = Minecraft.getInstance();
	private static final int PADDING = 2;

	// TODO: hardcoded
	private static final Supplier<ItemStack> CURRENCY_ITEM = Suppliers.memoize(() -> new ItemStack(BiodiversityBlitz.OSA_POINT.get()));
	private static final int ITEM_SIZE = 16;

	@SubscribeEvent
	public static void renderGameOverlay(RenderGameOverlayEvent event) {
		if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT) {
			return;
		}

		ClientBbSelfState selfState = ClientGameStateManager.getOrNull(BiodiversityBlitz.SELF_STATE);
		if (selfState != null) {
			CurrencyTargetState currencyTarget = ClientGameStateManager.getOrNull(BiodiversityBlitz.CURRENCY_TARGET);
			renderOverlay(event, selfState, currencyTarget);
		}
	}

	private static void renderOverlay(RenderGameOverlayEvent event, ClientBbSelfState selfState, CurrencyTargetState currencyTarget) {
		MatrixStack matrixStack = event.getMatrixStack();
		FontRenderer font = CLIENT.fontRenderer;

		final int left = PADDING;
		final int top = PADDING;

		int x = left;
		int y = top;

		renderItem(matrixStack, CURRENCY_ITEM.get(), x, y);

		String currency = String.valueOf(selfState.getCurrency());
		if (currencyTarget != null) {
			currency = currency + TextFormatting.GRAY + "/" + currencyTarget.getValue();
		}

		font.drawStringWithShadow(
				matrixStack, currency,
				x + ITEM_SIZE + PADDING,
				y + (ITEM_SIZE - font.FONT_HEIGHT) / 2,
				0xFFFFFFFF
		);
		y += ITEM_SIZE + PADDING;

		int increment = selfState.getNextIncrement();
		boolean gainingCurrency = increment > 0;
		TextFormatting incrementColor = gainingCurrency ? TextFormatting.AQUA : TextFormatting.RED;

		String nextCurrencyIncrement = incrementColor + "+" + increment + TextFormatting.GRAY + " next drop";
		font.drawStringWithShadow(matrixStack, nextCurrencyIncrement, x, y, 0xFFFFFFFF);
		y += font.FONT_HEIGHT;

		if (!gainingCurrency) {
			font.drawStringWithShadow(matrixStack, TextFormatting.GRAY + "You must be in your plot to receive osas!", x, y, 0xFFFFFFFF);
		}
	}

	private static void renderItem(MatrixStack matrixStack, ItemStack stack, int x, int y) {
		RenderSystem.pushMatrix();
		RenderSystem.multMatrix(matrixStack.getLast().getMatrix());
		CLIENT.getItemRenderer().renderItemIntoGUI(stack, x, y);
		RenderSystem.popMatrix();
	}

	@SubscribeEvent
	public static void onRenderPlayerName(RenderNameplateEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof PlayerEntity) {
			ClientBbGlobalState globalState = ClientGameStateManager.getOrNull(BiodiversityBlitz.GLOBAL_STATE);
			if (globalState != null && globalState.hasCurrencyFor(entity.getUniqueID())) {
				int currency = globalState.getCurrencyFor(entity.getUniqueID());
				renderPlayerCurrency(event, entity, currency);
			}
		}
	}

	private static void renderPlayerCurrency(RenderNameplateEvent event, Entity entity, int currency) {
		EntityRendererManager renderManager = event.getEntityRenderer().getRenderManager();
		double distance2 = renderManager.squareDistanceTo(entity);
		if (!ForgeHooksClient.isNameplateInRenderDistance(entity, distance2) || entity.isDiscrete()) {
			return;
		}

		if (entity == CLIENT.renderViewEntity || !Minecraft.isGuiEnabled()) {
			return;
		}

		final float itemSize = 16.0F;
		final float textScale = 1.0F / 2.5F;

		String currencyText = String.valueOf(currency);

		MatrixStack matrixStack = event.getMatrixStack();
		IRenderTypeBuffer buffer = event.getRenderTypeBuffer();
		int packedLight = event.getPackedLight();

		FontRenderer font = renderManager.getFontRenderer();
		ItemRenderer items = CLIENT.getItemRenderer();

		float left = -(font.getStringWidth(currencyText) * textScale + itemSize) / 2.0F;

		matrixStack.push();
		matrixStack.translate(0.0, entity.getHeight() + 0.75, 0.0);
		matrixStack.rotate(renderManager.getCameraOrientation());
		matrixStack.scale(0.0625F * textScale, 0.0625F * textScale, 0.0625F * textScale);

		matrixStack.push();
		matrixStack.scale(-1.0F, -1.0F, 1.0F);

		float textX = (left + itemSize) * textScale;
		float textY = -font.FONT_HEIGHT / 2.0F;
		font.drawString(matrixStack, currencyText, textX, textY, 0xFFFFFFFF);
		matrixStack.pop();

		matrixStack.push();
		matrixStack.translate(-left, 0.0F, 0.0F);
		matrixStack.scale(-itemSize, itemSize, -itemSize);
		items.renderItem(CURRENCY_ITEM.get(), ItemCameraTransforms.TransformType.GUI, packedLight, OverlayTexture.NO_OVERLAY, matrixStack, buffer);
		matrixStack.pop();

		matrixStack.pop();
	}
}
