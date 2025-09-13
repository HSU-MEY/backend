# ChatController 사용자 시나리오 분석

## 🎯 API 구조
- **엔드포인트**: `POST /api/chat/query`
- **요청**: `ChatRequest` (query + context + language)  
- **응답**: `CommonResponse<ChatResponse>`
- **다국어 지원**: 한국어(ko), 영어(en), 일본어(ja), 중국어(zh)

## 📋 사용자 시나리오별 질답 흐름

### 🌍 다국어 지원 예시

#### 영어 사용자 예시
```json
// 요청
{
  "query": "recommend me a kpop route",
  "context": null,
  "language": "en"
}

// 응답 ❓
{
  "responseType": "QUESTION",
  "message": "Which region would you like to visit? (e.g., Seoul, Busan)",
  "context": {
    "theme": "K_POP",
    "conversationState": "AWAITING_REGION",
    "lastBotQuestion": "Which region would you like to visit? (e.g., Seoul, Busan)",
    "sessionId": "...",
    "userLanguage": "en"
  }
}
```

#### 일본어 사용자 예시
```json
// 요청
{
  "query": "Kポップルートを推薦してください",
  "context": null,
  "language": "ja"
}

// 응답 ❓
{
  "responseType": "QUESTION", 
  "message": "どちらの地域をご希望ですか？（例：ソウル、釜山）",
  "context": {
    "theme": "K_POP",
    "conversationState": "AWAITING_REGION",
    "lastBotQuestion": "どちらの地域をご希望ですか？（例：ソウル、釜山）",
    "sessionId": "...",
    "userLanguage": "ja"
  }
}
```

### 시나리오 1: 새 루트 생성 (CREATE_ROUTE) 🆕

#### 1-1. 완전한 정보 제공 시
```json
// 요청 1
POST /api/chat/query
{
  "query": "2일 서울 K-POP 루트 추천해줘",
  "context": null,
  "language": "ko"
}

// 응답 1 ✅
{
  "responseType": "ROUTE_RECOMMENDATION",
  "message": "서울의 대표적인 K-POP 성지들을 둘러보는 환상적인 2일 여행입니다! JYP, SM, HYBE 사옥 등...",
  "routeRecommendation": {
    "routeId": 123,
    "endpoint": "/api/routes/123",
    "title": "서울 K-POP 2일 루트",
    "description": "...",
    "estimatedCost": 45000,
    "durationMinutes": 480
  }
}
```

