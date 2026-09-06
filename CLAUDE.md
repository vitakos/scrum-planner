# Working with Claude on this project

Notes for AI assistants (and humans) working in this repo.

## UI / visual work

Claude has access to a browser tool ("Claude in Chrome" / the built-in browser) that can open the
running frontend, click around, and read the actual rendered page — not just the source code. When
working on frontend/UI tasks:

- Prefer starting the app locally (see "Running Locally" in the README) and having Claude open it
  in the browser to inspect layout, verify a change visually, or debug a UI bug against the real
  rendered output, instead of reasoning from JSX/CSS alone.
- Screenshots and page reads from the browser tool are a good way to sanity-check design/UX changes
  before opening a PR.
- The person can also watch/drive the same browser session live, so UI work can be done
  collaboratively in real time (see below).

## Pairing on design in real time

Yes — this works both ways:

- If Claude uses its **built-in browser** (a browser pane inside the Claude desktop app), the same
  pane is visible to the person in the app, and they can take over or point things out live while
  Claude is navigating the UI.
- If Claude uses **Claude in Chrome**, it drives the person's actual Chrome browser, so the person
  is looking at their own browser window the whole time and can literally watch each click/navigation
  happen, or take the mouse back at any point.

Either way, no separate screen-share is needed — pick whichever browser is already open and go.
