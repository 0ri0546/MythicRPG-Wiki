from pathlib import Path
import shutil
import subprocess
import sys


ROOT = Path(__file__).resolve().parents[1]


def run(command: list[str], cwd: Path = ROOT) -> None:
    print("+", " ".join(str(part) for part in command))
    subprocess.run(command, cwd=cwd, check=True)


def main() -> int:
    run([sys.executable, "tools/build_catalog.py"])
    run([
        sys.executable,
        "-m",
        "unittest",
        "discover",
        "-s",
        "tests",
        "-v"
    ])

    npm = shutil.which("npm.cmd") or shutil.which("npm")

    if npm is None:
        raise RuntimeError(
            "npm est introuvable. Vérifie que Node.js et npm sont installés "
            "et disponibles dans le PATH."
        )

    run(
        [npm, "ci", "--no-audit", "--no-fund"],
        ROOT / "website"
    )

    run(
        [npm, "run", "build"],
        ROOT / "website"
    )

    run([sys.executable, "scripts/verify_delivery.py", "--require-build"])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())