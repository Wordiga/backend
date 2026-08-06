package com.wordiga.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhotoGalleryItem {
    private String galTitle;
    private String galWebImageUrl;
    private String galPhotographyMonth;
}
