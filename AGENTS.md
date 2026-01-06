# Axiom / Seek — AI Agent Notes

This repository contains **Axiom**, a Kotlin/Android robotics “core” library (FTC-oriented) plus **Seek**, an Electron + Svelte desktop app used to **connect to a running robot runtime**, **inspect scheduler state**, **graph numeric values**, and **send edits back** to the runtime.

## High-level purpose

- **Axiom Core (`/Core`)**: A command/scheduler framework intended to run in an FTC-style Android environment (or a mock environment for tests). It can serialize scheduler/runtime state and stream it over a WebSocket server.
- **Seek Interface (`/Interface`)**: A desktop UI that auto-connects to the Core WebSocket server, renders tool windows (scheduler, states, graphs, etc.), and can send edit commands back to Core.
- **Mock FTC (`/MockFTC`)**: Lightweight stubs of FTC SDK APIs to allow unit tests and “mock” builds.

## Repo layout (what lives where)

- **`Core/`** (Android library module, Kotlin)
  - **Scheduler/Commands**: `Core/src/main/java/.../scheduler/*`, `.../commands/*`
  - **WebSocket server + protocol**: `Core/src/main/java/.../web/Server.kt`, `.../web/serializable/*`
  - **Input/gamepad bindings**: `Core/src/main/java/.../input/*`
  - **Unit tests**: `Core/src/test/java/...`
- **`Interface/`** (Electron app, Svelte 5 + TS)
  - **Main process**: `Interface/src/main/index.ts`
  - **Preload bridge + WebSocket client**: `Interface/src/preload/index.ts`, `Interface/src/preload/AutoWebsocket.ts`
  - **Renderer app**: `Interface/src/renderer/src/App.svelte` and `.../components/*`, `.../lib/*`
- **`MockFTC/`** (mocked FTC classes)

## How Core and Interface talk

### Transport

- Core hosts a **WebSocket server on port `10464`** (NanoWSD):
  - `Core/src/main/java/.../web/Server.kt`
- Seek’s preload tries to connect to:
  - `ws://localhost:10464` (typical when connected via USB + ADB port forward)
  - `ws://192.168.43.1:10464` (typical robot device IP / WiFi scenario)
  - See: `Interface/src/preload/index.ts`

### ADB port forwarding (USB convenience)

Seek’s Electron main process periodically runs:

- `adb forward tcp:10464 tcp:10464`

to make the robot’s `:10464` reachable at `localhost:10464`. It also reports ADB status to the renderer (status bar + notifications).

See: `Interface/src/main/index.ts`

### Message envelope (Core → Seek)

Seek expects messages shaped like:

```ts
type BaseResponse = {
  name: string
  tick: number
  data: unknown
}
```

The renderer dispatches by `name` via a simple registry:

- `Interface/src/renderer/src/App.svelte`
- `Interface/src/renderer/src/lib/networkRegistry.ts`

### Known inbound event names (Core → Seek)

Defined in `Core/src/main/java/.../web/serializable/*`:

- **`schedulable_initial`**: initial list of schedulables (commands/systems) + their current state
- **`schedulable_update`**: structural updates/removals (name/type/parent changes)
- **`schedulable_state_update`**: field-level state deltas for schedulables
- **`schedulable_order`**: execution order (sorted command IDs)
- **`scheduler_details`**: scheduler tick + execution time + current wall time
- **`notification`**: toast/modal notification payload
- **`robot_telemetry`**: robot telemetry payloads (UI support may be incomplete)

Entry point for emitting most of these from the runtime tick:

- `Core/src/main/java/.../scheduler/Scheduler.kt`

### Outbound control message (Seek → Core)

Seek can send edits back to Core with:

```json
{ "type": "edit", "path": "<property-path>", "value": "<value>" }
```

- Sent from: `Interface/src/renderer/src/lib/stores/schedulableStore.ts` (`editState`)
- Parsed/handled in: `Core/src/main/java/.../web/Server.kt` → `Scheduler.edit(path, value)`
- Applied via: `Core/src/main/java/.../scheduler/PropertyEditor.kt` (reflection-based property editing)

## What the Seek UI currently provides

### Top “App Bar” window launcher

The menu categories and windows are declared in:

- `Interface/src/renderer/src/components/bars/AppBar.svelte`
- Registry exports: `Interface/src/renderer/src/components/windows/registry/index.ts`

Current windows:

- **States**: searchable list of schedulables; opens a detailed StateView per item
- **Scheduler**: shows tick / execution time and current execution order list
- **Graph**: select numeric fields from schedulables, plot over time (uPlot), pause, and export CSV

