package com.ellh.ai.gateway;

import com.ellh.ai.dto.PronunciationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ellh.ai.dto.TranslationRequestDto;
import com.ellh.ai.dto.TranslationResponse;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleColabAIService {

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.gateway.colab-primary-url}")
    private String primaryUrl;

    public PronunciationResponse analyze(MultipartFile audio, String targetWord) throws IOException {
        RequestBody fileBody = RequestBody.create(audio.getBytes(), MediaType.parse(audio.getContentType() != null ? audio.getContentType() : "audio/aac"));
        
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("audio", audio.getOriginalFilename(), fileBody)
                .addFormDataPart("targetWord", targetWord)
                .build();

        Request request = new Request.Builder()
                .url(primaryUrl + "/api/v1/ai/score")
                .post(requestBody)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Colab primary failed with code: " + response.code());
            }
            return objectMapper.readValue(response.body().string(), PronunciationResponse.class);
        }
    }
    
    public TranslationResponse translate(TranslationRequestDto requestDto) throws IOException {
        RequestBody body = RequestBody.create(
                objectMapper.writeValueAsString(requestDto),
                MediaType.parse("application/json; charset=utf-8")
        );

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(primaryUrl + "/api/v1/ai/translate")
                .post(body)
                .build();

        try (okhttp3.Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Colab translation failed with code: " + response.code());
            }
            return objectMapper.readValue(response.body().string(), TranslationResponse.class);
        }
    }
}
