# Moon Studio HMG background audio staging
This is a source template, not an installable or verified HMG mod.
Kyle's Light Vibes uses a persistent HMG mod script that execs a native daemon.
Moon Studio uses its own `moon-studio` mod path and Android package identity.
The script requires an explicit enable_native_audio file and never writes MICOM lighting commands.
It starts independently of the app UI, retries after playback stops, and runs bounded capture sessions.
WARNING: A hard kill or power loss can prevent mixer restoration. Do not deploy before vehicle validation.
