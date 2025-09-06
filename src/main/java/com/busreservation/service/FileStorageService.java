package com.busreservation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}") // application.properties se path lega
    private String uploadDir;

    public String storeFile(MultipartFile file) {
        // File ka naam saaf karna
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.contains("..")) {
            throw new RuntimeException("Invalid file name: " + originalFileName);
        }
        
        // Ek unique file name generate karna
        String fileName = UUID.randomUUID().toString() + "_" + originalFileName;
        
        try {
            // Upload directory banana agar nahi hai
            Path copyLocation = Paths.get(uploadDir + "/" + fileName);
            Files.createDirectories(copyLocation.getParent()); // Ensure directory exists
            Files.copy(file.getInputStream(), copyLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // File ka URL return karna (static folder mein store karne par)
            return "/uploads/" + fileName; // Ya jo bhi public URL path hai
            
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!");
        }
    }
}