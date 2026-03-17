#!/usr/bin/env python3 -u
"""
법정동 CSV → JSON 변환 스크립트 (카카오 Geocoding API 사용)
"""

import csv
import json
import time
import sys
import requests
from collections import defaultdict
from pathlib import Path

# 출력 버퍼 비활성화
sys.stdout.reconfigure(line_buffering=True)

KAKAO_API_KEY = "366992c357dd822f4c7ee547e6aaa5d7"
CSV_PATH = Path(__file__).parent.parent / "docs" / "법정동코드_원본.csv"
OUTPUT_PATH = Path(__file__).parent.parent / "core" / "data" / "src" / "main" / "assets" / "korean_regions.json"

def geocode(address: str) -> tuple:
    """주소 → 좌표 변환"""
    try:
        response = requests.get(
            "https://dapi.kakao.com/v2/local/search/address.json",
            params={"query": address},
            headers={"Authorization": f"KakaoAK {KAKAO_API_KEY}"},
            timeout=10
        )
        data = response.json()
        if data.get("documents"):
            doc = data["documents"][0]
            return float(doc["y"]), float(doc["x"])
    except Exception as e:
        print(f"  [ERROR] {address}: {e}")
    return None, None

def main():
    print("=== 법정동 CSV → JSON 변환 시작 ===\n")
    
    # 1. CSV 읽기
    print("1. CSV 읽는 중...")
    rows = []
    with open(CSV_PATH, "r", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        for row in reader:
            # 삭제된 항목 제외
            if row.get("삭제일자"):
                continue
            rows.append(row)
    print(f"   → {len(rows)}개 행 로드\n")
    
    # 2. 계층 구조 생성
    print("2. 계층 구조 생성 중...")
    hierarchy = defaultdict(lambda: defaultdict(lambda: defaultdict(dict)))
    
    for row in rows:
        sido = row["시도명"]
        sigungu = row.get("시군구명", "")
        eupmyeondong = row.get("읍면동명", "")
        ri = row.get("리명", "")
        
        if not sido:
            continue
            
        if not sigungu:
            # 시도 레벨
            if sido not in hierarchy:
                hierarchy[sido] = defaultdict(lambda: defaultdict(dict))
        elif not eupmyeondong:
            # 시군구 레벨
            if sigungu not in hierarchy[sido]:
                hierarchy[sido][sigungu] = defaultdict(dict)
        elif not ri:
            # 읍면동 레벨 (최하위)
            hierarchy[sido][sigungu][eupmyeondong] = {"is_leaf": True}
        else:
            # 리 레벨 (시골)
            hierarchy[sido][sigungu][eupmyeondong][ri] = {"is_leaf": True}
    
    print(f"   → 시도: {len(hierarchy)}개\n")
    
    # 3. 좌표 변환 (최하위 노드만)
    print("3. 좌표 변환 중 (카카오 API)...")
    total_geocode = 0
    success_geocode = 0
    
    for sido, sigungu_dict in hierarchy.items():
        for sigungu, emd_dict in sigungu_dict.items():
            for eupmyeondong, ri_or_leaf in emd_dict.items():
                if isinstance(ri_or_leaf, dict) and ri_or_leaf.get("is_leaf"):
                    # 읍면동이 최하위
                    address = f"{sido} {sigungu} {eupmyeondong}".strip()
                    total_geocode += 1
                    lat, lng = geocode(address)
                    if lat and lng:
                        hierarchy[sido][sigungu][eupmyeondong] = {"lat": lat, "lng": lng}
                        success_geocode += 1
                    else:
                        hierarchy[sido][sigungu][eupmyeondong] = {"lat": None, "lng": None}
                    
                    if total_geocode % 100 == 0:
                        print(f"   → {total_geocode}개 처리됨 ({success_geocode} 성공)")
                    time.sleep(0.05)  # Rate limit
                else:
                    # 리가 있는 경우
                    for ri, leaf in ri_or_leaf.items():
                        if ri == "is_leaf":
                            continue
                        address = f"{sido} {sigungu} {eupmyeondong} {ri}".strip()
                        total_geocode += 1
                        lat, lng = geocode(address)
                        if lat and lng:
                            hierarchy[sido][sigungu][eupmyeondong][ri] = {"lat": lat, "lng": lng}
                            success_geocode += 1
                        else:
                            hierarchy[sido][sigungu][eupmyeondong][ri] = {"lat": None, "lng": None}
                        
                        if total_geocode % 100 == 0:
                            print(f"   → {total_geocode}개 처리됨 ({success_geocode} 성공)")
                        time.sleep(0.05)
    
    print(f"\n   → 총 {total_geocode}개 중 {success_geocode}개 좌표 변환 성공\n")
    
    # 4. JSON 구조로 변환
    print("4. JSON 구조 생성 중...")
    
    def build_json(data, depth=0):
        result = []
        for name, children in data.items():
            if name == "is_leaf":
                continue
            if isinstance(children, dict):
                if "lat" in children:
                    # 최하위 노드
                    result.append({
                        "name": name,
                        "lat": children["lat"],
                        "lng": children["lng"]
                    })
                else:
                    # 중간 노드
                    result.append({
                        "name": name,
                        "children": build_json(children, depth + 1)
                    })
        return sorted(result, key=lambda x: x["name"])
    
    output = {"regions": build_json(hierarchy)}
    
    # 5. 저장
    print("5. JSON 저장 중...")
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
        json.dump(output, f, ensure_ascii=False, indent=2)
    
    print(f"   → {OUTPUT_PATH}\n")
    print("=== 완료! ===")

if __name__ == "__main__":
    main()
