# 실시간 생성기 실행

생성기 코드는 `src/main/java/springproject/datagenerate`에 모았습니다.

| 파일 | 역할 |
|---|---|
| `Scheduler.java` | 설정된 간격으로 실행하는 스케줄러 |
| `Simulation_Service.java` | LOT·공정 상태, 승인 대기, 실패 재시도 제어 |
| `SimulationApiClient.java` | REST 통신, 저장 여부 조회 및 재전송 처리 |
| `SimulationDataGenerator.java` | 난수 측정값과 요청 데이터 생성 |

Controller는 기존 `controller` 패키지에서 `datagenerate.Simulation_Service`를 호출합니다.

생성기는 Spring 서버 준비 후 기존 REST API에 요청하며 실제 MySQL에 데이터를 저장합니다.
SSE와 이상 판정은 포함하지 않습니다. 초기 SQL 파일에 내용을 덧붙이는 기능도 아닙니다.

`application.properties`에서 필요에 따라 다음 값을 설정합니다.

```properties
simulation.enabled=true
simulation.api-base-url=http://localhost:8080
simulation.user-id=1
simulation.tick-delay-ms=1000
simulation.process-duration-seconds=20
simulation.lot-delay-ms=10000
simulation.sensor-ticks=10
simulation.packaging-units=10
simulation.inspection-failure-rate=0.01
```

`user-id`는 DB에 존재하는 작업자 번호입니다. 서버 포트를 바꾸면 API 주소도 맞추세요.
API 주소를 생략하면 `server.port`를 사용한 localhost 주소가 기본값입니다.
자동 생성을 끄려면 `simulation.enabled=false`를 설정합니다.

모든 공정은 서버가 반환한 실제 start_time을 기준으로 20초 동안 진행합니다.
스케줄러는 기본 1초마다 확인하며, 공정별 목표 건수를 20초 구간에 분산 생성합니다.
예를 들어 포장 목표가 10개면 약 2초마다 한 개, 30개면 한 주기에 여러 개를 저장합니다.
20초 경과 및 데이터 저장 완료 후 종료를 요청합니다. 통신 지연·검사 승인·최종 판정
대기로 실제 종료와 다음 공정 시작은 더 늦어질 수 있습니다.
공정의 실제 소요시간은 기존 process_execution.duration_min에 기록합니다.
이 시간 제어는 생성기가 관리하는 LOT에 적용되며, 화면에서 별도로 시작한 LOT를
자동으로 찾아 생성 대상에 연결하는 기능은 포함하지 않습니다.

## API

- `POST /mask/simulation/start`: 생성 시작 또는 실패한 저장 요청 재시도
- `POST /mask/simulation/stop`: 현재 상태를 유지하고 생성 중지
- `GET /mask/simulation/status`: 실제 실행번호, 진행 상황, 대기 이유, 오류, 미저장 요청
- `GET /mask/simulation/samples`: 최근 저장 성공 기록 최대 200건
- `POST /mask/sensor-telemetries`: 센서 원본 저장
- `POST /mask/bulk-qc`: 벌크 검사 원본 저장

LOT 생성 → 원료 칭량 → 가열/혼합 → 냉각 → 벌크 검사 → 최종 포장 검사 순서입니다.
첫 공정은 기존 POST API로 시작하고, 이후 공정은 기존 finish/resume 응답을 사용합니다.
생성기가 process_execution에 별도의 가상 실행을 INSERT하지 않습니다.
측정 데이터에는 실제 execution_id가 연결됩니다. 원료 칭량은 기존 구조대로 batch_id로 연결됩니다.

온도 상승·하락 과정은 생성하지 않습니다. 가열은 62.0~63.0℃, 냉각은 원료 투입 전
37.5~38.5℃ 범위에서 매번 독립적으로 생성합니다. stage는 사용하지 않으며
실행번호에 연결된 공정명으로 가열/혼합과 냉각을 구분합니다.

## 검사 승인과 공정 대기

최종 판정은 요청에 포함하지 않습니다. 기존 품질관리 화면/API로 판정·승인해야 합니다.

1. 벌크 원본 저장 후 `PATCH /mask/bulk-qc/{qcId}/result`로 종합 결과를 확정합니다.
2. `PATCH /mask/process-executions/{executionId}/qc-approval`에 `qcId`, `userId`를 보내 승인합니다.
3. 합격 승인 및 알람 진행 조건을 충족하면 생성기가 resume API로 포장 단계에 진입합니다.
4. 생성된 각 제품의 `PATCH /mask/filling-packagings/{pouchId}/result`로 최종 판정을 확정합니다.
5. 모든 제품 판정과 알람 처리가 끝나면 기존 finish 서비스가 LOT를 완료합니다.

불합격 벌크는 포장으로 진행하지 않습니다. 미처리 알람도 기존 공정 서비스 정책을 따릅니다.
현재 생성기는 재검사·재작업을 자동으로 만들지 않습니다.

## 스키마와 재시도

센서 Entity에는 `tank_temp_c`, `source_sample_id`를 사용합니다.
이전 버전에서 생성된 DB의 stage 컬럼은 더 이상 읽거나 쓰지 않습니다.
`ddl-auto=update`는 사용하지 않는 컬럼을 자동 삭제하지 않으므로 DB에는 남아 있을 수 있습니다.
현재 프로젝트의 `ddl-auto=update` 설정이면 서버 시작 시 DB 컬럼과 unique 인덱스가 반영됩니다.
스키마 자동 변경을 끈 환경은 운영 방식에 맞는 마이그레이션을 먼저 적용해야 합니다.
기존 data_mask.sql은 다시 실행하지 않습니다.

센서 source_sample_id와 원료·검사·제품의 고유번호로 실패 후 재전송 중복을 방지합니다.
저장 실패 시 자동 중지되며 status의 lastError를 해결한 뒤 start를 호출합니다.
생성기 상태와 미저장 요청은 메모리에 있으므로 서버 재시작 후 복구되지는 않습니다.
재시작하면 이전 진행 LOT는 DB에 남고 새 생성기가 새 LOT를 만듭니다.

## 검증

```powershell
.\gradlew.bat verifySimulationApi --offline
```

로컬 테스트 HTTP 서버를 사용하므로 실제 MySQL 데이터는 변경하지 않습니다.
실제 실행번호 사용, DTO 요청 필드, 모든 단계 저장 요청, 실패 재시도,
검사 승인 대기, LOT 완료와 생성 중지를 검증합니다.
