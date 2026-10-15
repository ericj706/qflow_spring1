# 실시간 생성기 실행

생성기 코드는 `src/main/java/springproject/datagenerate`에 모았습니다.

| 파일 | 역할 |
|---|---|
| `Scheduler.java` | 설정된 간격으로 실행하는 스케줄러 |
| `Simulation_Service.java` | LOT·공정 상태, 승인 대기, 실패 재시도 제어 |
| `SimulationApiClient.java` | REST 통신, 저장 여부 조회 및 재전송 처리 |
| `SimulationDataGenerator.java` | 난수 측정값과 요청 데이터 생성 |

Controller는 기존 `controller` 패키지에서 `datagenerate.Simulation_Service`를 호출합니다.

## 클래스별 역할과 메서드

### 1. Scheduler.java — 생성 주기 실행

[소스 보기](src/main/java/springproject/datagenerate/Scheduler.java)

`@Configuration`, `@EnableScheduling`으로 스케줄 실행을 활성화합니다.
실제 데이터를 만들거나 DB를 저장하는 클래스는 아니며 `Simulation_Service`를 주기적으로 호출합니다.

| 메서드 | 기능 |
|---|---|
| `generateRealtimeData()` | `@Scheduled`에서 `simulationService.tick()`을 호출합니다. 기본 간격은 1초입니다. |

`fixedDelay`는 이전 호출이 끝난 뒤 설정 시간만큼 기다립니다. 따라서 REST 저장에 2초가 걸리면
다음 호출은 기본 대기 1초를 더한 뒤 시작합니다. 타이머 1회가 LOT 하나나 공정 하나를 의미하지는 않습니다.

### 2. Simulation_Service.java — LOT와 공정 흐름 제어

[소스 보기](src/main/java/springproject/datagenerate/Simulation_Service.java)

현재 LOT, 실제 실행번호, 진행 공정, 경과시간, 생성 건수, 미저장 요청을 관리하는 메인 서비스입니다.
`SimulationDataGenerator`에서 값을 받고 `SimulationApiClient`에 저장을 요청합니다.
다음 공정으로 진행할 수 있는지는 기존 공정 Service의 응답을 따릅니다.

| 메서드 | 기능 |
|---|---|
| 생성자 | REST 클라이언트와 난수 생성기를 주입받습니다. |
| `ready()` | `ApplicationReadyEvent` 이후 주기 실행이 가능하도록 표시합니다. |
| `tick()` | 자동 실행 진입점입니다. 비활성·서버 준비 전이면 반환하고, 오류가 나면 생성기를 정지하고 `lastError`를 기록합니다. |
| `generateTick()` | 미저장 요청을 우선 재시도하고, LOT 등록·시작 대기·측정 생성·종료 요청 중 현재 할 일을 선택합니다. |
| `createLot()` | 작업자 존재를 확인하고 대기 LOT 하나를 생성·저장합니다. 첫 공정은 시작하지 않습니다. |
| `connectStartedProcess()` | 시작 Controller에서 전달한 실행이 현재 대기 LOT의 진행 중 공정일 때 생성기에 연결합니다. |
| `useExecution()` | 실행번호·공정명·시작시간을 반영하고 현재 공정의 생성 건수를 0으로 초기화합니다. |
| `elapsedMillis()` | 실제 공정 시작시간에서 현재까지의 경과시간을 계산합니다. |
| `stageLimit()` | 원료 3건, 가열·냉각 각각 기본 10건, 벌크 1건, 포장 기본 100건으로 공정별 목표 건수를 결정합니다. |
| `generateSample()` | 공정에 맞는 생성 메서드를 선택하고 저장을 요청합니다. 원료 공정에서는 실제 칭량량을 누적합니다. |
| `emit()` | 생성값에 연결 정보를 붙여 `pending`에 보관하고 저장을 시도합니다. |
| `savePending()` | 보관 중인 동일 요청을 저장하고 성공하면 최근 기록에 추가한 뒤 `pending`을 비웁니다. |
| `finishProcess()` | 원료 공정이면 실측량 합계를 LOT에 반영한 뒤 종료·재개 API를 호출합니다. 409이면 대기 사유를 기록합니다. |
| `connectCompletedProcess()` | 화면에서 종료·재개한 결과를 연결합니다. 이미 다른 실행으로 전환된 경우는 무시합니다. |
| `applyCompletion()` | 응답에 다음 공정이 있으면 연결하고, LOT 완료이면 다음 대기 LOT 생성시간을 예약합니다. 그 외에는 승인·알람 대기 사유를 유지합니다. |
| `remember()` | 최근 성공 기록을 최대 200건 보관합니다. |
| `start()` | 생성기를 활성화합니다. 보관된 미저장 요청이 있다면 다음 주기에 다시 시도합니다. 공정 시작 버튼과는 다릅니다. |
| `stop()` | DB를 삭제하지 않고 이후 생성 주기를 중지합니다. |
| `status()` | 생성 여부·LOT·실행번호·대기 사유·오류·진행 건수·생성 확률을 반환합니다. |
| `samples()` | 최근 기록의 복사본을 오래된 순서로 반환합니다. |

