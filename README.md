# 🚀 대규모 트래픽 처리를 위한 분산 쿠폰/포인트 시스템

## 📄 프로젝트 개요

본 프로젝트는 실제 E-commerce 환경에서 발생할 수 있는 대규모 동시 접속 상황을 가정하여, 선착순 쿠폰 발급 및 포인트 시스템을 안정적으로 처리할 수 있는 백엔드 시스템을 설계하고 구축하는 것을 목표로 합니다.

단순한 기능 구현을 넘어, 성능 테스트 도구(k6, nGrinder)와 모니터링 시스템(Prometheus, Grafana)을 적극적으로 활용하여 시스템의 병목 지점을  분석하고, 비동기 처리, 분산 캐싱, 데이터베이스 이중화 등 다양한 기술을 적용하여 시스템을 점진적으로 개선해 나가는 과정에 집중했습니다.

## ✨ 주요 기능

* 선착순 쿠폰 발급: 대규모 동시 요청에도 데이터 정합성을 보장하는 비동기 쿠폰 발급 API

* 쿠폰 사용 및 조회: 비관적 락(Pessimistic Lock)을 이용한 안전한 쿠폰 사용 처리 및 조회 API

* 포인트 시스템: 쿠폰과 연계하여 사용할 수 있는 포인트 적립/사용/소멸 기능의 기반 설계

* 통합 모니터링 및 알림: Prometheus, Grafana, Alertmanager를 연동하여 시스템의 성능을 실시간으로 모니터링하고, 이상 징후 발생 시 Discord로 자동 알림 수신


## 🏗️ 아키텍처 설계 및 발전 과정

## 1. 🚀 [이슈] 포인트 사용: 낙관적 락 VS 비관적 락

초기에는 포인트 사용이나 적립이 동시에 발생할 일은 거의 없을 것이라고 가정하였습니다. 즉, 대부분의 트랜잭션 충돌이 없을 것이라 가정하여 낙관적 락을 선택했습니다. 

하지만 테스트 결과 데드락이 발생하였다. 로그를 통해 확인해보니 S-LOCK이 걸려 있는데 X-LOCK을 획득하려는 과정에서 발생한 것이었다. 
* S-LOCK: 데이터를 읽을 때 사용하는 락
* X-LOCK: 데이터를 변경할 때 사용하는 락

```java
public PointResponse use(Long partnerId, String userId, int amount, String description) {

        PointWallet wallet = pointWalletRepository.findByPartnerIdAndUserId(partnerId, userId)
                .orElseGet(() -> {
                    PointWallet newWallet = PointWallet.create(partnerId, userId);
                    return pointWalletRepository.save(newWallet);
                });

        wallet.use(amount);

        PointHistory history = PointHistory.builder()
                .pointWallet(wallet)
                .transactionType(TransactionType.USE)
                .amount(amount)
                .description(description)
                .build();

        pointHistoryRepository.save(history);

        return PointResponse.from(wallet);
    }
```
이 코드와 dead lock history 로그를 통해 deadlock 발생 시나리오를 작성할 수 있었습니다. 

이 때, 설명의 편의를 위해 두 트랜잭션이 경쟁한다고 가정하겠습니다.

1. **동시 진입 및 s-lock획득:** 
    - 두 트랜잭션 모두 pointWalletRepository.findByPartnerIdAndUserId(...)를 실행합니다. 이 SELECT 쿼리는 point_wallets 테이블의 row를 읽기 위해 접근합니다.
    - 로그를 보면, 두 트랜잭션 모두 **lock mode S** 를 **HOLDS THE LOCK(S)** 하고 있습니다. 즉, 두 트랜잭션 모두 id=26인 row에 대해 **읽기(공유) 락을 동시에 획득**하는 데 성공했습니다.
