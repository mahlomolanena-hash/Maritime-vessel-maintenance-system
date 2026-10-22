package com.example.maintenance.security;

/** Reusable @PreAuthorize expressions. */
public final class Access {

    /** Fleet, schedule and spare part management; work order assignment. */
    public static final String MANAGER = "hasAnyRole('ADMIN','SUPERINTENDENT')";

    /** User administration. */
    public static final String ADMIN = "hasRole('ADMIN')";

    private Access() {
    }
}
