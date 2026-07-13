-- 打刻メモ（備考）: 出勤・退勤それぞれに任意の備考を保存する（最大200文字）
ALTER TABLE attendance_records ADD COLUMN clock_in_memo  VARCHAR(200);
ALTER TABLE attendance_records ADD COLUMN clock_out_memo VARCHAR(200);
