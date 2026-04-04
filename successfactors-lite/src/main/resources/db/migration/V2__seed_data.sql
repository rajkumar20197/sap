-- Flyway Migration: V2__seed_data.sql
-- Sample data for multiple tenants — demonstrates multi-tenancy isolation

-- =============================================
-- TENANT: acme-corp
-- =============================================
INSERT INTO employees (tenant_id, name, department, email, title, performance_score)
VALUES
    ('acme-corp', 'Alice Johnson', 'Engineering', 'alice@acme.com', 'Senior Java Developer', 4.5),
    ('acme-corp', 'Bob Smith', 'Engineering', 'bob@acme.com', 'Backend Engineer', 3.8),
    ('acme-corp', 'Carol White', 'HR', 'carol@acme.com', 'HR Business Partner', 4.2),
    ('acme-corp', 'David Brown', 'Finance', 'david@acme.com', 'Financial Analyst', 3.9),
    ('acme-corp', 'Eve Davis', 'Engineering', 'eve@acme.com', 'DevOps Engineer', 4.7)
ON CONFLICT DO NOTHING;

INSERT INTO employee_skills (employee_id, skill)
SELECT e.id, s.skill FROM employees e
CROSS JOIN (VALUES
    ('Java'), ('Spring Boot'), ('PostgreSQL'), ('Docker')
) AS s(skill)
WHERE e.email = 'alice@acme.com'
ON CONFLICT DO NOTHING;

INSERT INTO employee_skills (employee_id, skill)
SELECT e.id, s.skill FROM employees e
CROSS JOIN (VALUES
    ('Java'), ('Kafka'), ('AWS'), ('Kubernetes')
) AS s(skill)
WHERE e.email = 'bob@acme.com'
ON CONFLICT DO NOTHING;

INSERT INTO employee_skills (employee_id, skill)
SELECT e.id, s.skill FROM employees e
CROSS JOIN (VALUES
    ('Terraform'), ('AWS'), ('CI/CD'), ('Docker'), ('Ansible')
) AS s(skill)
WHERE e.email = 'eve@acme.com'
ON CONFLICT DO NOTHING;

-- =============================================
-- TENANT: globex-inc (data-isolated from acme-corp)
-- =============================================
INSERT INTO employees (tenant_id, name, department, email, title, performance_score)
VALUES
    ('globex-inc', 'Frank Miller', 'Engineering', 'frank@globex.com', 'Staff Engineer', 4.9),
    ('globex-inc', 'Grace Lee', 'Product', 'grace@globex.com', 'Product Manager', 4.3),
    ('globex-inc', 'Henry Wilson', 'HR', 'henry@globex.com', 'Recruiter', 3.7)
ON CONFLICT DO NOTHING;

INSERT INTO employee_skills (employee_id, skill)
SELECT e.id, s.skill FROM employees e
CROSS JOIN (VALUES
    ('Java'), ('MCP'), ('Spring Boot'), ('SAP HANA'), ('OData')
) AS s(skill)
WHERE e.email = 'frank@globex.com'
ON CONFLICT DO NOTHING;