2. **낙관적 락을 위한 업데이트(UPDATE) 시도:**
    - 두 스레드 모두 메모리 상에서 wallet.use(amount)를 실행합니다.
    - 이제 트랜잭션이 끝나는 시점에, JPA는 변경된 PointWallet 엔티티를 DB에 반영하기 위해 UPDATE 쿼리를 실행하려고 합니다. 이 UPDATE 쿼리에는 @Version 때문에 WHERE ... AND version=0 조건이 포함되어 있습니다.
    - UPDATE는 데이터를 변경해야 하므로, 기존의 공유 락(S-Lock)을 **배타적 락(X-Lock)으로 업그레이드**하려고 시도합니다.
3. **deadlock 발생**
    - 스레드 A
        - **WAITING FOR THIS LOCK**: id=26인 row에 대해 **배타적 락(X-Lock)을 획득하려고 대기**합니다.
        - **왜 대기하나?** 스레드 B가 동일한 row에 대해 **공유 락(S-Lock)을 쥐고 놓아주지 않고 있기 때문**입니다. (X-Lock은 다른 어떤 락과도 공존할 수 없습니다.)
    - 스레드 B
        - **WAITING FOR THIS LOCK**: id=26인 row에 대해 **배타적 락(X-Lock)을 획득하려고 대기**합니다.
        - **왜 대기하나?** 스레드 A가 동일한 row에 대해 **공유 락(S-Lock)을 쥐고 놓아주지 않고 있기 때문**입니다.
    - **결과:** 스레드 A는 스레드 B가 끝나기를 기다리고, 스레드 B는 스레드 A가 끝나기를 기다리는, **전형적인 순환 대기 상태, 즉 데드락**이 완성되었습니다.
4. **MySQL의 해결**
    - InnoDB 엔진은 이 순환 대기를 감지하고, 둘 중 하나의 트랜잭션(로그에서는 TRANSACTION (2))을 희생양으로 삼아 강제로 롤백시켜 데드락을 풀어버립니다.


앞서 발생한 데드락으로 인해 낙관적 락으로는 해당 문제를 해결하기 힘들다고 생각하여 비관적 락을 도입했습니다.

기존의 시나리오는 비관적 락을 도입하여 해결할 수 있었습니다. 

``` java

@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select p from PointWallet p where p.partnerId = :partnerId and p.userId = :userId")
Optional<PointWallet> findByPartnerIdAndUserIdWithLock(
        @Param("partnerId") Long partnerId,
        @Param("userId") String userId
);

```

- 스레드 A가 SELECT ... FOR UPDATE로 point_wallets row에 X-Lock을 획득합니다.
- 스레드 B는 SELECT ... FOR UPDATE를 실행하지만, 스레드 A가 X-Lock을 쥐고 있으므로 **SELECT 단계에서부터 대기(Wait)**합니다.
- 애초에 두 스레드가 동시에 락을 잡는 상황 자체가 발생하지 않으므로, '락 업그레이드 경쟁'으로 인한 데드락은 **원천적으로 차단**됩니다.

하지만 다른 시나리오의 경우에는 비관적 락을 사용하더라도 데드락이 발생할 수 있습니다.

### 시나리오: 포인트 이전(Transfer) 기능

사용자 A가 사용자 B에게 포인트를 이전하는 기능을 만든다고 가정

- **트랜잭션 1 (스레드 1):** 사용자 A -> 사용자 B에게 100점 이전
    1. 사용자 A의 point_wallets row에 SELECT ... FOR UPDATE로 **락을 건다.** (성공)
    2. (어떤 로직 처리 후...)
    3. 사용자 B의 point_wallets row에 SELECT ... FOR UPDATE로 **락을 걸려고 한다.**
- **트랜잭션 2 (스레드 2):** 거의 동시에, 사용자 B -> 사용자 A에게 50점 이전
    1. 사용자 B의 point_wallets row에 SELECT ... FOR UPDATE로 **락을 건다.** (성공)
    2. (어떤 로직 처리 후...)
    3. 사용자 A의 point_wallets row에 SELECT ... FOR UPDATE로 **락을 걸려고 한다.**

### 교착 상태 (Deadlock) 발생!

