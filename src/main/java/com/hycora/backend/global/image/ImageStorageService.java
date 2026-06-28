package com.hycora.backend.global.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
public class ImageStorageService {

    static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    static final int MAX_ACTIVITY_IMAGES = 10;
    private static final long MAX_DECODED_PIXELS = 40_000_000L;
    private static final Pattern LEADER_NAME_PATTERN = Pattern.compile(
            "^[\\p{L}\\p{M}\\p{N}](?:[\\p{L}\\p{M}\\p{N} ·'’-]{0,98}[\\p{L}\\p{M}\\p{N}])?$"
    );

    private final Path uploadRoot;

    public ImageStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public StoredImage storeLeaderPhoto(String name, MultipartFile file) {
        String safeName = validateLeaderName(name);
        Path leadersDirectory = resolveUnder(uploadRoot, "leaders");
        requireBaseDirectory(leadersDirectory);

        BufferedImage resized = validateAndResize(file, 500, 500);
        Path target = resolveUnder(leadersDirectory, safeName + ".jpg");
        writeAtomically(resized, target);

        return new StoredImage(safeName, leaderImageUrl(safeName));
    }

    public String getLeaderPhotoUrl(String name) {
        String safeName = validateLeaderName(name);
        Path target = resolveUnder(resolveUnder(uploadRoot, "leaders"), safeName + ".jpg");
        if (!Files.isRegularFile(target)) {
            throw ImageUploadException.notFound("임원진 사진을 찾을 수 없습니다.");
        }
        return leaderImageUrl(safeName);
    }

    public void deleteLeaderPhoto(String name) {
        String safeName = validateLeaderName(name);
        Path target = resolveUnder(resolveUnder(uploadRoot, "leaders"), safeName + ".jpg");
        deleteExistingFile(target, "임원진 사진을 찾을 수 없습니다.");
    }

