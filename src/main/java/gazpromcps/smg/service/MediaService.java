package gazpromcps.smg.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class MediaService {
    private static final Path media = Paths.get("./media");

    static {
        try {
            Files.createDirectories(media);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("resource")
    public File getZip() throws IOException {
        final Path zipFile = media.resolve("media.zip");

        try (final var zipper = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            for (final var file : Files.list(media).toList()) {
                if (file.toString().endsWith(".zip")) {
                    continue;
                }

                zipper.putNextEntry(new ZipEntry(file.toFile().getName()));
                Files.copy(file, zipper);
            }

            zipper.closeEntry();

            return zipFile.toFile();
        }
    }

    public File saveMedia(final String filename) {
        return media.resolve(filename).toFile();
    }
}
