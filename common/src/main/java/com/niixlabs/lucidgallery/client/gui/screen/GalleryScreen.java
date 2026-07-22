package com.niixlabs.lucidgallery.client.gui.screen;

import com.niixlabs.lucidgallery.client.gui.card.ScreenshotCard;
import com.niixlabs.lucidgallery.client.gui.util.GuiScale;
import com.niixlabs.lucidgallery.client.gui.util.CatboxUploader;
import com.niixlabs.lucidgallery.client.gui.util.TextureLoader;
import com.niixlabs.lucidgallery.client.gui.util.LucidScrollHandler;
import com.niixlabs.lucidgallery.config.ConfigManager;
import com.niixlabs.lucidgallery.config.LucidConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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
    private boolean isDropdownOpen = false;
    private final int dropdownWidth = 60;
    private final int dropdownHeight = 16;
    private int dropdownX;
    private int dropdownY;

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
        this.dropdownX = this.width - dropdownWidth - 10;
        this.dropdownY = 10;

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
                .bounds(previewX, uploadRowY, 160, 20).build();
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
        if (isDropdownOpen || selectedCard != null) return true;
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
            if (LucidConfig.screenshotUploadEnabled && uploadButton.mouseClicked(scaledX, scaledY, button)) return true;
            if (closeButton.mouseClicked(scaledX, scaledY, button)) return true;
            if (scaledX < previewX || scaledX > previewX + previewWidth || scaledY < previewY || scaledY > previewY + previewHeight) {
                selectedCard = null;
            }
            return true;
        }

        if (scaledX >= dropdownX && scaledX <= dropdownX + dropdownWidth) {
            if (scaledY >= dropdownY && scaledY <= dropdownY + dropdownHeight) {
                isDropdownOpen = !isDropdownOpen;
                return true;
            }
            if (isDropdownOpen) {
                List<Integer> options = GuiScale.supportedScaleOptions(this.minecraft);
                for (int i = 0; i < options.size(); i++) {
                    int optY = dropdownY + dropdownHeight + (i * dropdownHeight);
                    if (scaledY >= optY && scaledY <= optY + dropdownHeight) {
                        ConfigManager.updateAndSave(LucidConfig.class, "customGuiScale", options.get(i));
                        isDropdownOpen = false;
                        this.init(this.minecraft, this.minecraft.getWindow().getGuiScaledWidth(), this.minecraft.getWindow().getGuiScaledHeight());
                        return true;
                    }
                }
            }
        }
        if (isDropdownOpen) {
            isDropdownOpen = false;
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

        String currentLabel = LucidConfig.customGuiScale == 0
                ? Component.translatable("gui.lucidgallery.scale_auto").getString()
                : Component.translatable("gui.lucidgallery.scale_label", LucidConfig.customGuiScale).getString();
        boolean btnHovered = mouseX >= dropdownX && mouseX <= dropdownX + dropdownWidth && mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight;

        guiGraphics.fill(dropdownX, dropdownY, dropdownX + dropdownWidth, dropdownY + dropdownHeight, btnHovered ? 0xFF555555 : 0xFF222222);
        guiGraphics.drawCenteredString(this.font, currentLabel, dropdownX + dropdownWidth / 2, dropdownY + 4, 0xFFFFFFFF);
        guiGraphics.renderOutline(dropdownX, dropdownY, dropdownWidth, dropdownHeight, 0xFF000000);

        if (isDropdownOpen) {
            List<Integer> options = GuiScale.supportedScaleOptions(this.minecraft);
            for (int i = 0; i < options.size(); i++) {
                int optY = dropdownY + dropdownHeight + (i * dropdownHeight);
                boolean optHovered = mouseX >= dropdownX && mouseX <= dropdownX + dropdownWidth && mouseY >= optY && mouseY <= optY + dropdownHeight;
                String optLabel = options.get(i) == 0
                        ? Component.translatable("gui.lucidgallery.scale_auto").getString()
                        : String.valueOf(options.get(i));

                guiGraphics.fill(dropdownX, optY, dropdownX + dropdownWidth, optY + dropdownHeight, optHovered ? 0xFF666666 : 0xFF333333);
                guiGraphics.drawCenteredString(this.font, optLabel, dropdownX + dropdownWidth / 2, optY + 4, 0xFFFFFFFF);
                guiGraphics.renderOutline(dropdownX, optY, dropdownWidth, dropdownHeight, 0xFF000000);
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
            case UPLOADING -> { /* ignore extra clicks */ }
        }
    }

    private void updateUploadButtonLabel() {
        if (selectedCard == null) return;

        switch (selectedCard.getUploadState()) {
            case IDLE -> {
                CatboxUploader.RejectReason reason = CatboxUploader.canUpload();
                if (reason == CatboxUploader.RejectReason.COOLDOWN) {
                    uploadButton.setMessage(Component.translatable("gui.lucidgallery.upload.button.cooldown"));
                    uploadButton.active = false;
                } else if (reason == CatboxUploader.RejectReason.BUSY) {
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
}