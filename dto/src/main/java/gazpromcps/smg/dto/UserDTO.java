package gazpromcps.smg.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDTO implements DTO {
    private final long id;
    private final String name;
}
