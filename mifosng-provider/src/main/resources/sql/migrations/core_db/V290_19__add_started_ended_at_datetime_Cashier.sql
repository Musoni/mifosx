ALTER TABLE m_cashiers 
ADD COLUMN started_at DATETIME NOT NULL after full_day,
ADD COLUMN ended_at DATETIME NULL after started_at;