주요 상태값은 다음과 같습니다.

| 상태값 | 의미 |
|---|---|
| `batchId`, `executionId` | 생성기가 현재 다루는 LOT와 실제 DB 공정 실행번호 |
| `processIndex` | 원료→가열→냉각→벌크→포장 순서의 인덱스(0~4) |
| `stageTick` | 현재 공정에서 생성한 샘플 수. DB의 내부 단계 컬럼이 아닙니다. |
| `pending` | 아직 저장 성공을 확인하지 못한 요청. 재시도 시 새 난수를 만들지 않습니다. |
| `waitingReason`, `lastError` | 업무 조건에 따른 대기 사유와 통신·생성 실패 사유 |
| `lotPrepared`, `nextLotAt` | 대기 LOT 중복 등록 방지와 다음 LOT 생성 예정 시각 |
| `history`, `sequence` | 최근 샘플 기록과 메모리상의 기록 순번. 전체 DB 건수와는 다릅니다. |

이 상태들은 서버 메모리에 있습니다. 브라우저 새로고침으로 사라지지 않지만 서버 재시작 시 복원되지는 않습니다.
`SimulationSample`은 순번·테이블명·LOT·실행번호·공정명·생성시각·요청 데이터를 묶는 내부 record입니다.
이 객체가 최근 샘플 조회와 미저장 요청 보관에 사용됩니다.

### 3. SimulationApiClient.java — 기존 REST API 호출

[소스 보기](src/main/java/springproject/datagenerate/SimulationApiClient.java)

`RestClient`를 사용하여 기존 Controller로 데이터를 전달합니다. Repository에 직접 INSERT하지 않습니다.
연결 제한시간은 5초, 응답 읽기 제한시간은 10초로 설정합니다.

| 메서드 | 기능 |
|---|---|
| 생성자 | `RestClient.Builder`와 API 주소로 HTTP 클라이언트를 구성합니다. |
| `validateUser()` | 사용자 목록 API에서 설정된 작업자 번호가 존재하는지 확인합니다. 권한 승인 검증을 대신하는 메서드는 아닙니다. |
| `updateActualBulk()` | LOT를 조회하고 `actualBulkKg`를 실측량 합계로 바꿔 기존 LOT 수정 API에 전달합니다. |
| `finishOrResume()` | 공정 상태가 완료이면 `resume`, 그 외에는 `finish`를 호출합니다. HTTP 409는 진행 대기 결과로 반환하고 다른 통신 오류는 호출자에게 전달합니다. |
| `save()` | 테이블명에 맞는 저장 API를 선택합니다. 고유번호 조회로 기존 행이 있으면 POST를 생략합니다. 센서는 `source_sample_id`로 저장 Service가 중복을 검증합니다. |
| `describeError()` | HTTP 상태·응답 본문 또는 예외 메시지를 `lastError`에 표시할 문자열로 변환합니다. |

