# weatherScreen 구조 개선 계획.
weather의 로딩은 3단계로 나뉜다.

1. 현재 위치 + 저장된 지역 목록 로딩.
2. 현재 보여지고 있는 위치의 날씨 일괄 로딩. 이때 뷰페이저의 페이지 내부만 새로고침이 일어난다.
3. 날씨 페이지(패널) 내부에서 당겨서 새로고침. 이때는 현재 보고 있는 날씨만 새로고침 한다.

위 구현을 위해, 현재 앱이 바로보고 있는 날씨 정보를 하나 정의해서,
그걸 Flow로 만들어서, 새로고침할때마다 해당 flow를 갱신하고,

각 요소들은 해당 flow를 자기가 바라보는 관점에서 변경되었을때만 변경이 관찰되게 만드는게 어떨까?

# MainActivity가 가진 권한 책임 분리.
권한 처리도 반응형/함수형으로 처리할 수 있는 도구로 적용하는게 가장 깔끔할듯.(툴이름 까먹음)

# WeatherViewModel._weathers
handleRefreshRequest은 Flow<WeathersReducer>를 리턴한다.

_weathers는 두개의 flow의 과정을 scan 한다..

merge, combine과 차이가 뭐지?

## merge — 여러 Flow를 하나의 파이프로 합치기

Flow A:  ──1──────3──────                                                                                                                                                                                         
Flow B:  ────2──────4────                                                                                                                                                                                         
merge:   ──1─2────3─4──── (오는 순서대로 그냥 흘려보냄)

- 타입이 같아야 함
- 변환 없이 그냥 섞는 것
- 아무나 emit하면 바로 발행  

## combine — 여러 Flow의 최신값끼리 조합

Flow A:  ──1──────3──────                                                                                                                                                                                         
Flow B:  ────"a"────"b"──                                                                                                                                                                                       
combine: ────(1,a)─(3,a)─(3,b)──

- 모든 Flow가 최소 1번은 emit해야 첫 발행 (이후엔 아무나 바뀌면 발행)
- 각 Flow의 최신값을 lambda로 조합
- 타입은 달라도 된다.

## scan — 하나의 Flow를 누적

Flow:    ──1──2──3──                                                                                                                                                                                            
scan(0): ──1──3──6──  (acc + value)

- Flow 합치는 게 아님
- 하나의 Flow에서 상태를 누적하는 것   
 
WeatherViewModel._weathers
얘는 여러개의 flow를 merge하고, scan으로 누적.
flow1. location의 갱신.
flow2. 요청에 의한 핸들.
이때 scan의 첫번째 파라미터는 scan되서 오는거고..
두번째 파라미터는 위의 merge에서 넘어오는 reducer에 의해 기존 
weathers에서 어떤 reduce를 할지 결정.
예를 들면, location이 바뀌어서 호출되면,
cleanup을 하고, handleRefreshRequest에서 새로운 weathers를 발행하겠지.


## handleRefreshRequest 함수
WeathersReducer를 발행하는 플로우를 리턴한다.
함수내에서 _weathers로 스냅샷에 접근해서 처리하지만,
막상 emit 블록 안에선, snap샷이 아닌, scan에서 현재 상태인
weathers로 접근이 가능하다. 이게 대체 무슨 개념일까?
