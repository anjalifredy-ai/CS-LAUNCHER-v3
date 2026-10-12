# Remom → Levi-style Bedrock multi-version (60-day plan)

**Rule:** Har chat / session mein **ek step**. Claim tabhi “playable multi-version” jab native host ready ho.

## Already done (before Day 1 list)
- Java launcher base (Pojav/CS)
- Java/Bedrock header switch
- Official package gate
- Bedrock profiles + version list UI
- Isolation folder skeleton

## Phase A — Days 1–10 (data + import)
| Day | Goal |
|-----|------|
| **1** | Official APK **file picker** + register meta on selected version (THIS COMMIT) |
| 2 | Copy full APK into isolation `meta/` when user picks (size/progress UI) |
| 3 | Read APK package name / versionName via `PackageManager.getPackageArchiveInfo` |
| 4 | Reject non-`com.mojang.minecraftpe` packages |
| 5 | Version list shows: Play Store / Imported / Missing |
| 6 | Per-profile bind to imported APK path |
| 7 | World/pack path helpers under isolation |
| 8–10 | Hardening, logs, “Open isolation folder” debug |

## Phase B — Days 11–30 (study + host design)
| Day | Goal |
|-----|------|
| 11–15 | Map LeviLaunchroid `:minecraft` launch flow (read-only study, license respect) |
| 16–20 | Design Remom `BedrockHost` interface (launch/stop/isolation) |
| 21–25 | Decide: embed vs dual-APK deep-link |
| 26–30 | Spike: load one official APK assets path without full game |

## Phase C — Days 31–50 (native host)
| Day | Goal |
|-----|------|
| 31–40 | Integrate or reimplement install-free launch path (Levi-class) |
| 41–45 | Version isolation wired to host |
| 46–50 | Play selected version end-to-end on test device |

## Phase D — Days 51–60 (polish)
| Day | Goal |
|-----|------|
| 51–55 | Errors, repair, update breaks |
| 56–60 | Docs, legal gate audit, release notes |

## How we work
1. User says **“agla step”** → next day item only  
2. No fake “all versions download from internet” without official source  
3. Cracked clients = never  

**Current:** Day **1** complete in code (picker + meta register).
