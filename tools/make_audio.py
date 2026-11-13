"""안내 음성 만들기: narrations.json 의 긴 설명 → assets/audio/*.mp3

읽는 글은 "{왼쪽|오른쪽} 창밖, {이름}. {긴 설명}" 이다. 방향과 이름, 파일 이름은
경로 JSON(routes_seoul_yangyang.json)의 side, name, audioFile 에서 가져온다.

    pip install edge-tts
    python tools/make_audio.py            # 전부 다시 만든다
    python tools/make_audio.py --out DIR  # 다른 폴더에 만든다(미리 들어 볼 때)

설명 글을 고치면 이 스크립트를 다시 돌려야 음성과 글이 맞는다.
"""
import argparse
import asyncio
import json
import sys
from pathlib import Path

import edge_tts

ASSETS = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets"
VOICE = "ko-KR-SunHiNeural"
RATE = "-5%"  # 달리는 차 안에서 듣는 글이라 조금 천천히


def lines():
    """(파일 이름, 읽을 글) 목록. 같은 지점이 두 경로에 있으면 방향이 같아야 한다."""
    routes = json.loads((ASSETS / "routes_seoul_yangyang.json").read_text(encoding="utf-8"))
    narrations = json.loads((ASSETS / "narrations.json").read_text(encoding="utf-8"))["narrations"]
    out = {}
    for route in routes["routes"]:
        for p in route.get("passPoints", []):
            audio = p.get("audioFile")
            if not audio:
                continue
            if p["id"] not in narrations:
                sys.exit(f"narrations.json 에 {p['id']} 가 없다")
            side = "오른쪽" if p["side"] == "R" else "왼쪽"
            text = f"{side} 창밖, {p['name']}. {narrations[p['id']]}"
            if out.setdefault(audio, text) != text:
                sys.exit(f"{audio}: 경로마다 방향이나 이름이 다르다")
    if not out:
        sys.exit("경로 JSON 에서 audioFile 이 있는 지점을 찾지 못했다")
    return out


async def main(out_dir: Path):
    out_dir.mkdir(parents=True, exist_ok=True)
    for audio, text in lines().items():
        target = out_dir / Path(audio).name
        await edge_tts.Communicate(text, VOICE, rate=RATE).save(str(target))
        print(target.name, len(text), "자")


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", type=Path, default=ASSETS / "audio")
    asyncio.run(main(ap.parse_args().out))
