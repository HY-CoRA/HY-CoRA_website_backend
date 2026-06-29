package com.hycora.backend.global.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebMvcConfigTest {

    @TempDir
    Path uploadRoot;

    @Test
    void createsAllUploadSubdirectories() {
        WebMvcConfig config = new WebMvcConfig(uploadRoot.toString());

        config.init();

        assertThat(uploadRoot.resolve("banners")).isDirectory();
        assertThat(uploadRoot.resolve("leaders")).isDirectory();
        assertThat(uploadRoot.resolve("activities")).isDirectory();
        assertThat(uploadRoot.resolve("events")).isDirectory();
    }

    @Test
    void failsFastWhenUploadSubdirectoryCannotBeCreated() throws Exception {
        Files.writeString(uploadRoot.resolve("leaders"), "not a directory");
        WebMvcConfig config = new WebMvcConfig(uploadRoot.toString());

        assertThatThrownBy(config::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("업로드 디렉터리를 생성할 수 없습니다")
                .hasMessageContaining("leaders");
    }
}
