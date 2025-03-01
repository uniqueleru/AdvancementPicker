# AdvancementPicker 개발 가이드

## 빌드 명령어
- 빌드: `mvn clean package`
- JAR 생성 위치: `target/AdvancementPicker-1.0.0.jar`

## 코드 스타일 가이드라인
- **네이밍 컨벤션**: 
  - 클래스: PascalCase
  - 메서드/변수: camelCase
  - 상수: SNAKE_CASE
- **패키지 구조**: `kr.ieruminecraft.advancementpicker.[기능영역]`
- **메시지 처리**: Adventure API의 Component 사용 (String 직접 사용 금지)
- **오류 처리**: 명확한 오류 메시지와 적절한 로깅
- **종속성**: Paper API 1.21.4 사용

## 중요 클래스
- `AdvancementPicker`: 메인 플러그인 클래스
- `ConfigManager`: 설정 및 언어 파일 관리
- `AdvancementManager`: 도전과제 할당/관리 로직

## 플러그인 구조
- `config.yml`: 도전과제 목록
- `lang.yml`: 모든 사용자 메시지
- 명령어: `/ap <pick|giveup|reload|help>`