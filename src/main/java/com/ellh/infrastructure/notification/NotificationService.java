package com.ellh.infrastructure.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Outbound Notification Service (ND-09).
 * Dispatches Push Notifications to Android devices via Firebase Cloud Messaging.
 */
@Slf4j
@Service
public class NotificationService {

    /**
     * Dispatches an Achievement Unlock notification to a specific device.
     * Matches the Android EllhFirebaseMessagingService.handleAchievementUnlock payload.
     *
     * @param fcmToken        the target device's registered FCM token
     * @param achievementName name of the unlocked badge (e.g. "Century Club")
     * @param xpReward        bonus XP awarded (e.g. 50)
     */
    public void sendAchievementUnlock(String fcmToken, String achievementName, int xpReward) {
        if (fcmToken == null || fcmToken.isBlank()) return;

        Map<String, String> data = new HashMap<>();
        data.put("type", "ACHIEVEMENT_UNLOCK");
        data.put("achievementName", achievementName);
        data.put("xpReward", String.valueOf(xpReward));

        Message message = Message.builder()
                .setToken(fcmToken)
                .putAllData(data)
                .setNotification(Notification.builder()
                        .setTitle("Achievement Unlocked! 🏆")
                        .setBody(achievementName + " — +" + xpReward + " XP")
                        .build())
                .build();

        sendAsync(message, "ACHIEVEMENT_UNLOCK");
    }

    /**
     * Dispatches a Daily Streak Reminder notification.
     * Matches the Android EllhFirebaseMessagingService.handleStreakReminder payload.
     */
    public void sendStreakReminder(String fcmToken, int currentStreak) {
        if (fcmToken == null || fcmToken.isBlank()) return;

        Map<String, String> data = new HashMap<>();
        data.put("type", "STREAK_REMINDER");
        data.put("currentStreak", String.valueOf(currentStreak));

        Message message = Message.builder()
                .setToken(fcmToken)
                .putAllData(data)
                .setNotification(Notification.builder()
                        .setTitle("Keep the Flame Burning! 🔥")
                        .setBody("Don't lose your " + currentStreak + " day streak. Practice for 5 minutes today!")
                        .build())
                .build();

        sendAsync(message, "STREAK_REMINDER");
    }

    private void sendAsync(Message message, String type) {
        try {
            // FirebaseMessaging is initialized by FirebaseApp on startup (FirebaseConfig)
            FirebaseMessaging.getInstance().sendAsync(message);
            log.info("FCM push successfully queued for transmission: type={}", type);
        } catch (IllegalStateException e) {
            // Safe fallback if FirebaseAdmin SDK was not initialized due to missing credentials
            log.warn("FCM push omitted: FirebaseApp is not initialized (expected in test profiles)");
        } catch (Exception e) {
            log.error("Failed to transmit FCM push: {}", e.getMessage(), e);
        }
    }
}
