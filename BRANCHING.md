# 브랜치 정책

## 브랜치 구조

| 브랜치 | 용도 |
|--------|------|
| `main` | 프로덕션 (릴리즈) |
| `develop` | 개발 통합 |

## 머지 규칙

### develop → main (일반)

```bash
git checkout main
git merge develop --no-ff -m "Merge develop into main"
git push
```

- **항상 `--no-ff`** (머지 커밋 생성)
- Fast-forward 하지 않음
- 머지 시점이 히스토리에 명확히 남음

### main → develop (예외)

- **핫픽스 반영 시에만** 수행
- 일반적으로 하지 않음

### 머지 전 체크리스트

- [ ] CI 통과 확인
- [ ] 앱 빌드 성공
- [ ] 주요 기능 테스트

## 직접 커밋

| 브랜치 | 직접 커밋 |
|--------|----------|
| `main` | ❌ (긴급 핫픽스만 예외) |
| `develop` | ✅ |

## 히스토리 예시

```
*   Merge develop into main (2026-03-26)
|\
| * feat: App Check 구현
| * chore: keystore 설정
|/
*   Merge develop into main (2026-03-20)
|\
| * feat: 위젯 개선
|/
*   v1.0.0
```
