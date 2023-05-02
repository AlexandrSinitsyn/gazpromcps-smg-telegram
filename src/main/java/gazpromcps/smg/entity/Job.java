package gazpromcps.smg.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "job")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @NotBlank
    private String stage;

    @NotNull
    @NotBlank
    private String master;

    @NotNull
    @NotBlank
    private String object;

    @NotNull
    @NotBlank
    private String title;

    @NotNull
    @NotBlank
    private String measurement;

    @NotNull
    @ColumnDefault("true")
    private boolean active;

    @CreationTimestamp
    private Timestamp creationTime;

    public static List<String> header() {
        return List.of("stage", "master", "object", "title", "measurement");
    }

    public List<String> stringRepresentation() {
        return List.of(stage, master, object, title, measurement);
    }
}
