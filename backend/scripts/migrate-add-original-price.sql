-- Migration: Add original_price column to tickets table
-- Run this if your database was created before the original_price field was added

ALTER TABLE tickets
ADD COLUMN IF NOT EXISTS original_price INT NOT NULL DEFAULT price;

-- Update existing tickets to have original_price = price if not set
UPDATE tickets
SET original_price = price
WHERE original_price IS NULL OR original_price = 0;