#### 1-2. 단계적 정보 수집
```json
// 요청 1 
{
  "query": "루트 추천해줘",
  "context": null,
  "language": "ko"
}

// 응답 1 ❓
{
  "responseType": "QUESTION",
  "message": "어떤 테마의 루트를 찾고 계신가요? (K-POP, K-드라마, K-푸드, K-패션 중 선택해주세요)",
  "context": {
    "theme": null,
    "region": null,
    "budget": null,
    "preferences": null,
    "durationMinutes": null,
    "days": null,
    "conversationState": "AWAITING_THEME",
    "sessionId": "...",
    "userLanguage": "ko"
  }
}

// 요청 2 (⚠️ 프론트엔드는 이전 응답의 context를 그대로 포함해서 전송)
{
  "query": "K-POP이요",
  "context": {
    "theme": null,
    "region": null,
    "budget": null,
    "preferences": null,
    "durationMinutes": null,
    "days": null,
    "conversationState": "AWAITING_THEME",
    "sessionId": "...",
    "userLanguage": "ko"
  },
  "language": "ko"
}

// 응답 2 ❓
{
  "responseType": "QUESTION", 
  "message": "어느 지역의 루트를 원하시나요? (예: 서울, 부산)",
  "context": {
    "theme": "KPOP",
    "region": null,
    "budget": null,
    "preferences": null,
    "durationMinutes": null,
    "days": null,
    "conversationState": "AWAITING_REGION",
    "sessionId": "...",
    "userLanguage": "ko"
  }
}

// 요청 3 (⚠️ 프론트엔드는 이전 응답의 context를 그대로 포함해서 전송)
{
  "query": "서울로",
  "context": {
    "theme": "KPOP",
    "region": null,
    "budget": null,
    "preferences": null,
    "durationMinutes": null,
    "days": null,
    "conversationState": "AWAITING_REGION",
    "sessionId": "...",
    "userLanguage": "ko"
  },
  "language": "ko"
}

// 응답 3 ❓
{
  "responseType": "QUESTION",
  "message": "몇 일 여행을 계획하고 계신가요? (예: 1일, 2일, 3일)",
  "context": {
    "theme": "KPOP",
    "region": "서울",
    "budget": null,
    "preferences": null,
    "durationMinutes": null,
    "days": null,
    "conversationState": "AWAITING_DAYS",
    "sessionId": "...",
    "userLanguage": "ko"
  }
}

// 요청 4 (⚠️ 프론트엔드는 이전 응답의 context를 그대로 포함해서 전송)
{
  "query": "2일",
  "context": {
    "theme": "KPOP",
    "region": "서울",
    "budget": null,
    "preferences": null,
    "durationMinutes": null,
    "days": null,
    "conversationState": "AWAITING_DAYS",
    "sessionId": "...",
    "userLanguage": "ko"
  },
  "language": "ko"
}

// 응답 4 ✅
{
  "responseType": "ROUTE_RECOMMENDATION",
  "message": "완벽한 2일 KPOP 테마 루트를 만들어드렸습니다!",
  "routeRecommendation": {...},
  "context": {
    "theme": "KPOP",
    "region": "서울",
    "budget": null,
    "preferences": null,
    "durationMinutes": null,
    "days": 2,
    "conversationState": "READY_FOR_ROUTE",
    "sessionId": "...",
    "userLanguage": "ko"
  }
}
```

#### 1-3. 일수 조정 필요 시
```json
// 요청
{
  "query": "4일 서울 K-POP 루트 만들어줘",
  "context": null,
  "language": "ko"
}

// 응답 ⚠️
{
  "responseType": "ROUTE_RECOMMENDATION",
  "message": "요청하신 4일 여행에는 16개의 장소가 필요하지만, 서울 지역의 KPOP 테마 장소는 총 12개만 있어서 3일 여행으로 조정했습니다.\n\n완벽한 3일 KPOP 테마 루트를 만들어드렸습니다!",
  "routeRecommendation": {...}
}
```

---

### 시나리오 2: 기존 루트 검색 (SEARCH_EXISTING_ROUTES) 🔍

```json
// 요청
{
  "query": "기존에 만들어진 부산 드라마 루트 있어?",
  "context": null,
  "language": "ko"
}

// 응답 ✅
{
  "responseType": "EXISTING_ROUTES",
  "message": "부산의 아름다운 드라마 촬영지들을 둘러보는 특별한 루트들을 찾았어요! 바다와 어우러진 로맨틱한...",
  "existingRoutes": [
    {
      "routeId": 456,
      "title": "부산 k-drama 루트 1", 
      "description": "아홉산 숲을(를) 포함한 부산 지역의 k-drama 테마 루트입니다.",
      "estimatedCost": 35000,
      "durationMinutes": 300,
      "themes": ["k-drama"]
    }
  ]
}
```

---

### 시나리오 3: 장소 검색 (SEARCH_PLACES) 📍

```json
// 요청
{
  "query": "홍대 근처 K-POP 장소 어디 있어?",
  "context": null,
  "language": "ko"
}

// 응답 ✅
{
  "responseType": "PLACE_INFO",
  "message": "검색하신 조건에 맞는 장소들을 찾았습니다:",
  "places": [
    {
      "placeId": 789,
      "name": "케이팝 스퀘어 홍대점",
      "description": "홍대에 위치한 케이팝 전문 쇼핑몰로...",
      "address": "서울특별시 마포구 홍익로",
      "themes": ["k-pop", "shopping", "hongdae"],
      "costInfo": "무료 입장"
    }
  ]
}
```

