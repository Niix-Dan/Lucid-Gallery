package com.niixlabs.lucidgallery.client.gui.screen;

import com.niixlabs.lucidgallery.client.gui.card.ScreenshotCard;
import com.niixlabs.lucidgallery.client.gui.util.GuiScale;
import com.niixlabs.lucidgallery.client.gui.util.LucidUploader;
import com.niixlabs.lucidgallery.client.gui.util.TextureLoader;
import com.niixlabs.lucidgallery.client.gui.util.LucidScrollHandler;
import com.niixlabs.lucidgallery.config.ConfigManager;
import com.niixlabs.lucidgallery.config.LucidConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class GalleryScreen extends Screen {
    private final List<ScreenshotCard> cards = new ArrayList<>();
    private final LucidScrollHandler scrollHandler = new LucidScrollHandler();
    private ScreenshotCard selectedCard = null;
    private String statusMessage = "";
    private long statusTimer = 0;

    private Button viewButton;
    private Button deleteButton;
    private Button closeButton;
    private Button uploadButton;
    private int previewX;
    private int previewY;
    private int previewWidth;
    private int previewHeight;
    private int uploadRowY;

    public GalleryScreen() {
        super(Component.translatable("gui.lucidgallery.title"));
    }

    @Override
    protected void init() {
        this.width = (int) (this.minecraft.getWindow().getScreenWidth() / GuiScale.targetScale(this.minecraft));
        this.height = (int) (this.minecraft.getWindow().getScreenHeight() / GuiScale.targetScale(this.minecraft));
        this.clearWidgets();
        reloadScreenshots();

        int centerX = this.width / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.lucidgallery.open_folder_button"), b -> {
            File screenshotsDir = new File(Minecraft.getInstance().gameDirectory, "screenshots");
            Util.getPlatform().openFile(screenshotsDir);
        }).bounds(centerX - 155, 10, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.lucidgallery.refresh_button"), b -> reloadScreenshots())
                .bounds(centerX - 50, 10, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.lucidgallery.close_button"), b -> this.onClose())
                .bounds(centerX + 55, 10, 100, 20).build());

        this.addRenderableWidget(new ScaleSlider(this.width - 110, 10, 100, 20));

        this.previewWidth = (int) (this.width * 0.7);
        this.previewHeight = (int) (this.height * 0.7);
        this.previewX = (this.width - previewWidth) / 2;
        this.previewY = (this.height - previewHeight) / 2;
        int btnY = previewY + previewHeight + 8;
        this.uploadRowY = btnY + 24;

        this.viewButton = Button.builder(Component.translatable("gui.lucidgallery.modal.view_button"), b -> {
            if (selectedCard != null) {
                Util.getPlatform().openFile(selectedCard.getFile());
            }
        }).bounds(previewX, btnY, 80, 20).build();

        this.deleteButton = Button.builder(Component.translatable("gui.lucidgallery.modal.delete_button"), b -> {
            if (selectedCard == null) return;
            try {
                Files.delete(selectedCard.getFile().toPath());
                showStatus(Component.translatable("gui.lucidgallery.action.delete_success").getString());
                selectedCard = null;
                reloadScreenshots();
            } catch (Exception e) {
                showStatus(Component.translatable("gui.lucidgallery.action.delete_fail").getString());
            }
        }).bounds(previewX + 90, btnY, 80, 20).build();

        this.closeButton = Button.builder(Component.translatable("gui.lucidgallery.modal.close_button"), b -> selectedCard = null)
                .bounds(previewX + 180, btnY, 80, 20).build();

        this.uploadButton = Button.builder(Component.translatable("gui.lucidgallery.upload.button.upload"), b -> handleUploadButtonClick())
                .bounds(previewX + 270, btnY, 80, 20).build();
    }

    private void reloadScreenshots() {
        cards.clear();
        File screenshotsDir = new File(Minecraft.getInstance().gameDirectory, "screenshots");
        if (screenshotsDir.exists() && screenshotsDir.isDirectory()) {
            File[] files = screenshotsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (files != null) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
                for (File file : files) {
                    cards.add(new ScreenshotCard(file, this::showStatus, () -> {
                        this.selectedCard = null;
                        this.reloadScreenshots();
                    }, card -> this.selectedCard = card));
                }
            }
        }
        recalculateLayout();
    }

    private void recalculateLayout() {
        int cols = Math.max(1, LucidConfig.gridColumns);
        int margin = 10;
        int startY = 40;
        int availableWidth = this.width - 40;
        int cardWidth = (availableWidth - (cols - 1) * margin) / cols;
        int cardHeight = (int) (cardWidth * 0.65);

        for (int i = 0; i < cards.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int x = 20 + col * (cardWidth + margin);
            int y = startY + row * (cardHeight + margin) - (int) scrollHandler.getScrollOffset();
            cards.get(i).setBounds(x, y, cardWidth, cardHeight);
        }

        int totalRows = (int) Math.ceil((double) cards.size() / cols);
        int totalHeight = totalRows * (cardHeight + margin);
        double maxScroll = Math.max(0, totalHeight - (this.height - startY - 20));
        scrollHandler.updateMaxScroll(maxScroll);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (selectedCard != null) return true;
        if (scrollHandler.handleMouseScrolled(scrollY, 25.0)) {
            recalculateLayout();
            return true;
        }
        float scale = GuiScale.scaleModifier(this.minecraft);
        return super.mouseScrolled(mouseX / scale, mouseY / scale, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float scale = GuiScale.scaleModifier(this.minecraft);
        double scaledX = mouseX / scale;
        double scaledY = mouseY / scale;

        if (selectedCard != null) {
            if (viewButton.mouseClicked(scaledX, scaledY, button)) return true;
            if (deleteButton.mouseClicked(scaledX, scaledY, button)) return true;

            if (LucidConfig.screenshotUploadEnabled) {
                if (uploadButton.mouseClicked(scaledX, scaledY, button)) return true;

                if (selectedCard.getUploadState() == ScreenshotCard.UploadState.UPLOADED && selectedCard.getUploadUrl() != null) {
                    String url = selectedCard.getUploadUrl();
                    int textX = previewX + 168;
                    int textY = uploadRowY + 6;
                    int textWidth = this.font.width(url);
                    int textHeight = this.font.lineHeight;

                    if (scaledX >= textX && scaledX <= textX + textWidth && scaledY >= textY && scaledY <= textY + textHeight) {
                        selectedCard.copyUploadLinkToClipboard();
                        return true;
                    }
                }
            }

            if (closeButton.mouseClicked(scaledX, scaledY, button)) return true;

            if (scaledX < previewX || scaledX > previewX + previewWidth || scaledY < previewY || scaledY > previewY + previewHeight) {
                selectedCard = null;
            }
            return true;
        }

        if (scrollHandler.handleMouseDown(scaledX, scaledY, this.width, 40, this.height - 40)) {
            return true;
        }
        for (ScreenshotCard card : cards) {
            if (card.getY() + card.getHeight() >= 40 && card.getY() <= this.height) {
                if (card.mouseClicked(scaledX, scaledY)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(scaledX, scaledY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scrollHandler.setDragging(false);
        float scale = GuiScale.scaleModifier(this.minecraft);
        return super.mouseReleased(mouseX / scale, mouseY / scale, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        float scale = GuiScale.scaleModifier(this.minecraft);
        double scaledY = mouseY / scale;
        if (scrollHandler.handleMouseDragged(scaledY, 40, this.height - 40)) {
            recalculateLayout();
            return true;
        }
        return super.mouseDragged(mouseX / scale, mouseY / scale, button, dragX / scale, dragY / scale);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        float scale = GuiScale.scaleModifier(this.minecraft);
        super.mouseMoved(mouseX / scale, mouseY / scale);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        float scale = GuiScale.scaleModifier(this.minecraft);
        boolean focused = this.minecraft.isWindowActive();
        int scaledMouseX = focused ? (int) (mouseX / scale) : Integer.MIN_VALUE;
        int scaledMouseY = focused ? (int) (mouseY / scale) : Integer.MIN_VALUE;
        boolean modalOpen = selectedCard != null;
        int bgMouseX = modalOpen ? Integer.MIN_VALUE : scaledMouseX;
        int bgMouseY = modalOpen ? Integer.MIN_VALUE : scaledMouseY;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0F);

        this.renderBackground(guiGraphics, bgMouseX, bgMouseY, partialTick);

        int viewTop = 40;
        int viewBottom = this.height;
        for (ScreenshotCard card : cards) {
            if (card.getY() + card.getHeight() >= viewTop && card.getY() <= viewBottom) {
                card.render(guiGraphics, bgMouseX, bgMouseY);
            } else {
                card.unloadTexture();
            }
        }

        guiGraphics.fill(0, 0, this.width, 34, 0x66000000);

        scrollHandler.renderScrollbar(guiGraphics, this.width, 40, this.height - 40);

        if (System.currentTimeMillis() < statusTimer) {
            guiGraphics.drawCenteredString(this.font, statusMessage, this.width / 2, this.height - 25, 0x00FF00);
        }

        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0F);

        renderTopMenu(guiGraphics, bgMouseX, bgMouseY, partialTick);

        if (modalOpen) {
            renderPreviewModal(guiGraphics, scaledMouseX, scaledMouseY, partialTick);
        }

        guiGraphics.pose().popPose();
    }

    private void renderTopMenu(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300);

        for (var listener : this.children()) {
            if (listener instanceof Renderable renderable) {
                renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        guiGraphics.pose().popPose();
    }

    private void renderPreviewModal(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500);

        guiGraphics.fill(0, 0, this.width, this.height, 0xD0000000);

        guiGraphics.fill(previewX - 2, previewY - 2, previewX + previewWidth + 2, previewY + previewHeight + 2, 0xFFFFFFFF);
        if (selectedCard.getTextureLocation() != null) {
            guiGraphics.blit(selectedCard.getTextureLocation(), previewX, previewY, 0, 0, previewWidth, previewHeight, previewWidth, previewHeight);
        }

        guiGraphics.fill(previewX, previewY + previewHeight - 20, previewX + previewWidth, previewY + previewHeight, 0x99000000);
        String info = Component.translatable("gui.lucidgallery.modal.info",
                selectedCard.getMetaDate(), selectedCard.getMetaSize(), selectedCard.getMetaResolution()).getString();
        guiGraphics.drawString(this.font, info, previewX + 5, previewY + previewHeight - 14, 0xFFFFFF, true);

        viewButton.render(guiGraphics, mouseX, mouseY, partialTick);
        deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
        closeButton.render(guiGraphics, mouseX, mouseY, partialTick);

        if (LucidConfig.screenshotUploadEnabled) {
            updateUploadButtonLabel();
            uploadButton.render(guiGraphics, mouseX, mouseY, partialTick);

            if (selectedCard.getUploadState() == ScreenshotCard.UploadState.UPLOADED && selectedCard.getUploadUrl() != null) {
                guiGraphics.drawString(this.font, selectedCard.getUploadUrl(), previewX + 168, uploadRowY + 6, 0xFF55FFFF, false);
            } else if (selectedCard.getUploadState() == ScreenshotCard.UploadState.ERROR && selectedCard.getUploadError() != null) {
                guiGraphics.drawString(this.font, selectedCard.getUploadError(), previewX + 168, uploadRowY + 6, 0xFFFF5555, false);
            }
        }

        guiGraphics.pose().popPose();
    }

    private void handleUploadButtonClick() {
        if (selectedCard == null) return;
        switch (selectedCard.getUploadState()) {
            case UPLOADED -> selectedCard.copyUploadLinkToClipboard();
            case IDLE, ERROR -> selectedCard.startUpload();
            case UPLOADING -> { }
        }
    }

    private void updateUploadButtonLabel() {
        if (selectedCard == null) return;

        switch (selectedCard.getUploadState()) {
            case IDLE -> {
                LucidUploader.RejectReason reason = LucidUploader.canUpload();
                if (reason == LucidUploader.RejectReason.COOLDOWN) {
                    uploadButton.setMessage(Component.translatable("gui.lucidgallery.upload.button.cooldown"));
                    uploadButton.active = false;
                } else if (reason == LucidUploader.RejectReason.BUSY) {
                    uploadButton.setMessage(Component.translatable("gui.lucidgallery.upload.button.uploading"));
                    uploadButton.active = false;
                } else {
                    uploadButton.setMessage(Component.translatable("gui.lucidgallery.upload.button.upload"));
                    uploadButton.active = true;
                }
            }
            case UPLOADING -> {
                uploadButton.setMessage(Component.translatable("gui.lucidgallery.upload.button.uploading"));
                uploadButton.active = false;
            }
            case UPLOADED -> {
                uploadButton.setMessage(Component.translatable("gui.lucidgallery.upload.button.copy_link"));
                uploadButton.active = true;
            }
            case ERROR -> {
                uploadButton.setMessage(Component.translatable("gui.lucidgallery.upload.button.retry"));
                uploadButton.active = true;
            }
        }
    }

    private void showStatus(String msg) {
        this.statusMessage = msg;
        this.statusTimer = System.currentTimeMillis() + 2500;
    }

    @Override
    public void onClose() {
        TextureLoader.clearCache();
        super.onClose();
    }

    private class ScaleSlider extends AbstractSliderButton {
        public ScaleSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), getInitialValue());
            this.updateMessage();
        }

        private static double getInitialValue() {
            int maxScale = GuiScale.maxSupportedScale(Minecraft.getInstance());
            int current = Mth.clamp(LucidConfig.customGuiScale, 0, maxScale);
            return maxScale > 0 ? (double) current / maxScale : 0.0;
        }

        @Override
        protected void updateMessage() {
            int maxScale = GuiScale.maxSupportedScale(Minecraft.getInstance());
            int current = (int) Math.round(this.value * maxScale);
            if (current == 0) {
                this.setMessage(Component.translatable("gui.lucidgallery.scale_auto"));
            } else {
                this.setMessage(Component.translatable("gui.lucidgallery.scale_label", current));
            }
        }

        @Override
        protected void applyValue() {
            this.updateMessage();
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            int maxScale = GuiScale.maxSupportedScale(Minecraft.getInstance());
            int current = (int) Math.round(this.value * maxScale);

            this.value = maxScale > 0 ? (double) current / maxScale : 0.0;
            this.updateMessage();

            if (LucidConfig.customGuiScale != current) {
                ConfigManager.updateAndSave(LucidConfig.class, "customGuiScale", current);
                GalleryScreen.this.init(Minecraft.getInstance(), Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
            }
        }
    }
}