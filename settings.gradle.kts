rootProject.name = "x-wal"

include(
    "hexagon:core",
    "hexagon:ports",
    "hexagon:application",
    "adapters:driving:web",
    "adapters:driving:cli",
    "adapters:driven:persistence",
    "adapters:driven:engine",
    "adapters:driven:identity",
    "adapters:driven:observability",
    "app"
)
