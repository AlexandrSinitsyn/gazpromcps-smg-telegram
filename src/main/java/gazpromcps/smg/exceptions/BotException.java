package gazpromcps.smg.exceptions;

import gazpromcps.smg.controller.BotController;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public final class BotException extends RuntimeException {
    private final BotController bot;
    private final BotErrorType type;
    private Throwable cause = null;
}
