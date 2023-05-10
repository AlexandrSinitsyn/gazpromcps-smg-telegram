package gazpromcps.smg.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageDTO implements DTO {
    public enum MessageType implements DTO {
        TEXT, BUTTON_RESPONSE
    }

    private final String message;
    private final UserDTO user;
    private final MessageType type;
}
