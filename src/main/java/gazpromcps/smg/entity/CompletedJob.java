package gazpromcps.smg.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Entity
@Table(name = "completed")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletedJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "job_id")
    @OrderBy(value = "creationTime desc")
    private Job job;

    @NotNull
    private double count;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    private Timestamp creationTime;

    public static List<String> header() {
        return Stream.concat(Job.header().stream(),
                Stream.of("user", "count", "date")).toList();
    }

    public List<String> separateString() {
        return Stream.concat(job.stringRepresentation().stream(),
                Stream.of(user.getName(),
                        NumberFormat.getInstance(Locale.getDefault()).format(count),
                        DateFormat.getDateInstance(DateFormat.SHORT,
                                Locale.getDefault()).format(creationTime))).toList();
    }

    public List<String> minimalString() {
        return List.of(job.getStage(), job.getMaster(), job.getObject(), job.getTitle(), job.getMeasurement(),
                NumberFormat.getInstance(Locale.getDefault()).format(count));
    }
}
