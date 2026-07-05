package com.ellh.ai.gateway;

import com.ellh.ai.dto.PronunciationResponse;
import com.ellh.ai.dto.TranslationRequestDto;
import com.ellh.ai.dto.TranslationResponse;
import com.ellh.user.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface AIServiceGateway {

    PronunciationResponse analyzePronunciation(MultipartFile audio, User user, String targetWord, String audioHash) throws Exception;

    TranslationResponse translateText(TranslationRequestDto request) throws Exception;
}
