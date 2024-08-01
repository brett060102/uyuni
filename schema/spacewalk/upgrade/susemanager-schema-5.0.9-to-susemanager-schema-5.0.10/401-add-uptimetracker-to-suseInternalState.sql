--
-- add uptime_data to suseInternalState
--

INSERT INTO suseInternalState (id, name, label)
  SELECT 13, 'uptimetracker.requestdata', 'Uptime Tracking Data'
   WHERE NOT EXISTS (
	SELECT 1 FROM suseInternalState
         WHERE id = 15
   );