| **시간** | **트랜잭션 1 (A -> B)** | **트랜잭션 2 (B -> A)** |
| --- | --- | --- |
| **T1** | **A의 row에 락 획득** |  |
| **T2** |  | **B의 row에 락 획득** |
| **T3** | B의 row에 락을 걸려고 시도 -> **대기!** (트랜잭션 2가 락을 쥐고 있음) |  |
| **T4** |  | A의 row에 락을 걸려고 시도 -> **대기!** (트랜잭션 1이 락을 쥐고 있음) |

이러한 종류의 데드락을 방지하기 위한 가장 일반적인 규칙은 "락 획득 순서를 항상 동일하게 유지하는 것"입니다.

### 결론

다행히도, 우리가 현재 구현하는 pointService.use() 메서드는 **오직 하나의 point_wallets row에만** 락을 겁니다. 여러 사용자의 지갑을 동시에 잠그는 로직이 없습니다.

따라서, **현재의 use 메서드에 한해서는 비관적 락을 적용하면 데드락이 발생할 가능성이 거의 없다**고 볼 수 있습니다.

- **비관적 락은 만병통치약이 아니다.** 락 획득 순서가 꼬이면 여전히 데드락이 발생할 수 있다.
- 하지만 비관적 락은 우리가 겪었던 **'락 업그레이드 경쟁' 데드락은 확실하게 막아준다.**
- 우리의 **현재 시나리오(단일 지갑 차감)는** 여러 자원의 락 순서가 꼬일 일이 없으므로, **비관적 락을 사용하는 것이 안전하고 효과적인 해결책이다.**


## 2. 🚀 [성능 개선] 선착순 쿠폰 발급 시스템: 비관적 락의 한계를 넘어 비동기 아키텍처로

### 1. 초기 접근: 비관적 락을 이용한 동기 처리

가장 먼저 고려한 것은 **데이터 정합성**이었습니다. 선착순 쿠폰은 정확히 약속된 수량만 발급되어야 합니다. 이를 위해, 여러 요청이 동시에 마지막 쿠폰에 접근하더라도 단 하나의 요청만 성공하도록 보장해주는 JPA의 비관적 락(Pessimistic Lock)을 도입했습니다.

- **구현:** CouponTemplate 조회 시 SELECT ... FOR UPDATE 쿼리를 발생시키는 @Lock(LockModeType.PESSIMISTIC_WRITE)를 적용.
- **장점:** 데이터 정합성을 100% 보장하는, 간단하고 확실한 방법이었습니다.

> 🤔 초기 가설: "비관적 락으로 데이터 정합성만 지키면, 선착순 문제는 해결될 것이다."
> 

### 2. 문제 발견: nGrinder 성능 테스트와 마주한 현실

이 가설을 검증하기 위해 **nGrinder**를 사용하여 부하 테스트를 진행했습니다.

- **테스트 조건:** 가상 사용자(VUser) 500명, 쿠폰 10,000개
- **테스트 목표:** 시스템이 안정적으로 부하를 견디며, 어느 정도의 처리량(TPS)을 보이는지 확인

테스트 결과는 다음과 같았습니다.

- **DB CPU 사용률 100%:** docker stats로 확인한 MySQL 컨테이너의 CPU 사용률이 테스트 시작과 동시에 100%에 근접하며 병목 현상이 발생했습니다.
- **급격한 TPS 저하 및 응답 시간 증가:** 테스트 초반 잠시 높은 TPS를 보이다가, DB 락 경합(Lock Contention)이 심화되면서 TPS는 급격히 떨어지고 평균 응답 시간은 수천 ms까지 치솟았습니다.
- **커넥션 풀 고갈:** JMC로 확인한 결과, HikariCP의 모든 DB 커넥션이 고갈되어 대기하는 스레드가 대량으로 발생했습니다.

<br><details><summary>📋 **Before: 비관적 락 방식 테스트 결과 데이터 (클릭하여 펼치기)**</summary>

<img src="image/스크린샷 2025-09-11 150745.png" alt="개선 전 성능 그래프" width="700"/>
<img src="image/image (1).png" alt="개선 전 성능 그래프" width="700"/>
<img src="image/image (2).png" alt="개선 전 성능 그래프" width="700"/>

