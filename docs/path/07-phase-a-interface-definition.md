# 07: Phase A インターフェース定義

## プロンプト

> 進んで

（前セッションで Phase A の存在を忘れて逐次実装を提案 → ユーザーに指摘され → 「進んで」で Phase A 開始）

## 選択肢への回答

Phase A の実装計画（29 ファイル）を提示 → ユーザーが承認

## やったこと

### 概要

Phase B（Unit 01〜04 並列実装）の前提として、全 Unit で共有されるインターフェースを一括定義した。

### Flyway マイグレーション V1〜V3

| ファイル | 内容 |
|---------|------|
| `V1__create_departments.sql` | departments テーブル（UUID PK, name UNIQUE, version, 監査カラム） |
| `V2__create_employees.sql` | employees テーブル（FK→departments, role CHECK, is_manager）+ インデックス 2 つ |
| `V3__create_attendance_records.sql` | attendance_records テーブル（FK→employees, work_date, clock_in/out）+ 複合インデックス |

### Enum

| ファイル | 値 |
|---------|-----|
| `employee/entity/Role.java` | `EMPLOYEE`, `ADMIN` |
| `attendance/domain/AttendanceStatus.java` | `NOT_CLOCKED_IN`, `CLOCKED_IN`, `CLOCKED_OUT` |

### Entity クラス

| Entity | テーブル | 特記事項 |
|--------|---------|---------|
| `Department` | departments | シンプルなマスタ |
| `Employee` | employees | `@ManyToOne` で Department 参照、`isManager` に明示的 `@Column(name)` |
| `AttendanceRecord` | attendance_records | `@ManyToOne` で Employee 参照、`corrected` に `@Builder.Default` |

共通パターン:
- `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate` / `@LastModifiedDate`
- `@Version Long version`（楽観ロック）
- Lombok: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

### DTO record（16 ファイル）

**department/dto/**: DepartmentRequest（Bean Validation 付き）, DepartmentResponse（`from()` ファクトリ）

**employee/dto/**: EmployeeCreateRequest, EmployeeUpdateRequest, RetireRequest, ManagerRequest, EmployeeResponse（`from()` ファクトリ）

**auth/dto/**: LoginRequest, AuthUserResponse（`from()` ファクトリ）

**attendance/dto/**: AttendanceRecordResponse（`from()` ファクトリ）, TodayStatusResponse, AttendanceHistoryResponse, DailyAttendanceResponse, MonthlySummaryResponse, TeamMemberSummaryResponse

### Value Object

`WorkDuration` — 勤務時間計算ロジック:
- 合算勤務時間（分）から休憩控除（6h超〜8h以下→45分、8h超→60分）を算出
- `calculate(List<AttendanceRecord>)` 静的ファクトリメソッド

### Service インターフェース（4 ファイル）

| インターフェース | メソッド |
|---------------|---------|
| `DepartmentService` | findAll, create, update |
| `EmployeeService` | findAll(pageable+filters), findById, create, update, retire, setManager |
| `AuthService` | getCurrentUser |
| `AttendanceService` | clockIn, clockOut, getTodayStatus, getHistory, getTeamAttendance, getAllAttendance |

### ArchUnit ルール更新

DTO・Domain レイヤーを `LayerDependencyTest` に追加:
- `DTO` レイヤー: Entity を参照可、Controller/Service から参照可
- `Domain` レイヤー: Entity を参照可、Service/DTO/Entity から参照可

## つまずき

### ArchUnit レイヤー違反（DTO → Entity）

DTO record が `Role` enum（entity パッケージ）を参照しているため、ArchUnit のレイヤー依存ルールに違反した。

**対処**: `DTO` と `Domain` を optionalLayer として追加し、それぞれの依存方向を定義した。

### boolean フィールドの JPA カラムマッピング

`Employee.isManager`（primitive boolean）は、Lombok の getter が `isManager()` になり、JPA のプロパティ名推論で `manager` → カラム `manager` になる可能性がある。

**対処**: `@Column(name = "is_manager")` を明示的に指定。

## 最終構成

```
packages/backend/src/main/
├── java/com/example/attendance/
│   ├── attendance/
│   │   ├── domain/
│   │   │   ├── AttendanceStatus.java      (enum)
│   │   │   └── WorkDuration.java          (Value Object)
│   │   ├── dto/
│   │   │   ├── AttendanceHistoryResponse.java
│   │   │   ├── AttendanceRecordResponse.java
│   │   │   ├── DailyAttendanceResponse.java
│   │   │   ├── MonthlySummaryResponse.java
│   │   │   ├── TeamMemberSummaryResponse.java
│   │   │   └── TodayStatusResponse.java
│   │   ├── entity/
│   │   │   └── AttendanceRecord.java
│   │   └── service/
│   │       └── AttendanceService.java     (interface)
│   ├── auth/
│   │   ├── dto/
│   │   │   ├── AuthUserResponse.java
│   │   │   └── LoginRequest.java
│   │   └── service/
│   │       └── AuthService.java           (interface)
│   ├── department/
│   │   ├── dto/
│   │   │   ├── DepartmentRequest.java
│   │   │   └── DepartmentResponse.java
│   │   ├── entity/
│   │   │   └── Department.java
│   │   └── service/
│   │       └── DepartmentService.java     (interface)
│   └── employee/
│       ├── dto/
│       │   ├── EmployeeCreateRequest.java
│       │   ├── EmployeeResponse.java
│       │   ├── EmployeeUpdateRequest.java
│       │   ├── ManagerRequest.java
│       │   └── RetireRequest.java
│       ├── entity/
│       │   ├── Employee.java
│       │   └── Role.java                  (enum)
│       └── service/
│           └── EmployeeService.java       (interface)
└── resources/db/migration/
    ├── V1__create_departments.sql
    ├── V2__create_employees.sql
    └── V3__create_attendance_records.sql
```

合計: 3 SQL + 26 Java = **29 ファイル**（+ ArchUnit テスト 1 ファイル更新）
