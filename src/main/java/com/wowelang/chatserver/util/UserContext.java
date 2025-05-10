package com.wowelang.chatserver.util;

public class UserContext {
    
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    
    private UserContext() {
        // 인스턴스화 방지
    }
    
    public static void setUserId(String userId) {
        USER_ID.set(userId);
    }
    
    public static String getUserId() {
        return USER_ID.get();
    }
    
    public static void clear() {
        USER_ID.remove();
    }
} 