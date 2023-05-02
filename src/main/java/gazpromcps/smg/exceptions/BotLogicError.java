package gazpromcps.smg.exceptions;

import gazpromcps.smg.controller.BotController;
import lombok.Getter;

public class BotLogicError extends Error {
    @Getter
    private final BotController bot;

    public BotLogicError(final BotController bot, final String message) {
        super(message);
        this.bot = bot;
    }
}
