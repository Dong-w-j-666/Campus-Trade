package com.campustrade.media;

import com.campustrade.auth.LoginRequired;
import com.campustrade.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
@LoginRequired
public class UploadController {

    private final Path imageDirectory;

    public UploadController(@Value("${app.upload-dir:./uploads}") String uploadDirectory) {
        this.imageDirectory = Path.of(uploadDirectory)
                .toAbsolutePath()
                .normalize()
                .resolve("images");
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("请选择要上传的图片");
        }

        String extension = switch (file.getContentType() == null ? "" : file.getContentType()) {
            case MediaType.IMAGE_JPEG_VALUE -> "jpg";
            case MediaType.IMAGE_PNG_VALUE -> "png";
            case "image/webp" -> "webp";
            default -> throw ApiException.badRequest("仅支持 JPG、PNG 或 WebP 图片");
        };

        String fileName = UUID.randomUUID() + "." + extension;
        Path destination = imageDirectory.resolve(fileName).normalize();
        if (!destination.startsWith(imageDirectory)) {
            throw ApiException.badRequest("图片文件名不合法");
        }

        try {
            Files.createDirectories(imageDirectory);
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save uploaded image", exception);
        }
        return Map.of("url", "/uploads/images/" + fileName);
    }
}
