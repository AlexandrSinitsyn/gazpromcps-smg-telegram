package gazpromcps.smg.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ServerResponse implements DTO {
    private enum ServerResponseType {
        SUCCESS, REJECT, ERROR
    }
    private record Button(String name, String callback) {}

    private final ServerResponseType type;
    private final String message;
    private final List<List<Button>> buttons;
}
