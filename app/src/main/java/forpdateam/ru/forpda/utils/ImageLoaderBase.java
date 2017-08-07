package forpdateam.ru.forpda.utils;

import android.support.annotation.NonNull;

import forpdateam.ru.forpda.views.IImageLoader;
import forpdateam.ru.forpda.views.widgets.AvatarPlaceholder;
import forpdateam.ru.forpda.views.widgets.AvatarView;

public abstract class ImageLoaderBase implements IImageLoader {

    private String defaultPlaceholderString;

    public ImageLoaderBase() {
        this.defaultPlaceholderString = AvatarPlaceholder.DEFAULT_PLACEHOLDER_STRING;
    }

    public ImageLoaderBase(String defaultPlaceholderString) {
        this.defaultPlaceholderString = defaultPlaceholderString;
    }

    @Override
    public void loadImage(@NonNull AvatarView avatarView, String avatarUrl, String name) {
        loadImage(avatarView, new AvatarPlaceholder(name, defaultPlaceholderString), avatarUrl);
    }

    @Override
    public void loadImage(@NonNull AvatarView avatarView, String avatarUrl, String name, int textSizePercentage) {
        loadImage(avatarView, new AvatarPlaceholder(name, textSizePercentage, defaultPlaceholderString), avatarUrl);
    }
}