</details><br>

### 3. 해결 방안 설계: Redis와 RabbitMQ를 활용한 비동기 처리

문제를 해결하기 위해, DB의 부담을 덜어주고 사용자 경험을 향상시키는 **비동기(Asynchronous) 아키텍처**로 전환을 결정했습니다.

> 💡 핵심 아이디어
> 
> 
> "선착순 줄 세우기를 느린 DB가 아닌, 매우 빠른 메모리(Redis)에서 처리하고, 실제 DB 작업은 메시지 큐(RabbitMQ)를 통해 나중에 안정적으로 처리하자!"
> 

**개선된 아키텍처 흐름:**

1. **[1차 방어선: Redis]** API 서버는 요청을 받으면 DB 대신 Redis에 먼저 접근합니다.
    - **중복 발급 체크:** Redis Set 자료구조(SADD)를 이용해 사용자가 이미 참여했는지 원자적으로 확인합니다.
    - **수량 체크:** Redis의 INCR 명령어를 이용해 현재 발급 시도 횟수를 원자적으로 카운트하고, 총 수량을 넘었는지 즉시 판단합니다.
2. **[빠른 실패/성공 응답]** Redis 체크를 통과하지 못한 요청은 DB에 도달하기도 전에 "소진되었습니다" 또는 "이미 받으셨습니다" 라는 실패 응답을 즉시 받습니다. Redis 체크를 통과한 요청은 **'성공 대상'**으로 간주됩니다.
3. **[부하 제어: RabbitMQ]** '성공 대상'이 된 요청 정보만 메시지 큐(RabbitMQ)에 메시지로 전송하고, API 서버는 사용자에게 "쿠폰이 발급되었습니다" 라는 **성공 응답**을 즉시 보냅니다.
4. **[안정적인 후처리: Consumer]** 별도의 프로세스로 동작하는 RabbitMQ Consumer는 큐에 쌓인 메시지를 자신의 처리 속도에 맞춰 순차적으로 가져와서, **실제 DB에 쿠폰 데이터를 INSERT**하고 수량을 UPDATE 하는 작업을 수행합니다.

### 4. 개선 결과 검증: 다시 nGrinder로

새로운 아키텍처를 구현한 뒤, **완전히 동일한 조건**으로 nGrinder 성능 테스트를 다시 진행했습니다.

- **높아진 TPS 및 응답 시간 개선:** API 서버는 더 이상 DB를 기다리지 않으므로, 이전과는 비교하여 TPS는 약 8배정도 상승하였습니다. 응답시간 또한 기존대비 80% 감소하였습니다.

<br><details><summary>📊 **After: Redis+MQ 방식 테스트 결과 데이터 (클릭하여 펼치기)**</summary>

<img src="image/image (3).png" alt="개선 전 성능 그래프" width="700"/>
</details><br>

### 5. 여전한 문제:

아키텍처를 변경했음에도 여전히 남아있는 문제점들이 있었습니다. 

- **DB CPU 사용률 :** DB의 부하는 여전히 높았습니다.
- **아직 부족한 TPS:** TPS는 상승하였지만 그럼에도 부족하다고 느껴졌습니다.
- **RABBITMQ 부하:**  RABBITMQ 를 도입하여 CPU사용률을 관찰했을 때, 90-100%이상의 CPU사용이 쿠폰이 발급되는 동안 지속적으로 높게 나타나는 모습을 보였습니다.

### 6. 최종 결론

비관적 락을 사용한 초기 동기 방식은 데이터 정합성을 보장하는 가장 간단한 방법이었지만, 대규모 동시 요청 환경에서는 시스템 전체를 마비시키는 병목 지점이 되었습니다.

**Redis를 이용해 선착순 로직을 메모리단에서 처리**하고, **RabbitMQ를 이용해 DB 작업을 비동기화**함으로써, 더 좋은 결과를 얻을 수 있었습니다. 

1. **사용자 경험:** 사용자는 DB 상태와 무관하게 거의 즉시 성공/실패 결과를 받아봅니다.