`Completion`은 종료·재개 응답 또는 대기 사유를 담는 내부 결과 타입입니다.
종료·재개 호출의 `X-Simulation-Request: true` 헤더는 생성기 자체 요청임을 표시합니다.
Controller는 이 요청의 생성기 콜백을 생략하고 생성기가 HTTP 응답을 받은 뒤 반영하므로,
동일 결과의 중복 반영과 생성기 잠금 대기를 피합니다.

### 4. SimulationDataGenerator.java — 난수와 데이터 관계 생성

[소스 보기](src/main/java/springproject/datagenerate/SimulationDataGenerator.java)

공정별 데이터를 `Map<String,Object>`로 만듭니다. DB 저장·최종 합격 승인·공정 전환은 하지 않습니다.
정상·이상 시나리오는 값의 생성 방식을 정하는 것이며, 최종 판정은 DB의 활성 규칙과 기존 Service가 수행합니다.

| 메서드 | 기능 |
|---|---|
| `nextBatchId()` | UUID 기반의 시뮬레이션 LOT 번호를 만듭니다. |
| `generateLot()` | 제품·탱크·목표 벌크량·목표 생산수량·대기 상태를 만듭니다. 기본 포장 목표는 100개입니다. |
| `generateMaterial()` | 큰 원료·일반 원료·미량 원료 3건의 목표량과 실제량을 만듭니다. |
| `generateSensor()` | 공정에 따라 가열 또는 냉각 유지 온도와 교반속도 등 센서값을 만듭니다. 승온·냉각 곡선은 만들지 않습니다. |
| `generateBulkQc()` | 벌크 검사 한 건을 만듭니다. 기본 정상 98%·주의 1%·이상 1%를 먼저 선택하고, 비정상 시 항목 하나만 변경합니다. |
| `generatePackaging()` | 제품 한 개를 만듭니다. 기본 정상 99%·이상 1%이며 이상 시 원인 하나를 선택합니다. 중량 합계와 중량검사 상태도 그 원인에 맞춥니다. |
| `random()` | 지정 구간의 균등 난수를 만든 뒤 지정 소수 자릿수로 반올림합니다. |
| `validateRate()` | 확률이 유한한 0~1 값인지 검증합니다. 벌크 생성 시 주의·이상 확률 합계도 검사합니다. |
| `preparePayload()` | 사용자·LOT·실행번호·출처를 DTO의 필드명에 맞게 붙입니다. 센서에는 중복 방지용 `source_sample_id`를 추가합니다. |
| `inspectionFailureRate()` | 기존 상태 API와 호환하기 위한 이름으로 제품 단위 이상 확률을 반환합니다. |
| `bulkWarningRate()`, `bulkFailureRate()` | 벌크 검사 단위 주의·이상 확률을 반환합니다. |

`generatePackaging()`은 제품당 난수를 한 번 추첨하므로 100개 중 이상 제품이 0개 또는 여러 개일 수도 있습니다.
시트·파우치·에센스·총중량의 생성 관계와 상세 확률은 아래 검사 데이터 생성 확률 절을 참고하세요.

### 5. Simulation_Controller.java — 생성기 제어·조회 창구

[소스 보기](src/main/java/springproject/controller/Simulation_Controller.java)

`/mask/simulation` 요청을 `Simulation_Service`에 전달합니다.

| 메서드 | API | 기능 |
|---|---|---|
| `start()` | `POST /start` | 생성기 활성화·실패 요청 재시도 허용 |
| `stop()` | `POST /stop` | 생성 중지 |
| `status()` | `GET /status` | 현재 상태 조회 |
| `samples()` | `GET /samples` | 최근 성공 기록 조회 |

`POST /mask/simulation/start`는 생성기 제어입니다. 실제 첫 공정 시작은
`Process_execution_Controller.save()`의 `POST /mask/process-executions`를 통해 처리합니다.

### 클래스 간 호출 흐름과 기존 코드 연결

