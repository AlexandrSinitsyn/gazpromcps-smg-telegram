package gazpromcps.smg.entity;

import java.util.Arrays;

public enum Role {
    ANONYMOUS(new String[][]{
            {"register"},
    }),
    USER(new String[][]{
            {"make-report"},
            {"update-report"},
    }),
    MANAGER(new String[][]{
            {"export-xlsx", "export-csv", "export-text"},
            {"export-media"},
            {"import-xlsx"},
    }),
    SERVICE(new String[][]{
            {"export-xlsx", "export-csv", "export-text"},
            {"export-media"},
            {"import-xlsx"},
            {"token"}
    }),
    ADMIN(new String[][]{
            {"make-report", "update-report"},
            {"export-xlsx", "export-csv", "export-text"},
            {"export-media"},
            {"import-xlsx"},
            {"promote", "users", "token"},
    }),
    SUPERUSER(new String[][]{
            {"start", "register"},
            {"make-report", "update-report"},
            {"export-xlsx", "export-csv", "export-text"},
            {"export-media"},
            {"import-xlsx"},
            {"promote", "users", "token"},
            {"reload"},
            {"ru", "en", "lang"},
            {"help"},
    });

    private static final boolean LANGUAGES = false;

    private final String[][] availableButtons;

    Role(final String[][] availableButtons) {
        this.availableButtons = availableButtons;
    }

    public String[][] getAvailableButtons() {
        final int length = availableButtons.length;

        final String[][] res;
        if (this != SUPERUSER) {
            res = Arrays.copyOf(availableButtons, length + 2);
            res[length] = LANGUAGES ? new String[]{"ru", "en", "lang"} : new String[]{};
            res[length + 1] = new String[]{"help"};
        } else {
            res = availableButtons;
        }

        return res;
    }

    public boolean isAllowed(final Role min) {
        return min.ordinal() <= this.ordinal();
    }
}
