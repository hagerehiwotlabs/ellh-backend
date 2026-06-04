package com.ellh.learning.dto;

import lombok.Data;

@Data
public class ContrastiveNote {
    private String ruleCategory;
    private String ruleTitle;
    private String contrastDescription;
    private String sourceLanguage;
    private String targetLanguage;
    private String versionStamp;
}
