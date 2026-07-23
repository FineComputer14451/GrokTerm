# xterm.js assets (offline)

GrokTerm loads the terminal from these local assets so it works fully offline.

## Required files

Place the following files in this directory (`android/app/src/main/assets/xterm/`):

```
xterm/
├── index.html          (already provided)
├── xterm.min.js
├── xterm.min.css
├── addon-fit.min.js
└── addon-web-links.min.js
```

## How to vendor (one-time)

From a machine with network access, run:

```bash
cd android/app/src/main/assets/xterm

# xterm core
curl -L -o xterm.min.js     https://cdn.jsdelivr.net/npm/@xterm/xterm@5.5.0/lib/xterm.min.js
curl -L -o xterm.min.css    https://cdn.jsdelivr.net/npm/@xterm/xterm@5.5.0/css/xterm.min.css

# addons
curl -L -o addon-fit.min.js       https://cdn.jsdelivr.net/npm/@xterm/addon-fit@0.10.0/lib/addon-fit.min.js
curl -L -o addon-web-links.min.js https://cdn.jsdelivr.net/npm/@xterm/addon-web-links@0.11.0/lib/addon-web-links.min.js
```

Or download the packages from npm and copy the minified files.

After adding the files, rebuild the app. The terminal will no longer require network access for the UI layer.

## Versions used in scaffold

- @xterm/xterm@5.5.0
- @xterm/addon-fit@0.10.0
- @xterm/addon-web-links@0.11.0