## 3. 🚀 [성능 개선] 보이지 않는 병목, API 인증 인터셉터 최적화기

애플리케이션의 성능을 이야기할 때, 우리는 흔히 복잡한 비즈니스 로직이나 무거운 쿼리에 집중하곤 합니다. 하지만 때로는 가장 단순하고 반복적인 작업이 시스템 전체의 발목을 잡는 '숨겨진 암살자'가 되기도 합니다.

이번에는 nGrinder와 모니터링 툴을 통해, 모든 API의 관문 역할을 하던 **인증 인터셉터(Interceptor)의 성능 병목**을 발견하고 **Redis 캐싱**으로 해결한 과정을 공유합니다.

### 1. 문제의 발단: "비즈니스 로직은 가벼운데, 왜 DB는 힘들어할까?"

선착순 쿠폰 발급 기능을 비동기 아키텍처로 개선한 후, 시스템의 전반적인 처리량(TPS)은 크게 향상되었습니다. 하지만 여전히 석연치 않은 부분이 있었습니다. 바로 **DB의 CPU 사용량**이었습니다.

> 🤔 의문점
> 
> 
> 쿠폰 발급 요청은 이제 Redis에서 대부분 처리되고 DB로 가지 않는데도, nGrinder로 높은 부하를 가하면 여전히 DB의 CPU 사용률이 예상보다 높게 나타났습니다. INSERT 작업이 없는 단순 조회 API에 부하를 가해도 비슷한 현상이 발생했습니다.
> 

이는 비즈니스 로직 외에, **모든 API 요청이 공통적으로 거치는 구간** 어딘가에 DB 부하를 유발하는 원인이 숨어있는 것이었습니다.

### 2. 원인 추적: 모니터링과 코드 분석

범인을 찾기 위해 몇 가지 도구를 활용하여 시스템을 분석했습니다.

Grafana, DB export를 통해 DB를 모니터링 하였고 예상하지 못했던 select 쿼리가 서비스 초반에 많이 발생하는 것을 발견했습니다. 

쿠폰 발급 로직에는 select쿼리가 대량으로 발생하는 부분이 없었기 때문에 비즈니스 로직 외 다른 부분을 조사하였습니다.

**2. 코드 역추적:**

이 쿼리가 어디서 실행되는지 코드를 역추적한 결과, 범인은 바로 **ApiKeyAuthInterceptor** 였습니다.



```java
// ApiKeyAuthInterceptor.java (Before)
@Override
public boolean preHandle(HttpServletRequest request, ...) {
    String apiKey = request.getHeader("X-API-KEY");
    // ...
    // [문제의 지점] 모든 API 요청마다 DB를 조회
    Partner partner = partnerRepository.findByApiKey(apiKey)
            .orElseThrow(...);
    // ...
    return true;
}
```

모든 API 요청이 통과해야 하는 '관문'에서, 매번 DB에 신분증(API Key) 조회를 요청하고 있었던 것입니다. VUser 500명이 초당 200번씩 요청을 보내면, DB에는 초당 200개의 SELECT 쿼리가 그대로 전달되고 있었습니다.

> ❗️ 문제 정의
> 
> 
> ApiKeyAuthInterceptor의 인증 로직이 **모든 API 요청마다 DB 조회**를 유발하여, 시스템 전체에 불필요한 DB 부하를 가하고 있었습니다. 파트너 API키 정보는 거의 변경되지 않는 데이터임에도 불구하고, 매번 DB에 접근하는 것은 매우 비효율적이었습니다.
> 

### 3. 해결 방안 설계: Redis를 이용한 인증 정보 캐싱

이 문제를 해결하기 위한 가장 이상적인 방법은, 자주 조회되지만 거의 변경되지 않는 파트너 인증 정보를  **Redis에 캐싱**하는 것입니다.

**개선된 인증 흐름:**

