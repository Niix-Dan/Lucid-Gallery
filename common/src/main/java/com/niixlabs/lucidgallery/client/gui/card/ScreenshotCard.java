package com.niixlabs.lucidgallery.client.gui.card;

import com.niixlabs.lucidgallery.client.gui.util.LucidUploader;
import com.niixlabs.lucidgallery.client.gui.util.TextureLoader;
import com.niixlabs.lucidgallery.config.LucidConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.function.Consumer;

public class ScreenshotCard {

    public enum UploadState { IDLE, UPLOADING, UPLOADED, ERROR }

    private final File file;
    private final Consumer<String> statusCallback;
    private final Runnable refreshCallback;
    private final Consumer<ScreenshotCard> selectCallback;
    private ResourceLocation textureLocation;
    private int x;
    private int y;
    private int width;
    private int height;
    private String metaDate = "";
    private String metaSize = "";
    private String metaResolution = "";
    private boolean metaLoaded = false;

    private UploadState uploadState = UploadState.IDLE;
    private String uploadUrl = null;
    private String uploadError = null;

    public ScreenshotCard(File file, Consumer<String> statusCallback, Runnable refreshCallback, Consumer<ScreenshotCard> selectCallback) {
        this.file = file;
        this.statusCallback = statusCallback;
        this.refreshCallback = refreshCallback;
        this.selectCallback = selectCallback;

        String cachedUrl = LucidUploader.getCachedUrl(file);
        if (cachedUrl != null) {
            this.uploadUrl = cachedUrl;
            this.uploadState = UploadState.UPLOADED;
        }
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void loadMetadata() {
        if (metaLoaded) return;
        try {
            this.metaDate = new SimpleDateFormat(Component.translatable("gui.lucidgallery.modal.info.date_format").getString()).format(file.lastModified());
            this.metaSize = Component.translatable("gui.lucidgallery.modal.info.size_format", file.length() / 1024).getString();
            Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix("png");
            if (iter.hasNext()) {
                ImageReader reader = iter.next();
                try (ImageInputStream stream = ImageIO.createImageInputStream(file)) {
                    reader.setInput(stream);
                    this.metaResolution = Component.translatable("gui.lucidgallery.modal.info.res_format", reader.getWidth(reader.getMinIndex()), reader.getHeight(reader.getMinIndex())).getString();
                } finally {
                    reader.dispose();
                }
            } else {
                this.metaResolution = "Unknown";
            }
        } catch (Exception e) {
            this.metaResolution = "Error";
            this.metaSize = "Error";
        }
        this.metaLoaded = true;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (textureLocation == null) {
            textureLocation = TextureLoader.getOrLoadTexture(file);
        }

        boolean isHovered = isMouseOver(mouseX, mouseY);
        int bgColor = isHovered ? 0xFFFFFFFF : LucidConfig.cardBackgroundColor;
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, bgColor);

        if (textureLocation != null) {
            guiGraphics.blit(textureLocation, x, y, 0, 0, width, height - 14, width, height - 14);
        } else {
            guiGraphics.fill(x, y, x + width, y + height - 14, 0xFF333333);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("gui.lucidgallery.loading").getString(), x + (width / 2), y + ((height - 14) / 2) - 4, 0xFFAAAAAA);
        }

        String name = file.getName();
        if (name.length() > 18) {
            name = name.substring(0, 15) + "...";
        }
        guiGraphics.drawString(Minecraft.getInstance().font, name, x + 4, y + height - 10, 0xFFE0E0E0, false);

