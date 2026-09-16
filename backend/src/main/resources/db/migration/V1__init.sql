-- V1__init.sql
-- Initial database setup for DocuSphere

-- Enable UUID generation function (gen_random_uuid)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Tables will be created in subsequent migrations as entities are defined.