1. **[Cache-Aside 패턴]** 인터셉터는 요청을 받으면, 먼저 **Redis에 API Key가 있는지 확인**합니다.
2. **(Cache Hit)** Redis에 Key가 존재하면, DB를 조회하지 않고 즉시 캐시된 파트너 정보를 사용하여 인증을 통과시킵니다. (대부분의 요청이 이 경로를 따름)
3. **(Cache Miss)** Redis에 Key가 없으면, **그때서야 DB에 접근**하여 파트너 정보를 조회합니다.
4. DB에서 조회한 정보는 다음에 재사용할 수 있도록 **Redis에 저장**한 뒤, 인증을 통과시킵니다.

### 4. 개선 결과 검증:

인증 로직에 Redis 캐싱을 적용한 후, 동일한 조건으로 k6 부하 테스트를 다시 진행했습니다.

- **DB CPU 사용률 대폭 감소:** SELECT ... FROM partners 쿼리가 거의 발생하지 않게 되면서, DB CPU 사용률이 **평균 20% 미만**으로 매우 안정적으로 유지되었습니다.
- **전체 TPS 향상:** DB의 부하가 줄어들자, 시스템 전체가 더 많은 요청을 처리할 수 있게 되어 전체적인 TPS 또한 소폭 상승했습니다.

| **지표** | **Before (매번 DB 조회)** | **After (Redis 캐싱 적용)** |
| --- | --- | --- |
| **SELECT  쿼리 수** | 3.400(MAX) | 67(MAX) |
| **DB CPU 사용률 (max)** | 120%(MAX) | **25%(MAX)** |

<br><details><summary>📊 **Before/After 비교 데이터 (클릭하여 펼치기)**</summary>


<BEFORE>
개선 전 (Before)

<img src="image/스크린샷 2025-08-26 오전 11.29.04.png" alt="before1" width="700"/>
<img src="image/스크린샷 2025-08-26 오전 11.30.12.png" alt="before2" width="700"/>


<br>

<AFTER>
개선 후 (After)

<img src="image/스크린샷 2025-08-27 오후 12.00.53.png" alt="after1" width="700"/>
<img src="image/스크린샷 2025-08-27 오후 12.00.23.png" alt="after2" width="700"/>


</details><br>

### 5. 최종 결론

이번 성능 개선을 통해, 복잡한 비즈니스 로직뿐만 아니라 **애플리케이션의 공통 로직(인증, 로깅 등) 또한 성능에 미치는 영향이 매우 크다**는 것을 다시 한번 확인했습니다.

단순히 기능을 구현하는 것을 넘어, **모니터링을 통해 시스템의 병목 지점을 데이터 기반으로 찾아내고, 캐싱과 같은 적절한 기술을 적용하여 문제를 해결**하는 것이 안정적인 서비스를 만들기 위한 백엔드 개발자의 핵심 역량임을 깨달았습니다.


## 4. 🚀 [성능 개선] 쿠폰 발급 Consumer 성능 최적화: JPA `saveAll`에서 `JdbcTemplate`으로의 전환

### 1. 문제 발견: 비동기 처리의 새로운 병목, 데이터베이스

선착순 쿠폰 발급 시스템의 API 응답 속도를 개선하기 위해 RabbitMQ를 도입하여 DB 저장을 비동기 방식으로 전환했습니다. 이로써 사용자 요청은 빨라졌지만, 이제는 초당 수천 개의 메시지를 처리해야 하는 **Consumer의 DB 쓰기 작업**이 새로운 병목 지점으로 떠올랐습니다.

RabbitMQ Consumer가 대량의 메시지를 배치(Batch)로 처리할 때, 예상보다 DB `INSERT` 성능이 나오지 않는 것을 확인했습니다. `repository.saveAll()`로 작성된 배치 전략이 예상과는 달리 개별 `INSERT` 로 동작하였고, 이는 JPA의 쓰기 지연 및 배치 INSERT 최적화를 방해하는 주된 원인이었습니다.

단순히 `for` 루프 안에서 `repository.save()`를 호출하는 방식은, 처리해야 할 메시지 수만큼 DB와의 네트워크 통신(Round Trip)과 트랜잭션을 발생시켜, 대규모 트래픽 상황에서 DB에 극심한 부하를 주었습니다.



