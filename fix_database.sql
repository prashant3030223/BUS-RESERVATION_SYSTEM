-- Fix orphaned notifications in the database
-- This script will clean up notifications that reference non-existent users

USE bus_db;

-- First, let's see how many orphaned notifications we have
SELECT COUNT(*) as orphaned_notifications
FROM notifications n
LEFT JOIN users u ON n.user_id = u.id
WHERE u.id IS NULL;

-- Delete orphaned notifications (notifications with user_id that doesn't exist in users table)
DELETE FROM notifications
WHERE user_id NOT IN (SELECT id FROM users);

-- Verify the cleanup
SELECT COUNT(*) as remaining_notifications FROM notifications;

-- Now try to add the foreign key constraint manually (optional, Hibernate will do this)
-- ALTER TABLE notifications 
-- ADD CONSTRAINT FK9y21adhxn0ayjhfocscqox7bh 
-- FOREIGN KEY (user_id) REFERENCES users (id);