```text
Scheduler.generateRealtimeData()
  → Simulation_Service.tick()
    → SimulationDataGenerator: 공정별 값 생성
    → SimulationDataGenerator.preparePayload(): DTO 필드에 맞춰 연결 정보 추가
    → SimulationApiClient.save(): 기존 REST API 호출
      → 기존 Controller → Service → Repository → DB

화면의 공정 시작
  → Process_execution_Controller.save()
    → Process_execution_Service.save(): 실행 이력 저장·커밋
    → Simulation_Service.connectStartedProcess(): 측정 생성 시작

시간 경과 및 목표 건수 저장 완료
  → Simulation_Service.finishProcess()
    → SimulationApiClient.finishOrResume()
      → Process_execution_Service.finish()/resume(): 판정·알람·승인 조건 검사
    → Simulation_Service.applyCompletion(): 다음 공정 또는 대기 상태 반영
```

| 생성 대상 | 저장 Controller | 요청 DTO |
|---|---|---|
| LOT | `Batches_Controller` | `Batches_Dto` |
| 원료 칭량 | `Material_dispensing_Controller` | `Material_dispensing_Dto` |
| 가열·냉각 센서 | `Sensor_telemetry_Controller` | `Sensor_telemetry_Dto` |
| 벌크 검사 | `Bulk_qc_Controller` | `Bulk_qc_Dto` |
| 개별 포장제품 | `Filling_packaging_Controller` | `Filling_packaging_Save_Dto` |

공정 조회·시작·전환에는 `Process_execution_Dto`, 종료·재개 응답에는
`Process_execution_Finish_Dto`를 사용합니다. `Measurement_check_Service`는 생성기 외부의
판정 코드이며 `Process_execution_Service`에서 호출합니다.

## 실행과 설정

생성기는 Spring 서버 준비 후 기존 REST API에 요청하며 실제 MySQL에 데이터를 저장합니다.
서버 시작 시 대기 LOT 하나만 등록하고, 해당 LOT의 공정 시작 버튼을 누를 때까지
측정값은 생성하지 않습니다. LOT 생산 완료 후 설정된 대기시간(기본 10초)이 지나면
다음 대기 LOT 하나만 등록합니다. 다음 LOT도 공정 시작 버튼을 기다립니다.
생성기 자체는 최종 이상 판정을 하지 않으며 공정 종료 시 기존 Service의 판정 로직을 거칩니다.
SSE는 포함하지 않습니다. 초기 SQL 파일에 내용을 덧붙이는 기능도 아닙니다.

`application.properties`에서 필요에 따라 다음 값을 설정합니다.

```properties
simulation.enabled=true
simulation.api-base-url=http://localhost:8080
simulation.user-id=1
simulation.tick-delay-ms=1000
simulation.process-duration-seconds=20
simulation.lot-delay-ms=10000
simulation.sensor-ticks=10
simulation.packaging-units=100
simulation.packaging-anomaly-rate=0.01
simulation.bulk-warning-rate=0.01
simulation.bulk-failure-rate=0.01
```

`user-id`는 DB에 존재하는 작업자 번호입니다. 서버 포트를 바꾸면 API 주소도 맞추세요.
API 주소를 생략하면 `server.port`를 사용한 localhost 주소가 기본값입니다.
자동 생성을 끄려면 `simulation.enabled=false`를 설정합니다.

모든 공정은 서버가 반환한 실제 start_time을 기준으로 20초 동안 진행합니다.
스케줄러는 기본 1초마다 확인하며, 공정별 목표 건수를 20초 구간에 분산 생성합니다.
기본 포장 목표 100개는 20초 동안 주기별로 분산 저장합니다. 기본 1초 주기라면 목표상 약 5개씩이며,
통신 지연이 있으면 이후 주기에 필요한 건수를 추가 생성합니다.
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
첫 공정 POST는 화면의 공정 시작 버튼이 호출하며 생성기는 시작 API를 호출하지 않습니다.
기존 Process_execution_Service.save()가 시작 이력과 담당관리자를 저장하고,
Service의 트랜잭션 커밋이 성공하면 시작 Controller가
Simulation_Service.connectStartedProcess()로 실제 실행번호를 연결합니다. 별도 이벤트는 사용하지 않습니다.
시작 대기 중에는 실행 이력을 주기적으로 조회하지 않으며 측정값도 생성하지 않습니다.
React 공정 모니터링 화면의 LOT 입력칸 위에 현재 대기·진행 LOT를 표시합니다.
상태 API를 2초마다 조회하고, 생산 완료 후 다음 대기 LOT가 등록되면 안내 번호가 갱신됩니다.
번호 표시는 사용자가 입력하거나 조회 중인 LOT를 자동으로 변경하지 않습니다.
생성기가 process_execution에 별도의 가상 실행을 INSERT하지 않습니다.
측정 데이터에는 실제 execution_id가 연결됩니다. 원료 칭량은 기존 구조대로 batch_id로 연결됩니다.