---

### 시나리오 4: 일반 질문 (GENERAL_QUESTION) 💬

```json
// 요청 
{
  "query": "BTS가 뭐야?",
  "context": null,
  "language": "ko"
}

// 응답 ✅
{
  "responseType": "GENERAL_INFO", 
  "message": "BTS는 대한민국의 7인조 보이 그룹으로, 2013년 빅히트 엔터테인먼트에서 데뷔했습니다. 전 세계적으로 큰 인기를 얻으며..."
}
```

---

## ⚠️ 에러 케이스 및 예외 상황

### 5-1. 서버/AI 오류
```json
// 요청
{
  "query": "루트 추천해줘",
  "context": {"theme": "KPOP", "region": "서울", "days": 2},
  "language": "ko"
}

// 응답 ❌
{
  "responseType": "QUESTION",
  "message": "루트 생성 중 오류가 발생했습니다. 다시 시도해주시겠어요?"
}
```

### 5-2. 검색 결과 없음
```json
// 요청
{
  "query": "제주도 K-POP 장소 찾아줘",
  "context": null,
  "language": "ko"
}

// 응답 ❌
{
  "responseType": "QUESTION",
  "message": "요청하신 조건에 맞는 장소를 찾을 수 없습니다. 다른 키워드로 검색해보시겠어요?"
}
```

### 5-3. JSON 파싱 오류
```json
// 요청 (잘못된 형식)
{
  "query": "루트 추천",
  "context": {
    "theme": "INVALID_THEME"  // 잘못된 Theme enum 값
  },
  "language": "ko"
}

// 응답 ❌ (400 Bad Request)
{
  "type": "about:blank",
  "title": "Bad Request", 
  "status": 400,
  "detail": "Failed to read request"
}
```

---

## 🔄 프론트엔드 Context 관리 가이드

### 📱 **클라이언트 구현 방법**

프론트엔드에서는 다음과 같이 context를 관리해야 합니다:

```typescript
interface ChatContext {
  theme?: "KDRAMA" | "KPOP" | "KFOOD" | "KFASHION" | null;
  region?: string | null;
  budget?: number | null;
  preferences?: string | null;
  durationMinutes?: number | null;
  days?: number | null;
  
  // 🆕 대화 상태 관리 필드들
  conversationState?: "INITIAL" | "AWAITING_THEME" | "AWAITING_REGION" | "AWAITING_DAYS" | "READY_FOR_ROUTE" | null;
  lastBotQuestion?: string | null;
  sessionId?: string | null;  // 🔒 사용자별 세션 보장
  conversationStartTime?: number | null;
  userLanguage?: "ko" | "en" | "ja" | "zh" | null;  // 🌍 사용자 언어
}

interface ChatRequest {
  query: string;
  context: ChatContext | null;
  language: "ko" | "en" | "ja" | "zh";  // 🌍 다국어 지원
}

interface ChatResponse {
  responseType: "QUESTION" | "ROUTE_RECOMMENDATION" | "EXISTING_ROUTES" | "PLACE_INFO" | "GENERAL_INFO";
  message: string;
  context: ChatContext;  // ⚠️ 모든 응답에 업데이트된 context 포함 (세션 정보 포함)
  // ... 기타 필드들
}
```

### 🔄 **Context 상태 관리 플로우 (다국어 지원 포함)**

