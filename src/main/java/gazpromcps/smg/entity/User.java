package gazpromcps.smg.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Locale;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Component
public class User {
    @Id
    @NotNull
    private long id;

    @NotNull
    @NotBlank
    private String name;

    @NotNull
    private Role role;

    @NotNull
    private long chatId;

    @CreationTimestamp
    private Timestamp creationTime;

    @SneakyThrows
    public synchronized String generateToken(final String salt) {
        final MessageDigest digest = MessageDigest.getInstance("SHA3-256");
        digest.update(salt.getBytes(StandardCharsets.UTF_8));
        final byte[] generated = digest.digest("""
                id: %d
                name: %s
                role: %s
                chatId: %d
                """.trim().formatted(id, name, role.name(), chatId).getBytes(StandardCharsets.UTF_8));

        final String f = Character.toUpperCase(name.charAt(0)) + String.valueOf(chatId);
        final String s = String.valueOf(Math.abs(Base64.getEncoder().encodeToString(generated).hashCode()));
        final StringBuilder res = new StringBuilder();
        for (int i = 0; i < f.length(); i++) {
            res.append(f.charAt(i)).append(s.charAt(i >= s.length() ? 0 : i));
        }

        return res.toString();
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "[`%9s`] -- `%10d` -- %s", role.name(), id, name);
    }
}
