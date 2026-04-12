package com.ocr.common.context;

import lombok.Data;

@Data
public class OcrUserContext {

    private static final ThreadLocal<UserInfo> CONTEXT = new ThreadLocal<>();

    @Data
    public static class UserInfo {
        private String tenantId;
        private String userId;
        private String userName;
    }

    public static void set(String tenantId, String userId, String userName) {
        UserInfo info = new UserInfo();
        info.setTenantId(tenantId);
        info.setUserId(userId);
        info.setUserName(userName);
        CONTEXT.set(info);
    }

    public static UserInfo get() {
        return CONTEXT.get();
    }

    public static String getTenantId() {
        UserInfo info = CONTEXT.get();
        return info != null ? info.getTenantId() : null;
    }

    public static String getUserId() {
        UserInfo info = CONTEXT.get();
        return info != null ? info.getUserId() : null;
    }

    public static String getUserName() {
        UserInfo info = CONTEXT.get();
        return info != null ? info.getUserName() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