온도 상승·하락 과정은 생성하지 않습니다. 가열은 62.0~63.0℃, 냉각은 원료 투입 전
37.5~38.5℃ 범위에서 매번 독립적으로 생성합니다. stage는 사용하지 않으며
실행번호에 연결된 공정명으로 가열/혼합과 냉각을 구분합니다.

## 검사 승인과 공정 대기

### 검사 데이터 생성 확률

- 4공정은 검사 한 건마다 정상 98%, 주의 1%, 이상 1% 시나리오를 선택합니다.
  정상은 pH 5.9~6.1, 점도 2300~2700, 외관·미세기포 합격으로 생성합니다.
  주의는 pH 또는 점도 하나만 주의 구간으로, 이상은 수치 또는 외관·미세기포 하나만 이탈시킵니다.
- 5공정은 LOT당 100개를 만들고 제품 한 개마다 정상 99%, 이상 1%를 독립 추첨합니다.
  매 LOT마다 반드시 99개 합격·1개 불합격으로 고정하는 방식은 아닙니다.
  이상 제품은 충진량, 상부 실링, 하부 실링, 금속검사, 비전검사 중 원인 하나를 선택합니다.
- 시트·파우치 중량은 정상 범위에서 작게 변동하며 에센스는 1·2차 충진량의 합과 일치합니다.
  총중량은 시트+파우치+에센스 합입니다. 에센스 충진 이상은 총중량·중량검사에도 함께 반영되므로
  원인 하나가 여러 이상 항목으로 판정될 수 있습니다. 알람을 묶는 기능은 변경하지 않습니다.
- 생성 범위는 기본 규칙 기준입니다. DB의 규칙을 변경하면 실제 판정 비율은 달라질 수 있습니다.
  이 확률은 생성 시나리오이며 최종 판정·승인 결과를 강제로 저장하지 않습니다.
- 기존 `simulation.inspection-failure-rate`는 새 packaging-anomaly-rate가 없을 때의 대체 설정입니다.

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
`ddl-auto=update` 설정이면 서버 시작 시 DB 컬럼과 unique 인덱스가 반영됩니다.
스키마 자동 변경을 끈 환경은 운영 방식에 맞는 마이그레이션을 먼저 적용해야 합니다.
생성기는 data_mask.sql을 실행하지 않습니다. 초기 SQL 실행 여부는 Spring 초기화 설정이 결정합니다.
현재 `application.properties`는 `ddl-auto=create-drop`, `spring.sql.init.mode=always`이므로
서버 재시작 시 테이블 재생성과 초기 SQL 입력이 수행됩니다. `update`·`never` 구성과 구분하세요.

센서 source_sample_id와 원료·검사·제품의 고유번호로 실패 후 재전송 중복을 방지합니다.
저장 실패 시 자동 중지되며 status의 lastError를 해결한 뒤 start를 호출합니다.
생성기 상태와 미저장 요청은 메모리에 있으므로 서버 재시작 후 복구되지는 않습니다.
데이터 유지 설정에서는 재시작해도 이전 LOT는 DB에 남지만 생성기는 새 LOT를 만듭니다.
현재 create-drop 설정에서는 이전 생성 데이터가 유지되지 않습니다.

## 검증

```powershell
.\gradlew.bat verifySimulationApi --offline
```

위 명령은 `springproject.SimulationApiCheck` 테스트 클래스가 있을 때 사용할 수 있습니다.
현재 `src/test`에는 해당 소스가 없어 이 명령만으로 전체 흐름 검증이 실행되지는 않습니다.
설정값·Controller·DB를 연결한 통합 검증과, 난수 생성·확률·중량 관계 검증은 구분해야 합니다.
