-- Flyway Migration: V1__init.sql
-- SPRINT 2 CONCEPT: Database Schema Migrations
--
-- WHY FLYWAY? (Interview talking point)
-- Schema migrations are version-controlled SQL scripts that run in order.
-- This solves: "How does your DB schema stay in sync across dev/staging/prod?"
-- Flyway tracks which migrations have run in a 'flyway_schema_history' table.
--
-- NAMING CONVENTION: V{version}__{description}.sql
-- V1__init.sql (versioned, always runs once)
-- R__seed.sql  (repeatable, runs when checksum changes)

-- =============================================
-- EMPLOYEES TABLE
-- Multi-tenant: tenant_id on every table
-- =============================================
CREATE TABLE IF NOT EXISTS employees (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        VARCHAR(100)         NOT NULL,
    name             VARCHAR(255)         NOT NULL,
    department       VARCHAR(100)         NOT NULL,
    email            VARCHAR(255)         NOT NULL,
    title            VARCHAR(255),
    performance_score DECIMAL(4, 2),
    created_at       TIMESTAMP            NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP            NOT NULL DEFAULT NOW(),

    -- Enforce unique email PER TENANT (not globally)
    CONSTRAINT uq_employee_email_tenant UNIQUE (email, tenant_id)
);

-- =============================================
-- EMPLOYEE SKILLS TABLE (ElementCollection mapping)
-- =============================================
CREATE TABLE IF NOT EXISTS employee_skills (
    employee_id BIGINT       NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    skill       VARCHAR(100) NOT NULL,
    PRIMARY KEY (employee_id, skill)
);

-- =============================================
-- INDEXES — Sprint 2: Query Optimization
-- RUN: EXPLAIN ANALYZE SELECT * FROM employees WHERE tenant_id = 'x' AND department = 'HR';
-- WITHOUT index: "Seq Scan"  — scans every row (slow at scale)
-- WITH index:    "Index Scan" — uses B-tree index (fast)
-- =============================================

-- Composite index: tenant_id + department (used together in most queries)
CREATE INDEX IF NOT EXISTS idx_employee_tenant_dept
    ON employees(tenant_id, department);

-- Single index: tenant_id alone (for queries without department filter)
CREATE INDEX IF NOT EXISTS idx_employee_tenant
    ON employees(tenant_id);

-- Email index (for duplicate check and lookups)
CREATE INDEX IF NOT EXISTS idx_employee_email
    ON employees(email);
