package gazpromcps.smg.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class CompletedJobDTO implements DTO {
    private final long id;
    private final JobDTO job;
    private final double count;
    private final UserDTO user;
    private final Date creationTime;
}
