# ChatController 사용자 시나리오 분석

## 🎯 API 구조
- **엔드포인트**: `POST /api/chat/query`
- **요청**: `ChatRequest` (query + context)  
- **응답**: `CommonResponse<ChatResponse>`

## 📋 사용자 시나리오별 질답 흐름

### 시나리오 1: 새 루트 생성 (CREATE_ROUTE) 🆕

#### 1-1. 완전한 정보 제공 시
```json
// 요청 1
POST /api/chat/query
{
  "query": "2일 서울 K-POP 루트 추천해줘",
  "context": null
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
  "context": null
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
    "days": null
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
    "days": null
  }
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
    "days": null
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
    "days": null
  }
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
    "days": null
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
    "days": null
  }
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
    "days": 2
  }
}
```

#### 1-3. 일수 조정 필요 시
```json
// 요청
{
  "query": "4일 서울 K-POP 루트 만들어줘",
  "context": null
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
  "context": null
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
  "context": null
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
  "context": null
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
  "context": {"theme": "KPOP", "region": "서울", "days": 2}
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
  "context": null
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
  }
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
}

interface ChatRequest {
  query: string;
  context: ChatContext | null;
}

interface ChatResponse {
  responseType: "QUESTION" | "ROUTE_RECOMMENDATION" | "EXISTING_ROUTES" | "PLACE_INFO" | "GENERAL_INFO";
  message: string;
  context: ChatContext;  // ⚠️ 모든 응답에 업데이트된 context 포함 (세션 정보 포함)
  // ... 기타 필드들
}
```

### 🔄 **Context 상태 관리 플로우 (개선된 세션 보장)**

```javascript
class ChatManager {
  constructor() {
    this.currentContext = null; // 현재 대화 컨텍스트
  }

  generateSessionId() {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
  }

  async sendMessage(query) {
    // 🆕 첫 번째 메시지에서 세션 ID 생성 (없는 경우에만)
    if (!this.currentContext?.sessionId) {
      this.currentContext = {
        ...this.currentContext,
        sessionId: this.generateSessionId(),
        conversationStartTime: Date.now()
      };
    }

    const request = {
      query: query,
      context: this.currentContext  // ⚠️ sessionId를 포함한 전체 context 전송
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
}
```

### 📝 **상태 기반 대화 사용 예시**

```javascript
const chatManager = new ChatManager();

// 1. 첫 번째 요청 (세션 시작)
let response1 = await chatManager.sendMessage("루트 추천해줘");
console.log(response1.result.context);
// 출력: {
//   sessionId: "session_1693920000_abc123",
//   conversationState: "AWAITING_THEME",
//   lastBotQuestion: "어떤 테마의 루트를 찾고 계신가요?",
//   conversationStartTime: 1693920000000,
//   theme: null, region: null, days: null, ...
// }

// 2. 두 번째 요청 (테마 제공 - 의도분류 건너뛰고 직접 처리!)
let response2 = await chatManager.sendMessage("K-POP이요");
console.log(response2.result.context);
// 출력: {
//   sessionId: "session_1693920000_abc123",  // 동일 세션 유지
//   conversationState: "AWAITING_REGION",    // 다음 상태로 전환
//   lastBotQuestion: "어느 지역의 루트를 원하시나요?",
//   theme: "KPOP",                          // 테마 정보 추가
//   region: null, days: null, ...
// }

// 3. 세 번째 요청 (지역 제공)
let response3 = await chatManager.sendMessage("서울로");
// conversationState: "AWAITING_DAYS", theme: "KPOP", region: "서울"

// 4. 네 번째 요청 (일수 제공)
let response4 = await chatManager.sendMessage("2일");
// responseType: "ROUTE_RECOMMENDATION" - 루트 생성 완료!

// 🔍 세션 상태 확인
console.log(`현재 세션 ID: ${chatManager.getCurrentSessionId()}`);
console.log(`대화 상태: ${chatManager.getConversationState()}`);
```

### ⚠️ **주의사항**

1. **Context 누적**: 각 응답의 `context` 필드를 다음 요청의 `context`로 그대로 사용
2. **세션 보장**: `sessionId`는 백엔드에서 자동 생성/관리되므로 프론트엔드는 그대로 전달만
3. **상태 초기화**: 새로운 대화 시작 시 `resetContext()` 호출하여 새 세션 생성
4. **에러 처리**: API 에러 시에도 context 상태를 적절히 관리 (세션 ID 보존)
5. **타입 안정성**: TypeScript 사용 시 확장된 Context 타입 정의 준수
6. **🔥 핵심 개선**: 이제 "k-pop" 같은 답변이 의도 분류 오류 없이 정확히 처리됨

---

## 🎛️ 의도 분류 키워드

| **Intent** | **키워드** | **예시** |
|------------|------------|----------|
| **CREATE_ROUTE** | "추천해줘", "계획해줘", "루트 만들", "여행 계획" | "2일 서울 여행 계획해줘" |
| **SEARCH_EXISTING_ROUTES** | "기존 루트", "만들어진 루트", "루트 찾아", "루트 검색" | "이미 있는 부산 루트 보여줘" |
| **SEARCH_PLACES** | "장소", "명소", "어디", "위치", "곳" | "명동 근처 뷰티샵 어디 있어?" |
| **GENERAL_QUESTION** | 기타 모든 질문 | "한류가 뭐야?", "K-POP 역사 알려줘" |

---

## 📊 응답 타입별 데이터 구조

| **ResponseType** | **포함 필드** | **용도** |
|------------------|---------------|----------|
| **QUESTION** | `message` | 추가 정보 요청 |
| **ROUTE_RECOMMENDATION** | `message`, `routeRecommendation` | 새 루트 생성 완료 |
| **EXISTING_ROUTES** | `message`, `existingRoutes[]` | 기존 루트 목록 |
| **PLACE_INFO** | `message`, `places[]` | 장소 정보 목록 |
| **GENERAL_INFO** | `message` | 일반 정보/답변 |

## 🔄 주요 처리 흐름

1. **ChatController.sendMessage()** 
   - 요청 로깅
   - ChatService.processUserQuery() 호출
   - 예외 처리 (LLMException 발생)

2. **ChatService.processUserQuery()**
   - 의도 분류 (classifyUserIntent)
   - 의도별 핸들러 분기
     - CREATE_ROUTE → handleCreateRouteIntent()
     - SEARCH_EXISTING_ROUTES → handleSearchExistingRoutesIntent()  
     - SEARCH_PLACES → handleSearchPlacesIntent()
     - GENERAL_QUESTION → handleGeneralQuestionIntent()

3. **루트 생성 플로우**
   - 컨텍스트 추출 (extractContextFromQuery)
   - 필수 정보 확인 (checkMissingRequiredInfo)
   - 일수 조정 (adjustDaysIfNeeded)
   - 장소 검색 (searchAndExtractPlaceIds)
   - RouteService 호출 (createRouteByAI)
   - 자연스러운 메시지 생성 (generateRouteRecommendationAnswer)
