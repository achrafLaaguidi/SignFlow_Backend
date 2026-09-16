package com.laaguidi.SignFlow.model.enums;

import lombok.Getter;

@Getter
public enum PermissionEnum {
    // ==================== User Management ====================
    USER_CREATE("Create new tenant users"),
    USER_READ("View tenant users and profiles"),
    USER_UPDATE("Update tenant user details"),
    USER_DELETE("Delete or deactivate tenant users"),
    USER_MANAGE_ROLES("Assign or modify user roles and permissions"),

    // ==================== Document Management ====================
    DOCUMENT_CREATE("Create or upload new documents"),
    DOCUMENT_READ("View and download documents"),
    DOCUMENT_UPDATE("Edit document metadata or structure"),
    DOCUMENT_DELETE("Delete or archive documents"),
    DOCUMENT_SHARE("Share documents with internal or external signers"),
    DOCUMENT_SIGN("Sign documents electronically"),
    DOCUMENT_REJECT("Decline or reject signing a document"),
    DOCUMENT_VOID("Cancel/Void an active document workflow"),

    // ==================== Template Management ====================
    TEMPLATE_CREATE("Create document reusable templates"),
    TEMPLATE_READ("View available document templates"),
    TEMPLATE_UPDATE("Edit existing document templates"),
    TEMPLATE_DELETE("Delete document templates"),

    // ==================== Signature Fields & Preparation ====================
    FIELD_CREATE("Add signature, text, or date fields to documents"),
    FIELD_UPDATE("Modify existing document fields"),
    FIELD_DELETE("Remove fields from documents"),

    // ==================== Audit & Compliance ====================
    AUDIT_READ("View document execution audit trails and history"),
    AUDIT_EXPORT("Export verification certificates and audit logs"),

    // ==================== Tenant & Organization Settings ====================
    TENANT_SETTINGS_READ("View tenant configuration and company profile"),
    TENANT_SETTINGS_UPDATE("Update company branding, custom domains, and settings"),

    // ==================== API Keys & Integrations ====================
    INTEGRATION_MANAGE("Manage API keys, webhooks, and third-party integrations");

    private final String description;

    PermissionEnum(String description) {
        this.description = description;
    }
}