### 2. 1차 해결책: JPA Batch Insert (`saveAll` + `SEQUENCE`)

가장 먼저 JPA가 제공하는 배치(Batch) 처리 기능을 활용하여 DB와의 통신 횟수를 줄이는 것을 목표로 했습니다.

### 가. `IDENTITY` 전략의 한계 발견

MySQL 환경에서 사용하는 `GenerationType.IDENTITY`(`AUTO_INCREMENT`) 전략은, `INSERT` 쿼리가 DB에서 실행된 **후에야** ID를 알 수 있습니다. 이 때문에 JPA는 여러 `INSERT`를 하나의 배치로 묶어 보내지 못하고, 결국 `saveAll()`을 사용하더라도 내부적으로는 개별 `INSERT`를 실행하는 것과 같은 비효율이 발생했습니다.

### 나. `SEQUENCE` 전략으로 전환, 하지만..?

이 문제를 해결하기 위해, `INSERT` **전에** ID를 미리 할당받을 수 있는 `GenerationType.SEQUENCE` 전략으로 변경하고자 했습니다.

하지만 MYSQL은 `SEQUENCE`를 지원하지 않았고, Hibernate가 **`hibernate_sequence` 테이블**을 통해 이를 흉내 내는 방식으로 동작했습니다. 

추가적인 테이블과 관리가 필요하다는 점과 JPA의 `saveAll`은 매우 효율적이지만, 내부적으로는 여전히 영속성 컨텍스트(Persistence Context) 관리, Dirty Checking 등 JPA 계층의 오버헤드가 존재한다는 단점이 있었습니다. 

실시간으로 수만 건의 메시지를 처리해야 하는 Consumer 로직에서는 이 미세한 오버헤드마저 제거하여, **가장 순수한 JDBC 레벨의 최고 성능**을 확보할 필요가 있다고 판단했습니다.

---

### 4. 최종 해결책: `JdbcTemplate.batchUpdate()` 도입

JPA/Hibernate 계층을 완전히 우회하여, JDBC 드라이버가 제공하는 가장 최적화된 방식으로 배치 `INSERT`를 실행하기 위해 `JdbcTemplate`을 도입했습니다.

### 가. `JdbcTemplate`을 사용한 배치 INSERT 구현

`issueCouponsInBatch` 메소드를 수정하여, Consumer가 받은 `List<CouponIssueMessage>`를 기반으로 순수 `INSERT` SQL을 실행하도록 변경했습니다.


```java
private void issueCouponsInBatchByJdbc(List<CouponIssueMessage> messages) {
    String sql = "INSERT INTO coupons (...) VALUES (?, ?, ...)";

    jdbcTemplate.batchUpdate(sql,
            messages,
            100, // Batch Size
            (PreparedStatement ps, CouponIssueMessage message) -> {
                // PreparedStatement에 직접 파라미터 설정
                ps.setLong(1, message.getPartnerId());
                // ...
            });
}
```

### 나. 대용량 데이터 처리: `OutOfMemoryError`와 청킹(Chunking)

10만 건 이상의 대규모 데이터를 한 번에 처리하려 할 때, JDBC 드라이버가 거대한 SQL 쿼리 문자열을 생성하다가 `OutOfMemoryError: Java heap space`가 발생하는 문제를 발견했습니다.

이를 해결하기 위해, Google의 Guava 라이브러리를 사용하여 전체 메시지 리스트를 **10000개 단위의 작은 묶음(Chunk)으로 나누고**, 각 묶음에 대해 `batchUpdate`를 반복 실행하는 **청킹(Chunking)** 기법을 적용했습니다. 이로써 메모리 사용량을 안정적으로 유지하면서 대용량 데이터를 처리할 수 있게 되었습니다.

```java
List<List<CouponIssueMessage>> partitionedMessages = Lists.partition(messages, 1000);

for (List<CouponIssueMessage> chunk : partitionedMessages) {
    jdbcTemplate.batchUpdate(sql, chunk, ...);
}
```

