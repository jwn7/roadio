"""안내 음성 만들기: narrations.json 의 긴 설명 → assets/audio/*.mp3

읽는 글은 "{왼쪽|오른쪽} 창밖, {이름}. {긴 설명}" 이다. 방향과 이름, 파일 이름은
경로 JSON(routes_seoul_yangyang.json)의 side, name, audioFile 에서 가져온다.

    pip install edge-tts imageio-ffmpeg
    python tools/make_audio.py            # 전부 다시 만든다
    python tools/make_audio.py --out DIR  # 다른 폴더에 만든다(미리 들어 볼 때)
    python tools/make_audio.py --finish-only  # 이미 있는 파일에 마무리 처리만 한다

마무리 처리: edge-tts 결과(24kHz, 48kbps, 앞 무음 약 0.22초)를 48kHz, 128kbps 로 다시 저장하고
앞 무음을 약 0.5초로 늘린다. 재생을 시작하는 순간 출력 장치가 흔들려 생기는 "지지직"이
말소리가 아니라 무음 위에 떨어지게 하려는 것이다(docs/verify.md 2026-11-14 음성 앞부분).

설명 글을 고치면 이 스크립트를 다시 돌려야 음성과 글이 맞는다.
"""
import argparse
import asyncio
import json
import os
import subprocess
import sys
from pathlib import Path

ASSETS = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets"
VOICE = "ko-KR-SunHiNeural"
RATE = "-5%"  # 달리는 차 안에서 듣는 글이라 조금 천천히
LEAD_MS = 280  # edge-tts 의 앞 무음(약 220ms)에 더할 무음


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


def finish(path: Path):
    """앞 무음을 늘리고 48kHz, 128kbps 모노로 다시 저장한다. 말소리는 바꾸지 않는다."""
    import imageio_ffmpeg
    tmp = path.with_suffix(".tmp.mp3")
    subprocess.run([imageio_ffmpeg.get_ffmpeg_exe(), "-v", "error", "-y", "-i", str(path),
                    "-af", f"adelay={LEAD_MS}", "-ar", "48000", "-ac", "1", "-b:a", "128k",
                    "-map_metadata", "-1", str(tmp)], check=True)
    os.replace(tmp, path)


async def main(out_dir: Path, finish_only: bool):
    out_dir.mkdir(parents=True, exist_ok=True)
    for audio, text in lines().items():
        target = out_dir / Path(audio).name
        if not finish_only:
            import edge_tts
            await edge_tts.Communicate(text, VOICE, rate=RATE).save(str(target))
        finish(target)
        print(target.name, len(text), "자")


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", type=Path, default=ASSETS / "audio")
    ap.add_argument("--finish-only", action="store_true", help="이미 있는 파일에 마무리 처리만 한다")
    args = ap.parse_args()
    asyncio.run(main(args.out, args.finish_only))
