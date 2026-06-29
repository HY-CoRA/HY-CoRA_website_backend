package com.hycora.backend.global.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageStorageServiceTest {

    private static final String WEBP_2X2_RED =
            "UklGRjwAAABXRUJQVlA4IDAAAADQAQCdASoCAAIALmk0mk0iIiIiIgBoSygABc6zbAAA/v56QAAAAA==";

    @TempDir
    Path uploadRoot;

    private ImageStorageService imageStorageService;

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(uploadRoot.resolve("leaders"));
        Files.createDirectories(uploadRoot.resolve("activities"));
        imageStorageService = new ImageStorageService(uploadRoot.toString());
    }

    @Test
    void storesJpegLeaderPhotoAs500SquareJpeg() throws Exception {
        MockMultipartFile image = imageFile("leader.jpg", "image/jpeg", "jpg", 800, 400, Color.RED);

        ImageStorageService.StoredImage stored = imageStorageService.storeLeaderPhoto("최관우", image);

        Path result = uploadRoot.resolve("leaders").resolve("최관우.jpg");
        BufferedImage decoded = ImageIO.read(result.toFile());
        assertThat(stored.imageUrl()).isEqualTo("/uploads/leaders/%EC%B5%9C%EA%B4%80%EC%9A%B0.jpg");
        assertThat(result).isRegularFile();
        assertThat(decoded.getWidth()).isEqualTo(500);
        assertThat(decoded.getHeight()).isEqualTo(500);
        assertThat(Files.readAllBytes(result)).startsWith((byte) 0xff, (byte) 0xd8, (byte) 0xff);
    }

    @Test
    void acceptsPngAndConvertsItToJpeg() throws Exception {
        MockMultipartFile image = imageFile("leader.png", "image/png", "png", 300, 600, Color.BLUE);

        imageStorageService.storeLeaderPhoto("홍길동", image);

        Path result = uploadRoot.resolve("leaders").resolve("홍길동.jpg");
        assertThat(ImageIO.read(result.toFile()).getWidth()).isEqualTo(500);
        assertThat(Files.readAllBytes(result)).startsWith((byte) 0xff, (byte) 0xd8, (byte) 0xff);
    }

    @Test
    void acceptsWebpAndConvertsItToJpeg() throws Exception {
        byte[] webp = Base64.getDecoder().decode(WEBP_2X2_RED);
        MockMultipartFile image = new MockMultipartFile("image", "leader.webp", "image/webp", webp);

        imageStorageService.storeLeaderPhoto("김코라", image);

        BufferedImage result = ImageIO.read(uploadRoot.resolve("leaders").resolve("김코라.jpg").toFile());
        assertThat(result.getWidth()).isEqualTo(500);
        assertThat(result.getHeight()).isEqualTo(500);
    }

    @Test
    void overwritesExistingLeaderPhotoOnlyAfterNewImageIsReady() throws Exception {
        imageStorageService.storeLeaderPhoto(
                "최관우",
                imageFile("first.jpg", "image/jpeg", "jpg", 500, 500, Color.RED)
        );
        imageStorageService.storeLeaderPhoto(
                "최관우",
                imageFile("second.png", "image/png", "png", 500, 500, Color.BLUE)
        );

        BufferedImage result = ImageIO.read(uploadRoot.resolve("leaders").resolve("최관우.jpg").toFile());
        Color center = new Color(result.getRGB(250, 250));
        assertThat(center.getBlue()).isGreaterThan(center.getRed());
    }

    @Test
    void storesActivityImagesWithUuidNamesAnd1200By900Size() throws Exception {
        List<ImageStorageService.StoredImage> stored = imageStorageService.storeActivityImages(
                3L,
                List.of(
                        imageFile("one.jpg", "image/jpeg", "jpg", 400, 800, Color.RED),
                        imageFile("two.png", "image/png", "png", 1000, 500, Color.BLUE)
                )
        );

        assertThat(stored).hasSize(2);
        for (ImageStorageService.StoredImage image : stored) {
            UUID.fromString(image.id());
            Path result = uploadRoot.resolve("activities").resolve("3").resolve(image.id() + ".jpg");
            BufferedImage decoded = ImageIO.read(result.toFile());
            assertThat(decoded.getWidth()).isEqualTo(1200);
            assertThat(decoded.getHeight()).isEqualTo(900);
            assertThat(image.imageUrl()).isEqualTo("/uploads/activities/3/" + image.id() + ".jpg");
        }
    }

    @Test
    void acceptsExactlyTenActivityImages() throws Exception {
        MockMultipartFile valid = imageFile("activity.jpg", "image/jpeg", "jpg", 20, 20, Color.RED);

        List<ImageStorageService.StoredImage> stored =
                imageStorageService.storeActivityImages(10L, java.util.Collections.nCopies(10, valid));

        assertThat(stored).hasSize(10);
        assertThat(uploadRoot.resolve("activities").resolve("10").toFile().listFiles()).hasSize(10);
    }

    @Test
    void cleansCreatedActivityFilesWhenLaterFileFails() throws Exception {
        MockMultipartFile invalid = new MockMultipartFile(
                "images", "fake.jpg", "image/jpeg", "not an image".getBytes()
        );

        assertThatThrownBy(() -> imageStorageService.storeActivityImages(
                3L,
                List.of(imageFile("valid.jpg", "image/jpeg", "jpg", 100, 100, Color.RED), invalid)
        )).isInstanceOf(ImageUploadException.class);

        Path activityDirectory = uploadRoot.resolve("activities").resolve("3");
        assertThat(activityDirectory).doesNotExist();
    }

    @Test
    void rejectsInvalidExtensionMimeSpoofCorruptionAndOversize() throws Exception {
        MockMultipartFile jpegWithBadExtension =
                imageFile("leader.gif", "image/jpeg", "jpg", 100, 100, Color.RED);
        MockMultipartFile jpegWithBadMime =
                imageFile("leader.jpg", "image/png", "jpg", 100, 100, Color.RED);
        MockMultipartFile spoofed =
                new MockMultipartFile("image", "leader.jpg", "image/jpeg", "%PDF-1.7".getBytes());
        byte[] brokenJpeg = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00, 0x01};
        MockMultipartFile corrupted =
                new MockMultipartFile("image", "leader.jpg", "image/jpeg", brokenJpeg);
        MockMultipartFile oversized = new MockMultipartFile(
                "image", "leader.jpg", "image/jpeg", new byte[(int) ImageStorageService.MAX_FILE_SIZE + 1]
        );

        assertThatThrownBy(() -> imageStorageService.storeLeaderPhoto("홍길동", jpegWithBadExtension))
                .isInstanceOf(ImageUploadException.class)
                .extracting(exception -> ((ImageUploadException) exception).getStatus().value())
                .isEqualTo(400);
        assertThatThrownBy(() -> imageStorageService.storeLeaderPhoto("홍길동", jpegWithBadMime))
                .isInstanceOf(ImageUploadException.class);
        assertThatThrownBy(() -> imageStorageService.storeLeaderPhoto("홍길동", spoofed))
                .isInstanceOf(ImageUploadException.class);
        assertThatThrownBy(() -> imageStorageService.storeLeaderPhoto("홍길동", corrupted))
                .isInstanceOf(ImageUploadException.class);
        assertThatThrownBy(() -> imageStorageService.storeLeaderPhoto("홍길동", oversized))
                .isInstanceOf(ImageUploadException.class)
                .extracting(exception -> ((ImageUploadException) exception).getStatus().value())
                .isEqualTo(413);
    }

    @Test
    void rejectsPathTraversalAndMoreThanTenActivityImages() throws Exception {
        MockMultipartFile valid = imageFile("valid.jpg", "image/jpeg", "jpg", 10, 10, Color.RED);

        assertThatThrownBy(() -> imageStorageService.storeLeaderPhoto("../admin", valid))
                .isInstanceOf(ImageUploadException.class);
        assertThatThrownBy(() -> imageStorageService.storeLeaderPhoto("admin/other", valid))
                .isInstanceOf(ImageUploadException.class);
        assertThatThrownBy(() -> imageStorageService.storeActivityImages(
                1L,
                java.util.Collections.nCopies(11, valid)
        )).isInstanceOf(ImageUploadException.class);
    }

    @Test
    void deletesLeaderAndActivityImages() throws Exception {
        imageStorageService.storeLeaderPhoto(
                "최관우",
                imageFile("leader.jpg", "image/jpeg", "jpg", 100, 100, Color.RED)
        );
        ImageStorageService.StoredImage activityImage = imageStorageService.storeActivityImages(
                4L,
                List.of(imageFile("activity.jpg", "image/jpeg", "jpg", 100, 100, Color.RED))
        ).getFirst();

        imageStorageService.deleteLeaderPhoto("최관우");
        imageStorageService.deleteActivityImage(4L, UUID.fromString(activityImage.id()));

        assertThat(uploadRoot.resolve("leaders").resolve("최관우.jpg")).doesNotExist();
        assertThat(uploadRoot.resolve("activities").resolve("4")).doesNotExist();
    }

    private MockMultipartFile imageFile(
            String filename,
            String contentType,
            String format,
            int width,
            int height,
            Color color
    ) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, color.getRGB());
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return new MockMultipartFile("image", filename, contentType, new ByteArrayInputStream(output.toByteArray()));
    }
}