        boolean hoverDel = isOverDelete(mouseX, mouseY);
        guiGraphics.fill(x + width - 22, y + 4, x + width - 4, y + 22, hoverDel ? 0xFFFF5555 : 0xAA000000);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("gui.lucidgallery.delete_button").getString(), x + width - 13, y + 9, hoverDel ? 0xFFFFFFFF : 0xFFDDDDDD);

        boolean hoverOpen = isOverOpen(mouseX, mouseY);
        guiGraphics.fill(x + width - 44, y + 4, x + width - 26, y + 22, hoverOpen ? 0xFF5555FF : 0xAA000000);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("gui.lucidgallery.open_button").getString(), x + width - 35, y + 9, hoverOpen ? 0xFFFFFFFF : 0xFFDDDDDD);

        if (LucidConfig.screenshotUploadEnabled && uploadState == UploadState.UPLOADED) {
            guiGraphics.fill(x + 4, y + 4, x + 20, y + 16, 0xAA00AA55);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, "\u2601", x + 12, y + 7, 0xFFFFFFFF);
        }
    }

    // MUDANÇA PRINCIPAL AQUI
    public void unloadTexture() {
        // Verifica se realmente tem uma textura antes de tentar deletar
        // Isso evita ficar chamando a limpeza do TextureLoader 60x por segundo à toa
        if (this.textureLocation != null) {
            TextureLoader.unloadTexture(this.file);
            this.textureLocation = null;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        if (isOverDelete(mouseX, mouseY)) {
            try {
                Files.delete(file.toPath());
                statusCallback.accept(Component.translatable("gui.lucidgallery.action.delete_success").getString());
                refreshCallback.run();
            } catch (Exception e) {
                statusCallback.accept(Component.translatable("gui.lucidgallery.action.delete_fail").getString());
            }
            return true;
        }

        if (isOverOpen(mouseX, mouseY)) {
            Util.getPlatform().openFile(getFile());
            return true;
        }

        loadMetadata();
        selectCallback.accept(this);
        return true;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        if (mouseY < 40) {
            return false;
        }
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private boolean isOverDelete(double mouseX, double mouseY) {
        if (mouseY < 40) return false;
        return mouseX >= x + width - 22 && mouseX <= x + width - 4 && mouseY >= y + 4 && mouseY <= y + 22;
    }

    private boolean isOverOpen(double mouseX, double mouseY) {
        if (mouseY < 40) return false;
        return mouseX >= x + width - 44 && mouseX <= x + width - 26 && mouseY >= y + 4 && mouseY <= y + 22;
    }

    public int getY() {
        return y;
    }

    public int getHeight() {
        return height;
    }

    public File getFile() {
        return file;
    }

    public ResourceLocation getTextureLocation() {
        return textureLocation;
    }

    public String getMetaDate() {
        return metaDate;
    }

    public String getMetaSize() {
        return metaSize;
    }

    public String getMetaResolution() {
        return metaResolution;
    }

    public UploadState getUploadState() {
        return uploadState;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public String getUploadError() {
        return uploadError;
    }

    public void startUpload() {
        if (uploadState == UploadState.UPLOADING) {
            return;
        }

        String cached = LucidUploader.getCachedUrl(file);
        if (cached != null) {
            this.uploadUrl = cached;
            this.uploadState = UploadState.UPLOADED;
            return;
        }

        LucidUploader.RejectReason reason = LucidUploader.canUpload();
        if (reason != LucidUploader.RejectReason.NONE) {
            statusCallback.accept(rejectMessage(reason));
            return;
        }

        this.uploadState = UploadState.UPLOADING;
        LucidUploader.uploadAsync(file, (url, error) -> {
            if (error != null) {
                this.uploadState = UploadState.ERROR;
                this.uploadError = error;
                statusCallback.accept(Component.translatable("gui.lucidgallery.upload.status.fail").getString());
            } else {
                this.uploadState = UploadState.UPLOADED;
                this.uploadUrl = url;
                statusCallback.accept(Component.translatable("gui.lucidgallery.upload.status.success").getString());
            }
        });
    }

    public void copyUploadLinkToClipboard() {
        if (uploadUrl == null) return;
        Minecraft.getInstance().keyboardHandler.setClipboard(uploadUrl);
        statusCallback.accept(Component.translatable("gui.lucidgallery.upload.status.copied").getString());
    }

    private String rejectMessage(LucidUploader.RejectReason reason) {
        return switch (reason) {
            case COOLDOWN -> Component.translatable("gui.lucidgallery.upload.status.cooldown").getString();
            case BUSY -> Component.translatable("gui.lucidgallery.upload.status.busy").getString();
            default -> "";
        };
    }
}