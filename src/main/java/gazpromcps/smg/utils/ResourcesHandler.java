package gazpromcps.smg.utils;

import lombok.Getter;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class ResourcesHandler {
    private ResourceBundle bundle;
    private final String base = "i18n/MessageBundle";
    @Getter
    private Locale locale;

    public ResourcesHandler(final Locale locale) {
        this.locale = locale;
        bundle = ResourceBundle.getBundle(base, locale);
    }

    public void changeLocale(final Locale locale) {
        try {
            this.locale = locale;
            bundle = ResourceBundle.getBundle(base, locale);
        } catch (final MissingResourceException ignored) {
            translate(locale);

            changeLocale(locale);
        }
    }

    private void translate(final Locale locale) {
        throw new RuntimeException("!!!!!!!!!! UNSUPPORTED `/lang` !!!!!!!!!!");
    }

    public String variable(final String key) {
        return bundle.getString(key);
    }
}
