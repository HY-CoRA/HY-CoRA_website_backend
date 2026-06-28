package com.hycora.backend.global.image;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ImageUploadException extends RuntimeException {

    private final HttpStatus status;

    private ImageUploadException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    private ImageUploadException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public static ImageUploadException invalid(String message) {
        return new ImageUploadException(HttpStatus.BAD_REQUEST, message);
    }

    public static ImageUploadException tooLarge() {
        return new ImageUploadException(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기는 5MB를 초과할 수 없습니다.");
    }

    public static ImageUploadException notFound(String message) {
        return new ImageUploadException(HttpStatus.NOT_FOUND, message);
    }

    public static ImageUploadException storage(String message, Throwable cause) {
        return new ImageUploadException(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
