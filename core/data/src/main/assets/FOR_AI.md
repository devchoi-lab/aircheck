여러 깊이로 된 데이터 구조가 지저분함.
관계형 테이블을 카피한 구조가 가장 깔끔할것임.

고로 기본적으로는 아래의 구조가 된다.
일단 시험 삼아서, 아래의 구조의 파일을 korean_regions_2.json으로 하나 만들어볼까?

``` json
{
    "regions": [
        {
          "name": "강동면",
          "lat": 37.7280943167627,
          "lng": 128.954226402687,
          "id": 1,
          "isLeaf" : true,
          "parentId" : 10,
          "parentName" : "강릉시" //parentName은 parentId매기는 로직 실패시 디버깅 하기 위한 목적.
        }
        ...
        {
          "name": "강릉시",
          "lat": 기존 korean_regions.json를 참고하여 하위 요소의 중앙 값,
          "lng": 기존 korean_regions.json를 참고하여 하위 요소의 중앙 값,
          "id": 10,
          "isLeaf" : false,
          "parentId" : 20,
          "parentName" : "강원특별자치도"
        }
        ...
        {
          "name": "강원특별자치도",
          "lat": 기존 korean_regions.json를 참고하여 하위 요소의 중앙 값,
          "lng": 기존 korean_regions.json를 참고하여 하위 요소의 중앙 값,
          "id": 20,
          "isLeaf" : false,
          "parentId" : null,
          "parentName" : null
        }
    ]
}
```

## 원본 데이터 누락 도시 (korean_regions.json)

아래 12개 도시는 children도 없고 lat/lng도 없어서 선택 불가 상태.
구 단위(덕양구, 일산동구 등) 하위 데이터 보강 필요.

- 고양시 (경기도)
- 부천시 (경기도)
- 성남시 (경기도)
- 수원시 (경기도)
- 안산시 (경기도)
- 안양시 (경기도)
- 용인시 (경기도)
- 창원시 (경상남도)
- 포항시 (경상북도)
- 전주시 (전라북도)
- 천안시 (충청남도)
- 청주시 (충청북도)

# FOR_AI
아래의 파일에서, 각 시이름으로 시작하는 지역들의 lat, lng를 가져와서 중앙 위치로 입력하면 어떨까?
[korean_regions_2.json](korean_regions_2.json)

