package ru.yandex.practicum.mymarket.service;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ImageService {

    @Value("${app.image.dir:src/main/resources/static}")
    private String dirProperty;

    private Path imageDirectory;

    @PostConstruct
    public void init() {
        this.imageDirectory = Paths.get(dirProperty);
    }

    public Resource getImageAsResource(String filename) {
        try {

            Path filePath = imageDirectory.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}