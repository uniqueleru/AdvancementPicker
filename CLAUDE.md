# AdvancementPicker 개발 가이드

## 빌드 및 개발 명령어
- 빌드: `mvn clean package`
- 특정 테스트 실행: `mvn test -Dtest=TestClassName`
- 코드 검사: `mvn verify`
- 서버 테스트: Paper 서버 plugins 폴더에 target/AdvancementPicker-1.0.0.jar 복사

## 코드 스타일 가이드라인
- **네이밍 컨벤션**: 
  - 클래스: PascalCase (AbstractGUI, MainGUI)
  - 메서드/변수: camelCase (getConfigManager, advancementManager)
  - 상수: SNAKE_CASE (MAX_ADVANCEMENTS)
- **패키지 구조**: `kr.ieruminecraft.advancementpicker.[기능영역]`
- **임포트**: 모든 임포트 명시적으로 작성, 와일드카드(*) 사용 금지
- **메시지 처리**: Adventure API의 Component 사용 (String 직접 사용 금지)
- **오류 처리**: try-catch로 명확한 예외 처리, getLogger()로 로깅
- **GUI 패턴**: AbstractGUI 상속, 일관된 이벤트 처리 방식 유지
- **종속성**: Paper API 1.21.4 사용, Java 21

## 중요 클래스 및 구조
- `AdvancementPicker`: 메인 플러그인 클래스, 싱글톤 패턴 사용
- `ConfigManager`: 설정(config.yml) 및 언어(lang.yml) 파일 관리
- `AdvancementManager`: 도전과제 관리 및 플레이어 상호작용
- `GUIManager`: GUI 관련 클래스 관리
- 명령어: `/ap <pick|giveup|reload|help>`