    public List<StoredImage> storeActivityImages(Long activityId, List<MultipartFile> files) {
        validateActivityId(activityId);
        if (files == null || files.isEmpty()) {
            throw ImageUploadException.invalid("이미지를 최소 1개 업로드해야 합니다.");
        }
        if (files.size() > MAX_ACTIVITY_IMAGES) {
            throw ImageUploadException.invalid("활동 이미지는 최대 10개까지 업로드할 수 있습니다.");
        }

        Path activitiesDirectory = resolveUnder(uploadRoot, "activities");
        requireBaseDirectory(activitiesDirectory);
        Path activityDirectory = resolveUnder(activitiesDirectory, activityId.toString());
        try {
            Files.createDirectories(activityDirectory);
        } catch (IOException exception) {
            throw ImageUploadException.storage("활동 이미지 디렉터리를 생성할 수 없습니다.", exception);
        }

        List<StoredImage> storedImages = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                BufferedImage resized = validateAndResize(file, 1200, 900);
                UUID imageId = UUID.randomUUID();
                Path target = resolveUnder(activityDirectory, imageId + ".jpg");
                writeAtomically(resized, target);
                storedImages.add(new StoredImage(imageId.toString(), activityImageUrl(activityId, imageId)));
            }
            return List.copyOf(storedImages);
        } catch (RuntimeException exception) {
            cleanupActivityImages(activityId, storedImages.stream().map(StoredImage::id).toList());
            throw exception;
        }
    }

    public boolean activityImageExists(Long activityId, UUID imageId) {
        Path target = activityImagePath(activityId, imageId);
        return Files.isRegularFile(target);
    }

    public void deleteActivityImage(Long activityId, UUID imageId) {
        Path target = activityImagePath(activityId, imageId);
        deleteExistingFile(target, "활동 이미지를 찾을 수 없습니다.");
        removeEmptyActivityDirectory(target.getParent());
    }

    public void cleanupActivityImages(Long activityId, List<String> imageIds) {
        for (String imageId : imageIds) {
            try {
                UUID uuid = UUID.fromString(imageId);
                Files.deleteIfExists(activityImagePath(activityId, uuid));
            } catch (IllegalArgumentException | IOException exception) {
                log.warn("업로드 실패 파일 정리에 실패했습니다. activityId={}, imageId={}", activityId, imageId, exception);
            }
        }
        Path activityDirectory = resolveUnder(resolveUnder(uploadRoot, "activities"), activityId.toString());
        removeEmptyActivityDirectory(activityDirectory);
    }

    public void deleteActivityDirectory(Long activityId) {
        validateActivityId(activityId);
        Path activityDirectory = resolveUnder(resolveUnder(uploadRoot, "activities"), activityId.toString());
        if (!Files.exists(activityDirectory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(activityDirectory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        } catch (IOException | UncheckedIOException exception) {
            log.warn("활동 삭제 중 이미지 디렉터리 정리에 실패했습니다. activityId={}", activityId, exception);
        }
    }

    public String validateLeaderName(String name) {
        if (!StringUtils.hasText(name) || !name.equals(name.trim())) {
            throw ImageUploadException.invalid("임원진 이름이 올바르지 않습니다.");
        }
        if (name.contains("..") || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0
                || name.chars().anyMatch(Character::isISOControl)) {
            throw ImageUploadException.invalid("임원진 이름에 허용되지 않는 문자가 포함되어 있습니다.");
        }

        String normalized = Normalizer.normalize(name, Normalizer.Form.NFC);
        if (!LEADER_NAME_PATTERN.matcher(normalized).matches()) {
            throw ImageUploadException.invalid("임원진 이름 형식이 올바르지 않습니다.");
        }
        return normalized;
    }

    private BufferedImage validateAndResize(MultipartFile file, int targetWidth, int targetHeight) {
        ValidatedImage validatedImage = validateFile(file);
        BufferedImage source = decode(validatedImage.bytes(), validatedImage.format());
        return centerCropAndResize(source, targetWidth, targetHeight);
    }

    private ValidatedImage validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ImageUploadException.invalid("빈 이미지 파일은 업로드할 수 없습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw ImageUploadException.tooLarge();
        }

        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename) || filename.contains("..")
                || filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0
                || filename.indexOf(':') >= 0 || filename.chars().anyMatch(Character::isISOControl)) {
            throw ImageUploadException.invalid("원본 파일명이 올바르지 않습니다.");
        }

        ImageFormat extensionFormat = ImageFormat.fromFilename(filename);
        ImageFormat mimeFormat = ImageFormat.fromMimeType(file.getContentType());
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw ImageUploadException.storage("업로드 파일을 읽을 수 없습니다.", exception);
        }
        ImageFormat byteFormat = ImageFormat.fromBytes(bytes);

        if (extensionFormat != mimeFormat || mimeFormat != byteFormat) {
            throw ImageUploadException.invalid("파일 확장자, MIME type, 실제 이미지 형식이 일치하지 않습니다.");
        }
        return new ValidatedImage(bytes, byteFormat);
    }

    private BufferedImage decode(byte[] bytes, ImageFormat expectedFormat) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw ImageUploadException.invalid("이미지 파일을 디코딩할 수 없습니다.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw ImageUploadException.invalid("지원하지 않거나 손상된 이미지 파일입니다.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                if (!expectedFormat.matchesReader(reader.getFormatName())) {
                    throw ImageUploadException.invalid("이미지 디코더 형식이 실제 파일 형식과 일치하지 않습니다.");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_DECODED_PIXELS) {
                    throw ImageUploadException.invalid("이미지 해상도가 너무 크거나 올바르지 않습니다.");
                }

                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw ImageUploadException.invalid("손상된 이미지 파일입니다.");
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (ImageUploadException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw ImageUploadException.invalid("지원하지 않거나 손상된 이미지 파일입니다.");
        }
    }

    private BufferedImage centerCropAndResize(BufferedImage source, int targetWidth, int targetHeight) {
        double sourceRatio = (double) source.getWidth() / source.getHeight();
        double targetRatio = (double) targetWidth / targetHeight;

        int sourceX = 0;
        int sourceY = 0;
        int cropWidth = source.getWidth();
        int cropHeight = source.getHeight();
        if (sourceRatio > targetRatio) {
            cropWidth = (int) Math.round(source.getHeight() * targetRatio);
            sourceX = (source.getWidth() - cropWidth) / 2;
        } else if (sourceRatio < targetRatio) {
            cropHeight = (int) Math.round(source.getWidth() / targetRatio);
            sourceY = (source.getHeight() - cropHeight) / 2;
        }

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, targetWidth, targetHeight);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(
                    source,
                    0, 0, targetWidth, targetHeight,
                    sourceX, sourceY, sourceX + cropWidth, sourceY + cropHeight,
                    null
            );
        } finally {
            graphics.dispose();
        }
        return resized;
    }

    private void writeAtomically(BufferedImage image, Path target) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(target.getParent(), ".image-", ".jpg");
            writeJpeg(image, temporary);
            try {
                Files.move(
                        temporary,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw ImageUploadException.storage("이미지 파일을 저장할 수 없습니다.", exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException exception) {
                    log.warn("임시 이미지 파일 정리에 실패했습니다. path={}", temporary, exception);
                }
            }
        }
    }

    private void writeJpeg(BufferedImage image, Path target) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("JPEG writer is not available");
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(target.toFile())) {
            if (output == null) {
                throw new IOException("JPEG output stream could not be created");
            }
            writer.setOutput(output);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            if (parameters.canWriteCompressed()) {
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parameters.setCompressionQuality(0.9f);
            }
            writer.write(null, new IIOImage(image, null, null), parameters);
        } finally {
            writer.dispose();
        }
    }

    private Path activityImagePath(Long activityId, UUID imageId) {
        validateActivityId(activityId);
        Path activitiesDirectory = resolveUnder(uploadRoot, "activities");
        Path activityDirectory = resolveUnder(activitiesDirectory, activityId.toString());
        return resolveUnder(activityDirectory, imageId + ".jpg");
    }

    private void deleteExistingFile(Path target, String notFoundMessage) {
        try {
            if (!Files.deleteIfExists(target)) {
                throw ImageUploadException.notFound(notFoundMessage);
            }
        } catch (ImageUploadException exception) {
            throw exception;
        } catch (IOException exception) {
            throw ImageUploadException.storage("이미지 파일을 삭제할 수 없습니다.", exception);
        }
    }

    private void removeEmptyActivityDirectory(Path directory) {
        try (Stream<Path> children = Files.list(directory)) {
            if (children.findAny().isEmpty()) {
                Files.deleteIfExists(directory);
            }
        } catch (IOException exception) {
            log.debug("빈 활동 이미지 디렉터리를 정리하지 못했습니다. path={}", directory, exception);
        }
    }

    private void requireBaseDirectory(Path directory) {
        if (!Files.isDirectory(directory)) {
            throw ImageUploadException.storage(
                    "기본 업로드 디렉터리가 준비되지 않았습니다.",
                    new IOException("Missing upload directory: " + directory)
            );
        }
    }

    private Path resolveUnder(Path base, String child) {
        Path normalizedBase = base.toAbsolutePath().normalize();
        Path target = normalizedBase.resolve(child).normalize();
        if (!target.startsWith(normalizedBase)) {
            throw ImageUploadException.invalid("안전하지 않은 파일 경로입니다.");
        }
        return target;
    }

    private void validateActivityId(Long activityId) {
        if (activityId == null || activityId <= 0) {
            throw ImageUploadException.invalid("활동 ID가 올바르지 않습니다.");
        }
    }

    private String leaderImageUrl(String name) {
        return "/uploads/leaders/" + UriUtils.encodePathSegment(name, StandardCharsets.UTF_8) + ".jpg";
    }

    public static String activityImageUrl(Long activityId, UUID imageId) {
        return "/uploads/activities/" + activityId + "/" + imageId + ".jpg";
    }

    public record StoredImage(String id, String imageUrl) {
    }

    private record ValidatedImage(byte[] bytes, ImageFormat format) {
    }

    private enum ImageFormat {
        JPEG(Set.of(".jpg", ".jpeg"), "image/jpeg", Set.of("jpeg", "jpg")),
        PNG(Set.of(".png"), "image/png", Set.of("png")),
        WEBP(Set.of(".webp"), "image/webp", Set.of("webp"));

        private final Set<String> extensions;
        private final String mimeType;
        private final Set<String> readerNames;

        ImageFormat(Set<String> extensions, String mimeType, Set<String> readerNames) {
            this.extensions = extensions;
            this.mimeType = mimeType;
            this.readerNames = readerNames;
        }

        static ImageFormat fromFilename(String filename) {
            String lower = filename.toLowerCase(Locale.ROOT);
            return Stream.of(values())
                    .filter(format -> format.extensions.stream().anyMatch(lower::endsWith))
                    .findFirst()
                    .orElseThrow(() -> ImageUploadException.invalid("JPEG, PNG, WebP 파일만 업로드할 수 있습니다."));
        }

        static ImageFormat fromMimeType(String mimeType) {
            if (!StringUtils.hasText(mimeType)) {
                throw ImageUploadException.invalid("이미지 MIME type이 없습니다.");
            }
            String normalized = mimeType.toLowerCase(Locale.ROOT);
            return Stream.of(values())
                    .filter(format -> format.mimeType.equals(normalized))
                    .findFirst()
                    .orElseThrow(() -> ImageUploadException.invalid("JPEG, PNG, WebP MIME type만 허용됩니다."));
        }

        static ImageFormat fromBytes(byte[] bytes) {
            if (isJpeg(bytes)) {
                return JPEG;
            }
            if (isPng(bytes)) {
                return PNG;
            }
            if (isWebp(bytes)) {
                return WEBP;
            }
            throw ImageUploadException.invalid("실제 파일 형식이 JPEG, PNG, WebP가 아닙니다.");
        }

        boolean matchesReader(String readerName) {
            return readerName != null && readerNames.contains(readerName.toLowerCase(Locale.ROOT));
        }

        private static boolean isJpeg(byte[] bytes) {
            return bytes.length >= 3
                    && (bytes[0] & 0xff) == 0xff
                    && (bytes[1] & 0xff) == 0xd8
                    && (bytes[2] & 0xff) == 0xff;
        }

        private static boolean isPng(byte[] bytes) {
            byte[] signature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
            if (bytes.length < signature.length) {
                return false;
            }
            for (int index = 0; index < signature.length; index++) {
                if (bytes[index] != signature[index]) {
                    return false;
                }
            }
            return true;
        }

        private static boolean isWebp(byte[] bytes) {
            return bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
        }
    }
}
