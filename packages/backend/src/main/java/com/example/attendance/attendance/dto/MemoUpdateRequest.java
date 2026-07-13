package com.example.attendance.attendance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 勤怠履歴からの打刻メモ編集・削除リクエスト。
 * memo は null / 空文字（削除）を許容し、version で楽観ロックする。
 */
public record MemoUpdateRequest(
    @Size(max = 200) String clockInMemo,
    @Size(max = 200) String clockOutMemo,
    @NotNull Long version
) {}