---

### 5. 최종 성능 비교 및 결론

`BatchInsertPerformanceTest`를 통해 여러 데이터를 기준으로 두 방식의 성능을 측정한 결과, `JdbcTemplate.batchUpdate`가 JPA의 `saveAll`보다 **더 빠르고 안정적인 성능**을 보여주었습니다.

```java
    @Test
    @DisplayName("성능 테스트: 메시지 x건으로 쿠폰 생성 및 수량 업데이트 동시 처리")
    @Transactional
    void issue_and_update_quantity_batch_test() {
        // given
        StopWatch stopWatch = new StopWatch();

        // when
        stopWatch.start();
        couponIssueSyncService.issueCouponsAndUpdateQuantityInBatch(testMessages);
        stopWatch.stop();

        // then
        System.out.println("--- 전체 배치 작업 (INSERT + UPDATE) 실행 시간 ---");
        System.out.println("Total Time (ms) for " + DATA_SIZE + " messages: " + stopWatch.getTotalTimeMillis());

        long actualCouponCount = couponRepository.count();
        CouponTemplate updatedTemplate = couponTemplateRepository.findById(template.getId()).orElseThrow();

        assertThat(actualCouponCount).isEqualTo(DATA_SIZE);
        assertThat(updatedTemplate.getIssuedQuantity()).isEqualTo(DATA_SIZE);
    }
```


| 방식 | 1000건 | 10_000건 | 100_000건 |
| --- | --- | --- | --- |
| **JPA `saveAll`** | 886ms | 5995ms | 53254ms |
| **`JdbcTemplate.batchUpdate`** | 161ms | 586ms | 3640ms |
| 개선 비율 | 5.5배 | 10.2배 | 14.6배 |

**결론적으로,** `JdbcTemplate`을 직접 사용하는 방식은 코드의 복잡성이 약간 증가하는 대신, 실시간으로 대량의 쓰기 작업이 발생하는 메시지 큐 Consumer 로직에서 **최고의 성능과 안정성을 보장하는 가장 확실한 아키텍처**임을 증명했습니다.




## ⚡️ 성능 개선 결과: 최종 비교
초기의 동기 방식 아키텍처와 최종적인 비동기 아키텍처의 성능을 k6를 사용하여 동일한 조건에서 측정한 결과, 다음과 같은 극적인 성능 향상을 확인할 수 있었습니다.

| 지표 | 개선전 | 개선 후 | 개선 결과 |
| --- | --- | --- | --- |
| 평균 처리량(RPS) | 약 177 RPS | 약 4300+ RPS | 약 24배 이상 성능 향상 |
| 응답 시간(P95) | 4.17s | 225ms | 응답 시간 18배 이상 속도 향상(95%단축) |

<br>


개선 전 (Before)

<img src="image/스크린샷 2025-09-11 150628.png" alt="개선 전 성능 그래프" width="700"/>

<br>

개선 후 (After)

<img src="image/스크린샷 2025-09-11 143407.png" alt="개선 후 성능 그래프" width="700"/>


## 👑 배운점

현재의 모습이 완벽한 설계라고는 생각하지 않고 더 발전시킬 부분이 많다고 생각합니다. 그럼에도 선착순 쿠폰 발급의 경우 처음의 설계와 비교했을 때, 유의미한 성능 향상을 이뤄낼 수 있었습니다. 

1. **성능 개선은 단순히 추측으로만 하면 안된다** -> 이전 프로젝트에서는 단순히 이론적인 추측으로만 성능을 고려했지만 실제 테스트를 진행했을 때, 이론과 다른 경우가 많았습니다. 직접 테스트를 해봐야지만 문제점이 무엇인지 파악할 수 있습니다.

2. **기술은 트레이드 오프 관계가 많다** -> 기술을 적용할 때, 특정 기술이 언제나 무조건 좋은 경우는 거의 없는 것 같습니다. 각 기술마다 장단점이 있기 때문에 본인의 상황에 맞게 잘 적용해야 합니다.





