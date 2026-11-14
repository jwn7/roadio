"""안내 음성 만들기: narrations.json 의 긴 설명 → assets/audio/*.mp3

읽는 글은 "{왼쪽|오른쪽} 창밖, {이름}. {긴 설명}" 이다. 방향과 이름, 파일 이름은
경로 JSON(routes_seoul_yangyang.json)의 side, name, audioFile 에서 가져온다.

    pip install edge-tts imageio-ffmpeg
    python tools/make_audio.py            # 전부 다시 만든다
    python tools/make_audio.py --out DIR  # 다른 폴더에 만든다(미리 들어 볼 때)
    python tools/make_audio.py --finish-only  # 이미 있는 파일에 마무리 처리만 한다

받는 형식: edge-tts 는 기본으로 24kHz·48kbps mp3 를 받는다. 이 낮은 비트레이트에서 생긴 거친 성분은
나중에 다시 저장해도 없어지지 않으므로, 같은 목소리를 Opus(webm) 로 받는다(무료 서비스가 허용하는
형식 중 가장 좋은 품질. WAV 나 고비트레이트 mp3 는 응답이 오지 않는다. 2026-11-14 시험).

마무리 처리: 받은 소리를 48kHz, 192kbps mp3 로 저장하고 앞 무음을 약 0.5초로 늘린다. 재생을 시작하는 순간 출력 장치가 흔들려 생기는 "지지직"이
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
FORMAT = "webm-24khz-16bit-mono-opus"


def use_opus():
    """edge-tts 가 고정해 둔 출력 형식을 Opus 로 바꾼다. 라이브러리가 바뀌어 고칠 곳을 못 찾으면 멈춘다."""
    import inspect
    import edge_tts.communicate as c
    src = inspect.getsource(c)
    old_fmt = '"outputFormat":"audio-24khz-48kbitrate-mono-mp3"'
    old_type = 'if content_type not in (b"audio/mpeg", None):'
    if old_fmt not in src or old_type not in src:
        sys.exit("edge-tts 버전이 달라 출력 형식을 바꿀 수 없다. edge-tts 7.3.1 에서 시험했다")
    src = src.replace(old_fmt, f'"outputFormat":"{FORMAT}"').replace(
        old_type, 'if content_type not in (b"audio/mpeg", b"audio/webm; codecs=opus", b"audio/webm", None):')
    exec(compile(src, c.__file__, "exec"), c.__dict__)


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
    src = path.with_suffix(".webm") if path.with_suffix(".webm").exists() else path
    tmp = path.with_suffix(".tmp.mp3")
    subprocess.run([imageio_ffmpeg.get_ffmpeg_exe(), "-v", "error", "-y", "-i", str(src),
                    "-af", f"adelay={LEAD_MS}", "-ar", "48000", "-ac", "1", "-b:a", "192k",
                    "-map_metadata", "-1", str(tmp)], check=True)
    os.replace(tmp, path)
    if src != path:
        src.unlink()


async def main(out_dir: Path, finish_only: bool):
    out_dir.mkdir(parents=True, exist_ok=True)
    for audio, text in lines().items():
        target = out_dir / Path(audio).name
        if not finish_only:
            import edge_tts
            use_opus()
            await edge_tts.Communicate(text, VOICE, rate=RATE).save(str(target.with_suffix(".webm")))
        finish(target)
        print(target.name, len(text), "자")


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", type=Path, default=ASSETS / "audio")
    ap.add_argument("--finish-only", action="store_true", help="이미 있는 파일에 마무리 처리만 한다")
    args = ap.parse_args()
    asyncio.run(main(args.out, args.finish_only))
