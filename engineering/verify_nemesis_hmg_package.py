#!/usr/bin/env python3
"""Check that a Moon Studio USB release retains the working v0.2.0 packaging contract.
Usage: python3 engineering/verify_nemesis_hmg_package.py release-usb.zip
Structural checks only: HMG-API signature and in-vehicle operation require separate verification.
"""
import io
import json
import sys
import zipfile

def require(condition, message):
    if not condition:
        raise ValueError(message)

def verify(path):
    with zipfile.ZipFile(path) as outer:
        names = set(outer.namelist())
        require("NEMESIS.data" in names, "Missing root NEMESIS.data")
        require("packages/moon.studio.zip" in names, "Missing Moon Studio nested NEMESIS installer")
        root = outer.read("NEMESIS.data").decode()
        require("type=install" in root, "Root descriptor must install nested package")
        with zipfile.ZipFile(io.BytesIO(outer.read("packages/hmgapi.moon-studio.zip"))) as nested:
            entries = set(nested.namelist())
            for item in ("NEMESIS.data", "mod/package.zip", "scripts/postinstall.sh", "mod/id"):
                require(item in entries, "Missing nested entry: " + item)
            require(nested.read("mod/id").strip() == b"moon-studio", "Wrong HMG-API mod ID")
            descriptor = nested.read("NEMESIS.data").decode()
            require("depends=\"hmgapi@>=1.0\"" in descriptor, "Missing HMG-API dependency")
            with zipfile.ZipFile(io.BytesIO(nested.read("mod/package.zip"))) as mod:
                mod_entries = set(mod.namelist())
                for item in ("META-HMGAPI/manifest.json", "META-HMGAPI/signature",
                             "mod.json", "app/MoonStudio.apk", "moon.sh",
                             "icons/tile.png", "icons/tile_disabled.png"):
                    require(item in mod_entries, "Missing HMG-API mod entry: " + item)
                config = json.loads(mod.read("mod.json"))
                require(config.get("id") == "moon-studio", "Wrong mod ID")
                require(any(a.get("package") == "com.nemesis.moonstudio" and
                            a.get("apk") == "app/MoonStudio.apk" for a in config.get("apps", [])),
                        "Missing Moon Studio APK registration")
                require(any(a.get("package") == "com.nemesis.moonstudio" and
                            a.get("icon") == "icons/tile.png" for a in
                            config.get("provides", {}).get("launcher", [])),
                        "Missing EV6 launcher registration")
                require(any(s.get("script") == "moon.sh" and s.get("restart") is True
                            for s in config.get("services", [])),
                        "Missing restartable background lighting service")
    print("PASS: Moon Studio USB/HMG-API package structure matches v0.2.0 contract")
    print("NOTE: This does not verify the signature, APK functionality, or vehicle behavior.")

if __name__ == "__main__":
    require(len(sys.argv) == 2, "Usage: verify_nemesis_hmg_package.py release-usb.zip")
    verify(sys.argv[1])