```javascript
class ChatManager {
  constructor(userLanguage = 'ko') {
    this.currentContext = null; // 현재 대화 컨텍스트
    this.userLanguage = userLanguage; // 🌍 사용자 언어 설정
  }

  generateSessionId() {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
  }

  setLanguage(language) {
    this.userLanguage = language;
    // 언어 변경 시 새 세션 시작
    this.resetContext();
  }

  async sendMessage(query) {
    // 🆕 첫 번째 메시지에서 세션 ID 생성 (없는 경우에만)
    if (!this.currentContext?.sessionId) {
      this.currentContext = {
        ...this.currentContext,
        sessionId: this.generateSessionId(),
        conversationStartTime: Date.now(),
        userLanguage: this.userLanguage  // 🌍 언어 정보 포함
      };
    }

    const request = {
      query: query,
      context: this.currentContext,  // ⚠️ sessionId를 포함한 전체 context 전송
      language: this.userLanguage    // 🌍 언어 명시적 전송
    };

    const response = await fetch('/api/chat/query', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request)
    }).then(res => res.json());

    // ⚠️ 중요: 응답받은 context로 현재 context 업데이트
    // 백엔드에서 sessionId와 상태 정보가 자동으로 관리됨
    this.currentContext = response.result.context;
    
    return response;
  }

  resetContext() {
    this.currentContext = null; // 새로운 대화 시작 시 초기화 (새 세션 ID 생성됨)
  }

  getCurrentSessionId() {
    return this.currentContext?.sessionId;
  }

  getConversationState() {
    return this.currentContext?.conversationState;
  }

  getUserLanguage() {
    return this.userLanguage;
  }
}
```

### 📝 **다국어 상태 기반 대화 사용 예시**

```javascript
// 영어 사용자
const chatManager = new ChatManager('en');

// 1. 첫 번째 요청 (세션 시작)
let response1 = await chatManager.sendMessage("recommend me a kpop route");
console.log(response1.result.context);
// 출력: {
//   sessionId: "session_1693920000_abc123",
//   conversationState: "AWAITING_REGION",  // 테마는 이미 추출됨
//   lastBotQuestion: "Which region would you like to visit? (e.g., Seoul, Busan)",
//   conversationStartTime: 1693920000000,
//   theme: "K_POP", region: null, days: null,
//   userLanguage: "en"
// }

// 2. 두 번째 요청 (지역 제공)
let response2 = await chatManager.sendMessage("Seoul");
// conversationState: "AWAITING_DAYS", theme: "K_POP", region: "Seoul"

// 3. 세 번째 요청 (일수 제공)  
let response3 = await chatManager.sendMessage("2 days");
// responseType: "ROUTE_RECOMMENDATION" - 루트 생성 완료!
```

### ⚠️ **주의사항**

1. **Context 누적**: 각 응답의 `context` 필드를 다음 요청의 `context`로 그대로 사용
2. **세션 보장**: `sessionId`는 백엔드에서 자동 생성/관리되므로 프론트엔드는 그대로 전달만
3. **다국어 지원**: `language` 필드를 항상 명시하고, context의 `userLanguage`와 일치시킴
4. **언어 변경**: 언어 변경 시 새 세션 시작으로 이전 대화 컨텍스트 초기화
5. **상태 초기화**: 새로운 대화 시작 시 `resetContext()` 호출하여 새 세션 생성
6. **에러 처리**: API 에러 시에도 context 상태를 적절히 관리 (세션 ID 보존)
7. **타입 안정성**: TypeScript 사용 시 확장된 Context 타입 정의 준수
8. **🔥 핵심**: 언어별 맞춤형 메시지 제공 및 fallback 전략 (ja/zh → en, 기타 → ko)

---

## 🎛️ 의도 분류 키워드 (다국어)

### 한국어 (ko)
| **Intent** | **키워드** | **예시** |
|------------|------------|----------|
| **CREATE_ROUTE** | "추천해줘", "계획해줘", "루트 만들", "여행 계획" | "2일 서울 여행 계획해줘" |
| **SEARCH_EXISTING_ROUTES** | "기존 루트", "만들어진 루트", "루트 찾아", "루트 검색" | "이미 있는 부산 루트 보여줘" |
| **SEARCH_PLACES** | "장소", "명소", "어디", "위치", "곳" | "명동 근처 뷰티샵 어디 있어?" |
| **GENERAL_QUESTION** | 기타 모든 질문 | "한류가 뭐야?", "K-POP 역사 알려줘" |

