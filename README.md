# 사용자 인증 기능 구현 (Authentication)

## 1. 프로젝트 개요

모바일 애플리케이션 환경을 기준으로 Spring Security 기반의 사용자 인증 구조를 학습하고 구현한 프로젝트이다.

이메일과 비밀번호를 이용한 로컬 로그인, OAuth 기반 소셜 로그인, 게스트 계정 로그인 및 게스트 계정의 로컬 계정 전환 기능을 구현하며, JWT를 활용한 모바일 클라이언트의 인증 흐름을 실습한다.

**주요 목표**
- 모바일 클라이언트와 서버 간 인증 구조 이해
- Spring Security 기반 인증 및 인가 구조 이해
- JWT 발급 및 모바일 클라이언트의 토큰 관리
- 비밀번호 암호화 및 검증
- OAuth 2.0 기반 소셜 로그인 구현
- 게스트 계정 생성 및 로컬 계정 전환 흐름 이해

## 2. 기술 스택

- **Backend**: Spring Boot, Spring Security
- **Authentication**: JWT, OAuth 2.0
- **Password**: BCrypt
- **Social Login**: Kakao OAuth
- **Testing**: JUnit 5, Mockito, Spring Boot Test
- **Database**: MySQL
- **Cache / Token Storage**: Redis

## 3. 주요 기능

### 3.1 로컬 로그인
- 이메일 및 비밀번호 기반 회원 인증
- BCrypt를 이용한 비밀번호 암호화 및 검증
- 로그인 성공 시 Access Token 및 Refresh Token 발급
- JWT 기반 인증 및 사용자 정보 확인
- 로그인 실패 시 HTTP 401 응답 처리

### 3.2 소셜 로그인 (OAuth 2.0)
- 카카오 OAuth 로그인 구현
- OAuth 인증 결과를 이용한 사용자 정보 조회
- 이메일을 기준으로 기존 사용자 조회 및 신규 사용자 생성
- 소셜 로그인 성공 후 JWT 발급 및 클라이언트 전달
- 구글 및 네이버 로그인은 미구현

### 3.3 게스트 계정
- Device ID 기반 게스트 계정 로그인
- 기존 게스트 계정 조회 또는 신규 계정 생성
- 게스트 계정에서 로컬 계정으로 전환
- 기존 게스트 계정의 사용자 ID를 유지하는 계정 전환 구조
- 게스트 계정에서 소셜 계정으로 전환하는 기능은 미구현

### 3.4 로그아웃
- 로그아웃 요청 시 Redis에 저장된 Refresh Token 삭제
- 이후 Refresh Token을 이용한 재발급 방지

## 4. API 명세

Base URL: `/api/auth`

| Method | Endpoint | 설명 |
|---|---|---|
| POST | `/guest` | 게스트 로그인 |
| POST | `/signup` | 회원가입 및 게스트 계정 전환 |
| POST | `/login` | 로컬 로그인 |
| POST | `/logout` | 로그아웃 |
| GET | `/login/success` | 소셜 로그인 성공 후 토큰 전달 |
| POST | `/refresh` | 토큰 재발급 |

## 5. JWT 토큰 관리

모바일 클라이언트에서 로그인 상태를 유지하고 인증된 API 요청을 처리할 수 있도록 Access Token과 Refresh Token을 발급한다.

- **Access Token**: API 요청 시 사용자 인증
- **Refresh Token**: Access Token 만료 시 토큰 재발급
- **Redis**: 사용자 ID를 기준으로 Refresh Token 저장 및 검증

발급받은 토큰은 모바일 클라이언트의 안전한 저장소에 보관하는 것을 전제로 하며, 인증이 필요한 요청에는 Access Token을 전달한다.

```http
Authorization: Bearer {accessToken}
```

Refresh Token을 이용한 재발급 요청 시 JWT 유효성을 검증하고, Redis에 저장된 토큰과 요청 토큰의 일치 여부를 확인한다. 검증에 성공하면 새로운 Access Token과 Refresh Token을 발급하고 Redis에 갱신된 Refresh Token을 저장한다.

## 6. 인증 기능 구현 현황

| 기능 | 구현 상태 |
|---|---|
| 이메일/비밀번호 로그인 | 완료 |
| JWT 발급 및 인증 | 완료 |
| 비밀번호 암호화 및 검증 | 완료 |
| 로그인 실패 처리 (401) | 완료 |
| Refresh Token 관리 (Redis) | 완료 |
| 카카오 로그인 | 완료 |
| 구글 로그인 | 미구현 |
| 네이버 로그인 | 미구현 |
| 게스트 계정 로그인 | 완료 |
| 게스트 → 로컬 계정 전환 | 완료 |
| 게스트 → 소셜 계정 전환 | 미구현 |

## 7. 인증 흐름

- **로컬 로그인**: 이메일 및 비밀번호 검증 → JWT 발급 → Redis에 Refresh Token 저장 → 인증된 요청 처리
- **카카오 로그인**: 카카오 OAuth 인증 → 사용자 정보 조회 → 이메일 기준 기존 사용자 조회 또는 신규 사용자 생성 → JWT 발급 및 클라이언트 전달
- **게스트 로그인**: Device ID 기반 게스트 계정 조회 또는 생성 → JWT 발급 → Redis에 Refresh Token 저장
- **계정 전환**: 기존 게스트 계정 조회 → 사용자 ID 유지 → 로컬 회원 정보 및 권한으로 변경

## 8. 테스트

### 테스트 환경
- JUnit 5
- Mockito
- Spring Boot Test

### 테스트 결과

| ID | 테스트 시나리오 | 결과 |
|---|---|---|
| AUTH-01 | 새로운 Device ID로 게스트 계정 생성 | PASS |
| AUTH-02 | 동일한 Device ID로 기존 게스트 계정 재사용 | PASS |
| AUTH-03 | 게스트 계정을 로컬 계정으로 전환 및 기존 ID 유지 | PASS |
| AUTH-04 | 유효한 Refresh Token으로 재발급 | PASS |
| AUTH-05 | Redis에 저장된 Refresh Token과 요청 토큰 불일치 시 재발급 실패 | PASS |

- AuthService 단위 테스트: 5개
- 성공: 5개
- 실패: 0개
- GitHub Actions CI: 통과

## 9. 향후 개선 사항

- 구글 OAuth 로그인 추가
- 네이버 OAuth 로그인 추가
- 게스트 계정의 소셜 계정 연동 및 전환 정책 수립
- 기존 게스트 데이터와 소셜 계정 데이터의 병합 및 중복 처리 방안 검토

## 10. 작업 결과

모바일 애플리케이션을 위한 인증 기능으로 이메일 및 비밀번호 기반 로컬 로그인, JWT 기반 인증, 카카오 소셜 로그인 및 게스트 계정에서 로컬 계정으로 전환하는 기능을 구현하였다.

이를 통해 Spring Security 기반의 인증 및 인가 구조와 JWT를 활용한 토큰 발급 및 검증 과정, OAuth 2.0 기반 소셜 로그인, Redis를 활용한 Refresh Token 관리 방법을 학습하였다.

구글 및 네이버 소셜 로그인과 게스트 계정의 소셜 계정 전환 기능은 구현 범위에서 제외하였으며, 향후 개선 사항으로 정리하였다.