Other menu items exist but appear stubbed/minimal in the current codebase:

- **Telemetry**, **Control**, **Gamepad Overview**, **Hardware Viewer**, **Config**

### Status bar: connectivity + updates

Status bar features:

- Axiom connection indicator + latency label (latency wiring may be incomplete; variable is present)
- USB/WiFi hinting based on ADB forwarding status
- App version shown (“Seek x.y.z”)
- Auto-update status/buttons via `electron-updater`

See: `Interface/src/renderer/src/components/bars/StatusBar.svelte`

### Notifications

- Core can push notifications via `Notification.send(...)`:
  - `Core/src/main/java/.../web/serializable/Notification.kt`
- Renderer renders them via:
  - `Interface/src/renderer/src/lib/stores/notificationStore.ts`
  - `Interface/src/renderer/src/components/notifications/*`

## How the scheduler works (Core)

- Commands and systems are scheduled into shared, concurrent maps and executed every loop tick. The scheduler is also responsible for **streaming the minimal set of changes** to connected clients (like Seek) over WebSocket.

### Core concepts: Schedulables, Commands, Systems

- **Schedulable**: a minimal interface with an `id` (`Core/.../commands/Schedulable.kt`).
- **System**: a long-lived mechanism/component with a stable `id` and `name` (`Core/.../commands/System.kt`).
  - May define `update` and/or `apply` commands.
  - Has a `SystemCommand` builder (`CommandBuilder(this)`) to conveniently create commands that are automatically associated with that system.
- **Command**: an executable schedulable with:
  - a `name`, `id`, optional **state object**, optional **interval** (rate limiting), and optional **parent system**
  - DSL hooks: `enter {}`, `exit {}`, `requires {}`, `action {}`, and `stop()`
  - a **dependency list** (`dependencies: ArrayList<WeakReference<Command<*>>>`) used for ordering

Key files:

- `Core/src/main/java/.../commands/Command.kt`
- `Core/src/main/java/.../commands/System.kt`
- `Core/src/main/java/.../commands/CommandBuilder.kt`

### Scheduling model (what gets stored)

Scheduler state is in `Core/.../scheduler/SchedulerState.kt`:

- **`commands`**: `ConcurrentHashMap<String, GenericCommand>` keyed by command id
- **`systems`**: `ConcurrentHashMap<String, System>` keyed by system id
- **`sortedCommands`**: the current execution order (topologically sorted)
- **Queues** used during ticks to avoid concurrent modification:
  - `addQueue`, `removeQueue`, `editQueue`

When you call:

- `Scheduler.schedule(command)` → inserts a command (or queues it if a tick is in progress)
- `Scheduler.schedule(system)` → stores the system, sets `system.update/apply.parent = system`, and schedules those commands

### Dependencies and execution order

Execution order is computed by `DependencyResolver.sort(...)` (`Core/.../scheduler/DependencyResolver.kt`):

- It performs a DFS topological sort over the graph implied by each command’s `dependencies`.
- Cycles throw an exception: `IllegalStateException("Cyclic dependency detected ...")`.

Important nuance:

- A `Command` created with a `parent System` automatically adds a dependency on that system (via `parent?.let { dependsOn(it) }` in `Command.kt`).
- `Command.dependsOn(system)` is implemented as a dependency on the system’s **`update` command** (weak-ref). So “system dependencies” effectively become command dependencies.

### Tick lifecycle (what happens each loop)

Main loop method: `Core/.../scheduler/Scheduler.tick()`.

At a high level, each tick:

- **Enters update cycle**: sets `inUpdateCycle = true`
- **Applies queued edits**: `PropertyEditor.edit(path, value)` for each edit
- **Applies queued adds/removes**
- **Re-sorts** if structure changed: `sortedCommands = DependencyResolver.sort(commands)`
- **Executes** commands in `sortedCommands`:
  - each `Command.execute()` can be rate-limited by `interval`
  - it tracks timing in `Command.Meta` (`executionTime`, `deltaTime`, etc.)
- **Computes deltas** for streaming:
  - serializes current commands/systems → `DeltaResolver.serialize(...)`
  - compares against snapshots → `DeltaResolver.resolve(...)`
  - emits:
    - `schedulable_update` (structure/name/type/parent changes + removals)
    - `schedulable_state_update` (field-level deltas)
    - `schedulable_order` (when the sorted list changes)
