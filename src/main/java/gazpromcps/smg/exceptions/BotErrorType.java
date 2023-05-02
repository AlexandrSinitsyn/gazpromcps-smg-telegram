package gazpromcps.smg.exceptions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum BotErrorType {
    UNKNOWN_ERROR("unknown-error"),
    UNKNOWN_COMMAND("unknown-command"),
    NOT_ALLOWED("not-allowed"),
    UNWELCOME("unwelcome"),
    INVALID_TOKEN("invalid-token"),
    INVALID_REQUEST_SEQUENCE("invalid-request-sequence"),
    INVALID_COMMAND_USAGE("invalid-command-usage"),
    INVALID_COUNT("invalid-count"),
    INVALID_INPUT_FORMAT("invalid-input-format"),
    INVALID_NUMBER("invalid-number"),
    JOB_NOT_FOUND("job-not-found"),
    UNSUPPORTED_FILE_EXTENSION("unsupported-file-extension");

    @Getter
    private final String message;
}