### 영어 (en)
| **Intent** | **키워드** | **예시** |
|------------|------------|----------|
| **CREATE_ROUTE** | "recommend", "suggest", "plan", "create route", "make itinerary" | "Recommend a 2-day Seoul K-POP route" |
| **SEARCH_EXISTING_ROUTES** | "existing routes", "available routes", "find route", "search route" | "Are there existing Busan drama routes?" |
| **SEARCH_PLACES** | "place", "location", "where", "spot", "find", "near", "around" | "Where are K-POP places near Hongdae?" |
| **GENERAL_QUESTION** | 기타 모든 질문 | "What is BTS?", "Tell me about K-POP history" |

### 일본어 (ja)
| **Intent** | **키워드** | **예시** |
|------------|------------|----------|
| **CREATE_ROUTE** | "推薦", "計画", "ルート作成", "旅行プラン" | "2日間のソウルK-POPルートを推薦してください" |
| **SEARCH_EXISTING_ROUTES** | "既存ルート", "作成済みルート", "ルート検索" | "釜山のドラマルートはありますか？" |
| **SEARCH_PLACES** | "場所", "スポット", "どこ", "位置", "近く" | "弘大近くのK-POP場所はどこですか？" |
| **GENERAL_QUESTION** | 기타 모든 질문 | "BTSとは何ですか？", "K-POP歴史を教えて" |

### 중국어 (zh)
| **Intent** | **키워드** | **예시** |
|------------|------------|----------|
| **CREATE_ROUTE** | "推荐", "计划", "路线制作", "旅行规划" | "请推荐2天首尔K-POP路线" |
| **SEARCH_EXISTING_ROUTES** | "现有路线", "已制作路线", "路线搜索" | "有釜山戏剧路线吗？" |
| **SEARCH_PLACES** | "地点", "景点", "哪里", "位置", "附近" | "弘大附近的K-POP地点在哪里？" |
| **GENERAL_QUESTION** | 기타 모든 질문 | "BTS是什么？", "告诉我K-POP历史" |

---

## 📊 응답 타입별 데이터 구조

| **ResponseType** | **포함 필드** | **용도** |
|------------------|---------------|----------|
| **QUESTION** | `message` | 추가 정보 요청 (다국어 지원) |
| **ROUTE_RECOMMENDATION** | `message`, `routeRecommendation` | 새 루트 생성 완료 |
| **EXISTING_ROUTES** | `message`, `existingRoutes[]` | 기존 루트 목록 |
| **PLACE_INFO** | `message`, `places[]` | 장소 정보 목록 |
| **GENERAL_INFO** | `message` | 일반 정보/답변 (다국어 지원) |

---

## 🌍 언어별 Fallback 전략

- **지원 언어**: ko (한국어), en (영어), ja (일본어), zh (중국어)
- **완전 지원**: 모든 4개 언어가 데이터베이스에서 완전 지원됨
- **Fallback 규칙**: 
  - 각 언어별 데이터가 누락된 경우에만 다음 순서로 fallback:
    - ja (일본어) → en (영어) → ko (한국어)
    - zh (중국어) → en (영어) → ko (한국어)
    - en (영어) → ko (한국어)
  - 기타 모든 언어 → ko (한국어 기본값)
- **데이터 품질**: 
  - 모든 Place 엔티티에 4개 언어 완전 지원 (nameKo/En/Jp/Ch, descriptionKo/En/Jp/Ch, addressKo/En/Jp/Ch)
  - RAG 시스템에서 언어별 맞춤 검색 및 응답 생성
  - 챗봇 메시지 템플릿 4개 언어 완전 지원
- **언어 코드 변환**: 
  - chatbot 도메인: "ja", "zh" 사용
  - Tour API 호출 시: "J", "C"로 자동 변환