- **Always emits `scheduler_details`** at the end of the tick

Note: `scheduler_details.executionTime` is currently sent as **seconds** (`DurationUnit.SECONDS`) even though the UI labels it “ms” — if you rely on this, verify and/or fix the units.

### What “state” means (serialization model)

The scheduler streams “state” for:

- each **Command’s `state` object** (if non-null)
- each **System object itself** (its properties)

Serialization happens in `Core/.../scheduler/DeltaResolver.kt` via Kotlin reflection and a small annotation set in `Core/.../web/*`:

- **`@Hidden(exclude = true)`**:
  - default (`exclude=true`) means “do not serialize this property at all”
  - using `@Hidden(false)` means “serialize it, but mark it hidden”
- **`@Editable`**: marks a property as user-editable (otherwise it is treated as readonly)
- **`@Display(priority = ...)`**:
  - for top-level state properties: influences sort/priority metadata
  - for nested objects: only properties annotated with `@Display` are included

Primitive-like values are wrapped as:

- `{ value: <primitive|null>, metadata: { readonly, priority, hidden? } }`

Collections/arrays are serialized recursively; nested objects only serialize their `@Display` properties.

### Editing model (Seek → Core)

Edits are applied by `PropertyEditor` (`Core/.../scheduler/PropertyEditor.kt`):

- Path format is: `<type>.<id>.<field>.<subfield>...`
  - `<type>` is `command` or `system`
  - numeric path segments are treated as **indexes** into lists/arrays
- Values arrive as strings and are converted to the target type (int/float/double/bool/etc.) when possible.

If an edit can’t be applied, Core emits a `notification` (“Edit Failure”) instructing the UI to resynchronize.

### Command groups (composing behavior)

Core includes simple composition helpers in `Core/.../commands/groups/*`:

- **`SequentialCommandGroup`**: schedules commands one-by-one, waiting for each to stop running before scheduling the next.
- **`ConcurrentCommandGroup`**: schedules all commands at once and finishes when **ALL** or **ANY** of them complete (configurable).

These are implemented as Commands themselves (with their own group state), so they participate in the scheduler like any other command.

Key files:

- `Core/src/main/java/.../scheduler/Scheduler.kt`
- `Core/src/main/java/.../scheduler/SchedulerState.kt`
- `Core/src/main/java/.../scheduler/DeltaResolver.kt`
- `Core/src/main/java/.../scheduler/DependencyResolver.kt`

## How to build / run (common workflows)

### Seek (Interface)

From `Interface/`:

- **Dev**: `npm run dev`
- **Typecheck**: `npm run typecheck`
- **Build**: `npm run build`
- **Package**: `npm run build:win` / `build:mac` / `build:linux`

Notes:

- CI uses **Bun** (`bun install`, `bun run ...`) but `package.json` also supports `npm`.
- Packaging configuration is in `Interface/electron-builder.yml` (GitHub Releases provider).

### Core (Android/Kotlin)

From repo root:

- **Run unit tests**: `./gradlew :core:test`

Notes:

- Core uses product flavors:
  - **`mock`** flavor depends on `MockFTC` and is intended for testing without the real FTC SDK
  - **`prod`** flavor depends on FTC SDK artifacts
- Core publishes as `io.github.bionictigers.axiom:core` (see `Core/build.gradle.kts`).

## Common extension points (for future agents)

- **Add a new streaming data feed**:
  - Create a new `Serializable` in `Core/.../web/serializable/*` with a unique `name`
  - Emit it from scheduler tick or other runtime events via `Server.send(...)`
  - Register a handler in Seek via `registerNetworkEvent(name, handler)`
  - Add a UI window or store to surface it

- **Add a new UI tool window**:
  - Implement a Svelte component in `Interface/src/renderer/src/components/windows/registry/`
  - Export it from `registry/index.ts`
  - Add it to a menu in `components/bars/AppBar.svelte`

- **Edit/teleop controls**:
  - Seek currently sends edit messages (`type: "edit"`) through the WebSocket
  - Core applies edits via reflection (`PropertyEditor`) — keep paths stable and versioned if you change them

## “Gotchas” / stability notes

- **Protocol compatibility**: Seek is largely “stringly typed” on event `name`; changing `name` values in Core breaks the UI unless updated together.
- **Connection environment**: USB tends to rely on ADB being installed and a device being connected; WiFi uses a hard-coded fallback IP.
- **Some UI windows are placeholders**: don’t assume telemetry/gamepad/hardware/config are fully implemented unless you verify their code.

