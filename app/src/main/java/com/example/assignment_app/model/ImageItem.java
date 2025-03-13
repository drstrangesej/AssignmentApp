package com.example.assignment_app.model;

import java.util.List;

public class ImageItem {
    // Fields to store image details
    private String id; // Unique identifier for the image
    private String title; // Title or name of the image
    private Thumbnail thumbnail; // Thumbnail details

    // Constructor to initialize an ImageItem object
    public ImageItem(String id, String title, Thumbnail thumbnail) {
        this.id = id;
        this.title = title;
        this.thumbnail = thumbnail;
    }

    // Getter methods to retrieve values
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Thumbnail getThumbnail() {
        return thumbnail;
    }

    // Nested class to represent thumbnail details
    public static class Thumbnail {
        private String domain; // Domain URL where the image is hosted
        private String basePath; // Base path for the image storage
        private String key; // Unique key identifying the image file
        private List<Integer> qualities; // List of available image quality resolutions

        // Constructor to initialize a Thumbnail object
        public Thumbnail(String domain, String basePath, String key, List<Integer> qualities) {
            this.domain = domain;
            this.basePath = basePath;
            this.key = key;
            this.qualities = qualities;
        }

        // Getter methods to retrieve values
        public String getDomain() {
            return domain;
        }

        public String getBasePath() {
            return basePath;
        }

        public String getKey() {
            return key;
        }

        public List<Integer> getQualities() {
            return qualities;
        }

        // Method to generate the full image URL
        public String getImageUrl() {
            return domain + "/" + basePath + "/" + key;
        }
    }
}
