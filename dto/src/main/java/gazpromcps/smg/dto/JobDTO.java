package gazpromcps.smg.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobDTO implements DTO {
    private final long id;
    private final String stage;
    private final String master;
    private final String object;
    private final String title;
    private final boolean active;
}
